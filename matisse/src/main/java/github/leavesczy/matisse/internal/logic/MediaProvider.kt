package github.leavesczy.matisse.internal.logic

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import github.leavesczy.matisse.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal object MediaProvider {

    data class MediaInfo(
        val uri: Uri,
        val mimeType: String,
        val mediaId: Long,
        val bucketId: String,
        val bucketName: String
    )

    suspend fun createImage(
        context: Context,
        imageName: String,
        mimeType: String
    ): Uri? {
        return withContext(context = Dispatchers.Default) {
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
        withContext(context = Dispatchers.Default) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }
    }

    private suspend fun queryMediaInfoList(
        context: Context,
        selection: String?,
        selectionArgs: Array<String>?
    ): List<MediaInfo>? {
        return withContext(context = Dispatchers.Default) {
            val idColumn = MediaStore.MediaColumns._ID
            val pathColumn = MediaStore.MediaColumns.DATA
            val mimeTypeColumn = MediaStore.MediaColumns.MIME_TYPE
            val bucketIdColumn = MediaStore.MediaColumns.BUCKET_ID
            val bucketDisplayNameColumn = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
            val dateModifiedColumn = MediaStore.MediaColumns.DATE_MODIFIED
            val projection = arrayOf(
                idColumn,
                pathColumn,
                mimeTypeColumn,
                bucketIdColumn,
                bucketDisplayNameColumn
            )
            val contentUri = MediaStore.Files.getContentUri("external")
            val sortOrder = "$dateModifiedColumn DESC"
            val mediaInfoList = mutableListOf<MediaInfo>()
            try {
                val cursor = context.contentResolver.query(
                    contentUri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder,
                ) ?: return@withContext null
                cursor.use { cursor ->
                    while (cursor.moveToNext()) {
                        try {
                            val invalidId = Long.MAX_VALUE
                            val id = cursor.getLong(idColumn, invalidId)
                            val path = cursor.getString(pathColumn, "")
                            if (id == invalidId || path.isBlank()) {
                                continue
                            }
                            val file = File(path)
                            if (!file.isFile || !file.exists()) {
                                continue
                            }
                            val uri = ContentUris.withAppendedId(contentUri, id)
                            val bucketId = cursor.getString(bucketIdColumn, "")
                            val bucketName = cursor.getString(bucketDisplayNameColumn, "")
                            val mimeType = cursor.getString(mimeTypeColumn, "")
                            val mediaInfo = MediaInfo(
                                uri = uri,
                                mimeType = mimeType,
                                mediaId = id,
                                bucketId = bucketId,
                                bucketName = bucketName
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
            mediaInfoList
        }
    }

    suspend fun loadMediaInfoList(
        context: Context,
        mediaType: MediaType
    ): List<MediaInfo>? {
        return withContext(context = Dispatchers.Default) {
            queryMediaInfoList(
                context = context,
                selection = generateSqlSelection(mediaType = mediaType),
                selectionArgs = null
            )
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

    suspend fun loadMediaInfo(context: Context, uri: Uri): MediaInfo? {
        return withContext(context = Dispatchers.Default) {
            val id = ContentUris.parseId(uri)
            val selection = MediaStore.MediaColumns._ID + " = " + id
            val matchedMediaInfoList = queryMediaInfoList(
                context = context,
                selection = selection,
                selectionArgs = null
            )
            if (matchedMediaInfoList.isNullOrEmpty() || matchedMediaInfoList.size != 1) {
                null
            } else {
                matchedMediaInfoList[0]
            }
        }
    }

    private fun Cursor.getLong(columnName: String, default: Long): Long {
        return try {
            val columnIndex = getColumnIndexOrThrow(columnName)
            getLong(columnIndex)
        } catch (throwable: IllegalArgumentException) {
            throwable.printStackTrace()
            default
        }
    }

    private fun Cursor.getString(columnName: String, default: String): String {
        return try {
            val columnIndex = getColumnIndexOrThrow(columnName)
            getString(columnIndex) ?: default
        } catch (throwable: IllegalArgumentException) {
            throwable.printStackTrace()
            default
        }
    }

}