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
        val bucketName: String
    )

    data class MediaBucketAggregate(
        val bucketId: String,
        val bucketName: String,
        val itemCount: Int,
        val coverUri: Uri,
        val coverMimeType: String
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
                throwable.printStackTrace()
                null
            }
        }
    }

    suspend fun deleteMedia(context: Context, uri: Uri) {
        withContext(context = Dispatchers.IO) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }
    }

    suspend fun loadMediaInfoPage(
        context: Context,
        mediaType: MediaType,
        bucketId: String?,
        limit: Int,
        offset: Int
    ): List<MediaInfo> {
        return withContext(context = Dispatchers.IO) {
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
            queryMediaInfoList(
                context = context,
                selection = selectionParts.joinToString(separator = " AND ") { "($it)" },
                selectionArgs = selectionArgs.toTypedArray(),
                limit = limit,
                offset = offset
            ).orEmpty()
        }
    }

    suspend fun loadMediaBuckets(
        context: Context,
        mediaType: MediaType
    ): List<MediaBucketAggregate> {
        return withContext(context = Dispatchers.IO) {
            val summaries = queryGroupedBucketSummaries(
                context = context,
                mediaType = mediaType
            )
            if (summaries != null) {
                loadBucketCovers(
                    context = context,
                    mediaType = mediaType,
                    summaries = summaries
                )
            } else {
                queryBucketsByFullScan(
                    context = context,
                    mediaType = mediaType
                )
            }
        }
    }

    /**
     * GROUP BY 只取相册数量；封面再按与网格相同的 recency 排序各查 1 条（有限并发），
     * 避免 MAX(_ID) 与列表不一致，并直接带上 MIME。聚合失败时返回 null，回退全表扫描。
     */
    private fun queryGroupedBucketSummaries(
        context: Context,
        mediaType: MediaType
    ): List<BucketSummary>? {
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
        try {
            val mediaSelection = generateSqlSelection(mediaType = mediaType)
            val cursor = queryMediaCursor(
                contentResolver = context.contentResolver,
                contentUri = contentUri,
                projection = projection,
                selection = withMediaStoreStateSelection(selection = mediaSelection.selection),
                selectionArgs = mediaSelection.selectionArgs.takeIf { it.isNotEmpty() },
                limit = null,
                offset = null,
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
                    val bucketId = cursor.getString(bucketIdIndex).orEmpty()
                    val bucketName = cursor.getString(bucketNameIndex).orEmpty()
                    if (bucketId.isBlank() || bucketName.isBlank()) {
                        continue
                    }
                    val itemCount = cursor.getInt(countIndex)
                    if (itemCount <= 0) {
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
            throwable.printStackTrace()
            return null
        }
        return summaries
    }

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
                        val cover = loadMediaInfoPage(
                            context = context,
                            mediaType = mediaType,
                            bucketId = summary.bucketId,
                            limit = 1,
                            offset = 0
                        ).firstOrNull() ?: return@withPermit null
                        MediaBucketAggregate(
                            bucketId = summary.bucketId,
                            bucketName = summary.bucketName,
                            itemCount = summary.itemCount,
                            coverUri = cover.uri,
                            coverMimeType = cover.mimeType
                        )
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private fun queryBucketsByFullScan(
        context: Context,
        mediaType: MediaType
    ): List<MediaBucketAggregate> {
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
        try {
            val mediaSelection = generateSqlSelection(mediaType = mediaType)
            val cursor = queryMediaCursor(
                contentResolver = context.contentResolver,
                contentUri = contentUri,
                projection = projection,
                selection = withMediaStoreStateSelection(selection = mediaSelection.selection),
                selectionArgs = mediaSelection.selectionArgs.takeIf { it.isNotEmpty() },
                limit = null,
                offset = null
            ) ?: return emptyList()
            cursor.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(idColumn)
                val mimeTypeIndex = cursor.getColumnIndexOrThrow(mimeTypeColumn)
                val bucketIdIndex = cursor.getColumnIndexOrThrow(bucketIdColumn)
                val bucketNameIndex = cursor.getColumnIndexOrThrow(bucketDisplayNameColumn)
                while (cursor.moveToNext()) {
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
                        throwable.printStackTrace()
                    }
                }
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
        return aggregates.values.map { aggregate ->
            MediaBucketAggregate(
                bucketId = aggregate.bucketId,
                bucketName = aggregate.bucketName,
                itemCount = aggregate.itemCount,
                coverUri = aggregate.coverUri,
                coverMimeType = aggregate.coverMimeType
            )
        }
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
        limit: Int?,
        offset: Int? = null
    ): List<MediaInfo>? {
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
        val mediaInfoList = mutableListOf<MediaInfo>()
        try {
            val cursor = queryMediaCursor(
                contentResolver = context.contentResolver,
                contentUri = contentUri,
                projection = projection,
                selection = selection,
                selectionArgs = selectionArgs,
                limit = limit,
                offset = offset
            ) ?: return null
            cursor.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(idColumn)
                val mimeTypeIndex = cursor.getColumnIndexOrThrow(mimeTypeColumn)
                val bucketIdIndex = cursor.getColumnIndexOrThrow(bucketIdColumn)
                val bucketNameIndex = cursor.getColumnIndexOrThrow(bucketDisplayNameColumn)
                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idIndex)
                        val uri = ContentUris.withAppendedId(contentUri, id)
                        val mediaInfo = MediaInfo(
                            uri = uri,
                            mimeType = cursor.getString(mimeTypeIndex).orEmpty(),
                            mediaId = id,
                            bucketId = cursor.getString(bucketIdIndex).orEmpty(),
                            bucketName = cursor.getString(bucketNameIndex).orEmpty()
                        )
                        mediaInfoList.add(element = mediaInfo)
                    } catch (throwable: Throwable) {
                        throwable.printStackTrace()
                    }
                }
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
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
        offset: Int?,
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
                if (offset != null) {
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                }
            }
            contentResolver.query(contentUri, projection, queryArgs, null)
        } else {
            val groupedSelection = if (groupBy != null) {
                val baseSelection = selection ?: "1"
                "$baseSelection) GROUP BY ($groupBy"
            } else {
                selection
            }
            val pagedSortOrder = if (limit != null) {
                val safeOffset = offset ?: 0
                "$sortOrder LIMIT $limit OFFSET $safeOffset"
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
     * 取 DATE_ADDED 与 DATE_MODIFIED 中较新者作为排序时间，其次按 `_ID` 降序。
     */
    private fun mediaRecencySortOrder(): String {
        val dateAddedColumn = MediaStore.MediaColumns.DATE_ADDED
        val dateModifiedColumn = MediaStore.MediaColumns.DATE_MODIFIED
        val idColumn = MediaStore.MediaColumns._ID
        return "(CASE WHEN $dateAddedColumn > $dateModifiedColumn THEN $dateAddedColumn ELSE $dateModifiedColumn END) DESC, $idColumn DESC"
    }

    suspend fun loadMediaInfo(context: Context, uri: Uri): MediaInfo? {
        return withContext(context = Dispatchers.IO) {
            val id = ContentUris.parseId(uri)
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

    private fun withMediaStoreStateSelection(selection: String): String {
        val stateSelection = mediaStoreStateSelection()
        return if (stateSelection.isBlank()) {
            selection
        } else {
            "($selection) AND ($stateSelection)"
        }
    }

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
            is MediaType.ImageOnly -> {
                MediaSqlSelection(selection = queryImageSelection)
            }

            MediaType.VideoOnly -> {
                MediaSqlSelection(selection = queryVideoSelection)
            }

            is MediaType.ImageAndVideo -> {
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

    private class MutableBucketAggregate(
        val bucketId: String,
        val bucketName: String,
        var itemCount: Int,
        val coverUri: Uri,
        val coverMimeType: String
    )

}

internal fun MediaProvider.MediaBucketAggregate.toCoverMediaResource(): MediaResource {
    return MediaResource(
        uri = coverUri,
        mimeType = coverMimeType
    )
}
