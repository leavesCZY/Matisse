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
import kotlinx.coroutines.withContext

internal object MediaProvider {

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

    suspend fun createImage(
        context: Context,
        imageName: String,
        mimeType: String
    ): Uri? {
        return withContext(context = Dispatchers.IO) {
            try {
                val contentValues = ContentValues()
                contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
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
            val selectionParts = mutableListOf(
                withMediaStoreStateSelection(
                    selection = generateSqlSelection(mediaType = mediaType)
                )
            )
            val selectionArgs = mutableListOf<String>()
            if (!bucketId.isNullOrBlank()) {
                selectionParts.add("${MediaStore.MediaColumns.BUCKET_ID} = ?")
                selectionArgs.add(bucketId)
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
            val cursor = queryMediaCursor(
                contentResolver = context.contentResolver,
                contentUri = contentUri,
                projection = projection,
                selection = withMediaStoreStateSelection(
                    selection = generateSqlSelection(mediaType = mediaType)
                ),
                selectionArgs = null,
                limit = null,
                offset = null,
                groupBy = bucketIdColumn,
                sortOrder = "$bucketDisplayNameColumn ASC"
            ) ?: return null
            cursor.use { cursor ->
                val bucketIdIndex = cursor.getColumnIndexOrThrow(bucketIdColumn)
                val bucketNameIndex = cursor.getColumnIndexOrThrow(bucketDisplayNameColumn)
                val countIndex = cursor.indexOfCountColumn(countSql = countColumn)
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
        val aggregates = ArrayList<MediaBucketAggregate>(summaries.size)
        for (summary in summaries) {
            val cover = loadMediaInfoPage(
                context = context,
                mediaType = mediaType,
                bucketId = summary.bucketId,
                limit = 1,
                offset = 0
            ).firstOrNull() ?: continue
            aggregates.add(
                element = MediaBucketAggregate(
                    bucketId = summary.bucketId,
                    bucketName = summary.bucketName,
                    itemCount = summary.itemCount,
                    coverUri = cover.uri,
                    coverMimeType = cover.mimeType
                )
            )
        }
        return aggregates
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
            val cursor = queryMediaCursor(
                contentResolver = context.contentResolver,
                contentUri = contentUri,
                projection = projection,
                selection = withMediaStoreStateSelection(
                    selection = generateSqlSelection(mediaType = mediaType)
                ),
                selectionArgs = null,
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

    private fun Cursor.indexOfCountColumn(countSql: String): Int {
        val exactIndex = getColumnIndex(countSql)
        if (exactIndex >= 0) {
            return exactIndex
        }
        for (index in 0 until columnCount) {
            if (getColumnName(index).contains(other = "COUNT", ignoreCase = true)) {
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
     * Prefer whichever is newer between generation/import time and last modification.
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

    private fun generateSqlSelection(mediaType: MediaType): String {
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
                queryImageSelection
            }

            MediaType.VideoOnly -> {
                queryVideoSelection
            }

            is MediaType.ImageAndVideo -> {
                buildString {
                    append(queryImageSelection)
                    append(" or ")
                    append(queryVideoSelection)
                }
            }

            is MediaType.MultipleMimeType -> {
                mediaType.mimeTypes.joinToString(
                    prefix = "$mimeTypeColumn in (",
                    postfix = ")",
                    separator = ",",
                    transform = {
                        "'${it}'"
                    }
                )
            }
        }
    }

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
