package github.leavesczy.matisse.internal.logic

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.internal.MatisseLog
import github.leavesczy.matisse.internal.logic.MediaProvider.BUCKET_COVER_LOAD_CONCURRENCY
import github.leavesczy.matisse.internal.logic.MediaProvider.mediaRecencySortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

internal object MediaProvider {

    private const val BUCKET_COVER_LOAD_CONCURRENCY = 8

    data class MediaInfo(
        val uri: Uri,
        val mimeType: String,
        val mediaId: Long,
        val bucketId: String,
        val bucketName: String,
        val pageKey: MediaPageKey
    )

    /** 媒体在 [mediaRecencySortOrder] 中的排序位置，作为 keyset 分页的游标。 */
    data class MediaPageKey(
        val recency: Long,
        val mediaId: Long
    )

    data class MediaBucketAggregate(
        val bucketId: String,
        val bucketName: String,
        val itemCount: Int,
        /** 封面查询失败时为 null，相册仍保留在列表中。 */
        val coverUri: Uri?,
        val coverMimeType: String?
    )

    data class MediaBuckets(
        /** 与「全部」网格相同的查询范围，包含未归属任何有效相册（相册名为空）的媒体。 */
        val totalItemCount: Int,
        val buckets: List<MediaBucketAggregate>
    )

    private val isAtLeastQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    suspend fun createImageUri(
        context: Context,
        imageName: String,
        mimeType: String
    ): Uri? {
        return withContext(context = Dispatchers.IO) {
            try {
                val contentValues = ContentValues()
                contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                val imageCollection = if (isAtLeastQ) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                context.contentResolver.insert(imageCollection, contentValues)
            } catch (throwable: Throwable) {
                MatisseLog.e(throwable = throwable)
                null
            }
        }
    }

    suspend fun deleteMedia(context: Context, uri: Uri) {
        withContext(context = Dispatchers.IO) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (throwable: Throwable) {
                MatisseLog.e(throwable = throwable)
            }
        }
    }

    /**
     * 按 [mediaRecencySortOrder] 查询排在 [after] 之后的至多 [limit] 条媒体，[after] 为 null 时从头开始。
     * 分页游标是上一页最后消费一条的排序位置，不使用偏移量。
     */
    suspend fun loadMediaInfoPage(
        context: Context,
        mediaType: MediaType,
        bucketId: String?,
        limit: Int,
        after: MediaPageKey? = null
    ): List<MediaInfo> {
        return withContext(context = Dispatchers.IO) {
            queryMediaInfoPage(
                context = context,
                mediaType = mediaType,
                bucketId = bucketId,
                limit = limit,
                after = after
            )
        }
    }

    /**
     * 同步分页查询。调用方须已在后台线程（例如 [Dispatchers.IO]），
     * 避免在已处于 IO 的封面并发加载路径中再次 [withContext]。
     */
    private fun queryMediaInfoPage(
        context: Context,
        mediaType: MediaType,
        bucketId: String?,
        limit: Int,
        after: MediaPageKey? = null
    ): List<MediaInfo> {
        val mediaSelection = generateSqlSelection(mediaType = mediaType)
        val selectionParts = mutableListOf(
            withMediaStoreStateSelection(selection = mediaSelection.selection)
        )
        val selectionArgs = mutableListOf<String>().apply {
            addAll(elements = mediaSelection.selectionArgs)
        }
        if (!bucketId.isNullOrBlank()) {
            selectionParts.add(element = "${MediaStore.MediaColumns.BUCKET_ID} = ?")
            selectionArgs.add(element = bucketId)
        }
        if (after != null) {
            // 排序表达式没有列亲和性，以字符串参数绑定时会按 TEXT 比较，因此直接内联数值
            val recency = mediaRecencySortExpression()
            val idColumn = MediaStore.MediaColumns._ID
            selectionParts.add(
                element = "$recency < ${after.recency} OR ($recency = ${after.recency} AND $idColumn < ${after.mediaId})"
            )
        }
        return queryMediaInfoList(
            context = context,
            selection = selectionParts.joinToString(separator = " AND ") { "($it)" },
            selectionArgs = selectionArgs.toTypedArray(),
            limit = limit
        ).orEmpty()
    }

    /**
     * 加载相册列表：先按 `BUCKET_ID` 聚合各相册数量与「全部」总数，再按与网格相同的 recency
     * 排序为每个相册各取 1 条作为封面（并发上限 [BUCKET_COVER_LOAD_CONCURRENCY]），封面即该相册网格首项。
     * 聚合失败或拿不到计数列时走全表扫描，封面取扫描顺序中该相册首次出现的媒体。
     */
    suspend fun loadMediaBuckets(
        context: Context,
        mediaType: MediaType
    ): MediaBuckets {
        return withContext(context = Dispatchers.IO) {
            val groupedSummaries = queryGroupedBucketSummaries(
                context = context,
                mediaType = mediaType
            )
            if (groupedSummaries != null) {
                MediaBuckets(
                    totalItemCount = groupedSummaries.totalItemCount,
                    buckets = loadBucketCovers(
                        context = context,
                        mediaType = mediaType,
                        summaries = groupedSummaries.summaries
                    )
                )
            } else {
                queryBucketsByFullScan(
                    context = context,
                    mediaType = mediaType
                )
            }
        }
    }

    /** 按 `BUCKET_ID` 聚合各相册数量与「全部」总数；失败或拿不到计数列时返回 null。 */
    private fun queryGroupedBucketSummaries(
        context: Context,
        mediaType: MediaType
    ): GroupedBucketSummaries? {
        val bucketIdColumn = MediaStore.MediaColumns.BUCKET_ID
        val bucketDisplayNameColumn = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        val countColumn = "COUNT(${MediaStore.MediaColumns._ID})"
        val projection = arrayOf(
            bucketIdColumn,
            bucketDisplayNameColumn,
            countColumn
        )
        val contentUri = MediaStore.Files.getContentUri("external")
        val summaries = ArrayList<BucketSummary>()
        var totalItemCount = 0
        try {
            val mediaSelection = generateSqlSelection(mediaType = mediaType)
            val cursor = queryMediaCursor(
                contentResolver = context.contentResolver,
                contentUri = contentUri,
                projection = projection,
                selection = withMediaStoreStateSelection(selection = mediaSelection.selection),
                selectionArgs = mediaSelection.selectionArgs.takeIf { it.isNotEmpty() },
                limit = null,
                groupBy = bucketIdColumn,
                sortOrder = "$bucketDisplayNameColumn ASC"
            ) ?: return null
            cursor.use { cursor ->
                val bucketIdIndex = cursor.getColumnIndexOrThrow(bucketIdColumn)
                val bucketNameIndex = cursor.getColumnIndexOrThrow(bucketDisplayNameColumn)
                val countIndex = cursor.indexOfAggregateColumn(
                    aggregateSql = countColumn,
                    keyword = "COUNT"
                )
                if (countIndex < 0) {
                    return null
                }
                while (cursor.moveToNext()) {
                    val itemCount = cursor.getInt(countIndex)
                    if (itemCount <= 0) {
                        continue
                    }
                    // 「全部」网格不按相册过滤，相册名为空的媒体同样会展示，需要计入总数
                    totalItemCount += itemCount
                    val bucketId = cursor.getString(bucketIdIndex).orEmpty()
                    val bucketName = cursor.getString(bucketNameIndex).orEmpty()
                    if (bucketId.isBlank() || bucketName.isBlank()) {
                        continue
                    }
                    summaries.add(
                        element = BucketSummary(
                            bucketId = bucketId,
                            bucketName = bucketName,
                            itemCount = itemCount
                        )
                    )
                }
            }
        } catch (throwable: Throwable) {
            MatisseLog.e(throwable = throwable)
            return null
        }
        return GroupedBucketSummaries(
            totalItemCount = totalItemCount,
            summaries = summaries
        )
    }

    /** 每个相册按 recency 取 1 条封面，与该相册网格第一项一致。 */
    private suspend fun loadBucketCovers(
        context: Context,
        mediaType: MediaType,
        summaries: List<BucketSummary>
    ): List<MediaBucketAggregate> {
        if (summaries.isEmpty()) {
            return emptyList()
        }
        val semaphore = Semaphore(permits = BUCKET_COVER_LOAD_CONCURRENCY)
        return coroutineScope {
            summaries.map { summary ->
                async {
                    semaphore.withPermit {
                        val cover = queryMediaInfoPage(
                            context = context,
                            mediaType = mediaType,
                            bucketId = summary.bucketId,
                            limit = 1
                        ).firstOrNull()
                        MediaBucketAggregate(
                            bucketId = summary.bucketId,
                            bucketName = summary.bucketName,
                            itemCount = summary.itemCount,
                            coverUri = cover?.uri,
                            coverMimeType = cover?.mimeType
                        )
                    }
                }
            }.awaitAll()
        }
    }

    /** 全表按 recency 扫描：首次遇到某相册的行作为其封面。 */
    private fun queryBucketsByFullScan(
        context: Context,
        mediaType: MediaType
    ): MediaBuckets {
        val idColumn = MediaStore.MediaColumns._ID
        val mimeTypeColumn = MediaStore.MediaColumns.MIME_TYPE
        val bucketIdColumn = MediaStore.MediaColumns.BUCKET_ID
        val bucketDisplayNameColumn = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        val projection = arrayOf(
            idColumn,
            mimeTypeColumn,
            bucketIdColumn,
            bucketDisplayNameColumn
        )
        val contentUri = MediaStore.Files.getContentUri("external")
        val aggregates = linkedMapOf<String, MutableBucketAggregate>()
        var totalItemCount = 0
        try {
            val mediaSelection = generateSqlSelection(mediaType = mediaType)
            val cursor = queryMediaCursor(
                contentResolver = context.contentResolver,
                contentUri = contentUri,
                projection = projection,
                selection = withMediaStoreStateSelection(selection = mediaSelection.selection),
                selectionArgs = mediaSelection.selectionArgs.takeIf { it.isNotEmpty() },
                limit = null
            ) ?: return MediaBuckets(totalItemCount = 0, buckets = emptyList())
            cursor.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(idColumn)
                val mimeTypeIndex = cursor.getColumnIndexOrThrow(mimeTypeColumn)
                val bucketIdIndex = cursor.getColumnIndexOrThrow(bucketIdColumn)
                val bucketNameIndex = cursor.getColumnIndexOrThrow(bucketDisplayNameColumn)
                while (cursor.moveToNext()) {
                    totalItemCount += 1
                    try {
                        val id = cursor.getLong(idIndex)
                        val bucketId = cursor.getString(bucketIdIndex).orEmpty()
                        val bucketName = cursor.getString(bucketNameIndex).orEmpty()
                        if (bucketId.isBlank() || bucketName.isBlank()) {
                            continue
                        }
                        val uri = ContentUris.withAppendedId(contentUri, id)
                        val mimeType = cursor.getString(mimeTypeIndex).orEmpty()
                        val aggregate = aggregates.getOrPut(key = bucketId) {
                            MutableBucketAggregate(
                                bucketId = bucketId,
                                bucketName = bucketName,
                                itemCount = 0,
                                coverUri = uri,
                                coverMimeType = mimeType
                            )
                        }
                        aggregate.itemCount += 1
                    } catch (throwable: Throwable) {
                        MatisseLog.e(throwable = throwable)
                    }
                }
            }
        } catch (throwable: Throwable) {
            MatisseLog.e(throwable = throwable)
        }
        return MediaBuckets(
            totalItemCount = totalItemCount,
            buckets = aggregates.values.map { aggregate ->
                MediaBucketAggregate(
                    bucketId = aggregate.bucketId,
                    bucketName = aggregate.bucketName,
                    itemCount = aggregate.itemCount,
                    coverUri = aggregate.coverUri,
                    coverMimeType = aggregate.coverMimeType
                )
            }
        )
    }

    private fun Cursor.indexOfAggregateColumn(aggregateSql: String, keyword: String): Int {
        val exactIndex = getColumnIndex(aggregateSql)
        if (exactIndex >= 0) {
            return exactIndex
        }
        for (index in 0 until columnCount) {
            if (getColumnName(index).contains(other = keyword, ignoreCase = true)) {
                return index
            }
        }
        return -1
    }

    private fun queryMediaInfoList(
        context: Context,
        selection: String?,
        selectionArgs: Array<String>?,
        limit: Int?
    ): List<MediaInfo>? {
        val idColumn = MediaStore.MediaColumns._ID
        val mimeTypeColumn = MediaStore.MediaColumns.MIME_TYPE
        val bucketIdColumn = MediaStore.MediaColumns.BUCKET_ID
        val bucketDisplayNameColumn = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        val dateAddedColumn = MediaStore.MediaColumns.DATE_ADDED
        val dateModifiedColumn = MediaStore.MediaColumns.DATE_MODIFIED
        val projection = arrayOf(
            idColumn,
            mimeTypeColumn,
            bucketIdColumn,
            bucketDisplayNameColumn,
            dateAddedColumn,
            dateModifiedColumn
        )
        val contentUri = MediaStore.Files.getContentUri("external")
        val mediaInfoList = mutableListOf<MediaInfo>()
        try {
            val cursor = queryMediaCursor(
                contentResolver = context.contentResolver,
                contentUri = contentUri,
                projection = projection,
                selection = selection,
                selectionArgs = selectionArgs,
                limit = limit
            ) ?: return null
            cursor.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(idColumn)
                val mimeTypeIndex = cursor.getColumnIndexOrThrow(mimeTypeColumn)
                val bucketIdIndex = cursor.getColumnIndexOrThrow(bucketIdColumn)
                val bucketNameIndex = cursor.getColumnIndexOrThrow(bucketDisplayNameColumn)
                val dateAddedIndex = cursor.getColumnIndexOrThrow(dateAddedColumn)
                val dateModifiedIndex = cursor.getColumnIndexOrThrow(dateModifiedColumn)
                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idIndex)
                        val uri = ContentUris.withAppendedId(contentUri, id)
                        // 与 mediaRecencySortExpression 一致：NULL 视为 0，取两者中较大者；getLong 对 NULL 返回 0
                        val recency = maxOf(
                            a = cursor.getLong(dateAddedIndex),
                            b = cursor.getLong(dateModifiedIndex)
                        )
                        val mediaInfo = MediaInfo(
                            uri = uri,
                            mimeType = cursor.getString(mimeTypeIndex).orEmpty(),
                            mediaId = id,
                            bucketId = cursor.getString(bucketIdIndex).orEmpty(),
                            bucketName = cursor.getString(bucketNameIndex).orEmpty(),
                            pageKey = MediaPageKey(recency = recency, mediaId = id)
                        )
                        mediaInfoList.add(element = mediaInfo)
                    } catch (throwable: Throwable) {
                        MatisseLog.e(throwable = throwable)
                    }
                }
            }
        } catch (throwable: Throwable) {
            MatisseLog.e(throwable = throwable)
        }
        return mediaInfoList
    }

    private fun queryMediaCursor(
        contentResolver: ContentResolver,
        contentUri: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        limit: Int?,
        groupBy: String? = null,
        sortOrder: String = mediaRecencySortOrder()
    ): Cursor? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                if (groupBy != null) {
                    putString(ContentResolver.QUERY_ARG_SQL_GROUP_BY, groupBy)
                }
                if (limit != null) {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                }
            }
            contentResolver.query(contentUri, projection, queryArgs, null)
        } else {
            // 低于 API 30 走旧 query API：GROUP BY 拼进 selection，LIMIT 写在 sortOrder
            val groupedSelection = if (groupBy != null) {
                val baseSelection = selection ?: "1"
                "$baseSelection) GROUP BY ($groupBy"
            } else {
                selection
            }
            val pagedSortOrder = if (limit != null) {
                "$sortOrder LIMIT $limit"
            } else {
                sortOrder
            }
            contentResolver.query(
                contentUri,
                projection,
                groupedSelection,
                selectionArgs,
                pagedSortOrder
            )
        }
    }

    /**
     * 取 DATE_ADDED 与 DATE_MODIFIED 中较新者作为排序时间（NULL 视为 0），其次按 `_ID` 降序。
     * 排序值不能为 NULL，否则 keyset 比较条件恒不成立，这些条目不会出现在后续页。
     */
    private fun mediaRecencySortExpression(): String {
        val dateAdded = "IFNULL(${MediaStore.MediaColumns.DATE_ADDED}, 0)"
        val dateModified = "IFNULL(${MediaStore.MediaColumns.DATE_MODIFIED}, 0)"
        return "(CASE WHEN $dateAdded > $dateModified THEN $dateAdded ELSE $dateModified END)"
    }

    private fun mediaRecencySortOrder(): String {
        val idColumn = MediaStore.MediaColumns._ID
        return "${mediaRecencySortExpression()} DESC, $idColumn DESC"
    }

    suspend fun loadMediaInfo(context: Context, uri: Uri): MediaInfo? {
        return withContext(context = Dispatchers.IO) {
            val id = try {
                ContentUris.parseId(uri)
            } catch (throwable: Throwable) {
                MatisseLog.e(throwable = throwable)
                return@withContext null
            }
            if (id < 0L) {
                return@withContext null
            }
            val selection = withMediaStoreStateSelection(
                selection = MediaStore.MediaColumns._ID + " = " + id
            )
            val matchedMediaInfoList = queryMediaInfoList(
                context = context,
                selection = selection,
                selectionArgs = null,
                limit = 1
            )
            if (matchedMediaInfoList.isNullOrEmpty() || matchedMediaInfoList.size != 1) {
                null
            } else {
                matchedMediaInfoList[0]
            }
        }
    }

    /**
     * 通过文件描述符读取真实大小，而不是 MediaStore 的 `SIZE` 列：后者在外部相机写入后可能尚未更新。
     */
    suspend fun hasContent(context: Context, uri: Uri): Boolean {
        return withContext(context = Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use {
                    it.statSize > 0L
                } ?: false
            } catch (throwable: Throwable) {
                MatisseLog.e(throwable = throwable)
                false
            }
        }
    }

    private fun withMediaStoreStateSelection(selection: String): String {
        val stateSelection = mediaStoreStateSelection()
        return if (stateSelection.isBlank()) {
            selection
        } else {
            "($selection) AND ($stateSelection)"
        }
    }

    /** Android 10+ 排除 pending，Android 11+ 同时排除已进入回收站的条目。 */
    private fun mediaStoreStateSelection(): String {
        return buildString {
            if (isAtLeastQ) {
                val isPendingColumn = MediaStore.MediaColumns.IS_PENDING
                append("($isPendingColumn IS NULL OR $isPendingColumn = 0)")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (isNotEmpty()) {
                    append(" AND ")
                }
                val isTrashedColumn = MediaStore.MediaColumns.IS_TRASHED
                append("($isTrashedColumn IS NULL OR $isTrashedColumn = 0)")
            }
        }
    }

    private fun generateSqlSelection(mediaType: MediaType): MediaSqlSelection {
        val mediaTypeColumn = MediaStore.Files.FileColumns.MEDIA_TYPE
        val mediaTypeImageColumn = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        val mediaTypeVideoColumn = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        val mimeTypeColumn = MediaStore.Files.FileColumns.MIME_TYPE
        val queryImageSelection =
            "$mediaTypeColumn = $mediaTypeImageColumn and $mimeTypeColumn like 'image/%'"
        val queryVideoSelection =
            "$mediaTypeColumn = $mediaTypeVideoColumn and $mimeTypeColumn like 'video/%'"
        return when (mediaType) {
            MediaType.ImageOnly -> {
                MediaSqlSelection(selection = queryImageSelection)
            }

            MediaType.VideoOnly -> {
                MediaSqlSelection(selection = queryVideoSelection)
            }

            MediaType.ImageAndVideo -> {
                MediaSqlSelection(
                    selection = buildString {
                        append(queryImageSelection)
                        append(" or ")
                        append(queryVideoSelection)
                    }
                )
            }

            is MediaType.MimeTypes -> {
                val mimeTypes = mediaType.mimeTypes.toList()
                val placeholders = mimeTypes.joinToString(separator = ",") { "?" }
                MediaSqlSelection(
                    selection = "$mimeTypeColumn in ($placeholders)",
                    selectionArgs = mimeTypes.toTypedArray()
                )
            }
        }
    }

    private class MediaSqlSelection(
        val selection: String,
        val selectionArgs: Array<String> = emptyArray()
    )

    private class BucketSummary(
        val bucketId: String,
        val bucketName: String,
        val itemCount: Int
    )

    private class GroupedBucketSummaries(
        val totalItemCount: Int,
        val summaries: List<BucketSummary>
    )

    private class MutableBucketAggregate(
        val bucketId: String,
        val bucketName: String,
        var itemCount: Int,
        val coverUri: Uri,
        val coverMimeType: String
    )

}

internal fun MediaProvider.MediaBucketAggregate.toCoverMediaResource(): MediaResource? {
    val uri = coverUri ?: return null
    val mimeType = coverMimeType ?: return null
    return MediaResource(
        uri = uri,
        mimeType = mimeType
    )
}
