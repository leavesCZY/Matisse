package github.leavesczy.matisse.internal.logic

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @Author: CZY
 * @Date: 2022/6/2 11:11
 * @Desc:
 */
internal object MediaProvider {

    suspend fun createImage(context: Context, fileName: String): Uri? {
        return withContext(context = Dispatchers.IO) {
            return@withContext try {
                val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val newImage = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                }
                context.contentResolver.insert(imageCollection, newImage)
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun deleteImage(context: Context, imageUri: Uri) {
        withContext(context = Dispatchers.IO) {
            try {
                context.contentResolver.delete(imageUri, null, null)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadResources(
        context: Context,
        selection: String?,
        selectionArgs: Array<String>?
    ): List<MediaResource>? {
        return withContext(context = Dispatchers.IO) {
            val idColumn = MediaStore.Images.Media._ID
            val dataColumn = MediaStore.Images.Media.DATA
            val displayNameColumn = MediaStore.Images.Media.DISPLAY_NAME
            val mineTypeColumn = MediaStore.Images.Media.MIME_TYPE
            val widthColumn = MediaStore.Images.Media.WIDTH
            val heightColumn = MediaStore.Images.Media.HEIGHT
            val orientationColumn = MediaStore.Images.Media.ORIENTATION
            val sizeColumn = MediaStore.Images.Media.SIZE
            val bucketIdColumn = MediaStore.Images.Media.BUCKET_ID
            val bucketDisplayNameColumn = MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            val externalContentUriColumn = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                idColumn,
                dataColumn,
                displayNameColumn,
                mineTypeColumn,
                widthColumn,
                heightColumn,
                orientationColumn,
                sizeColumn,
                bucketIdColumn,
                bucketDisplayNameColumn
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            val mediaResourceList = mutableListOf<MediaResource>()
            try {
                val mediaCursor = context.contentResolver.query(
                    externalContentUriColumn,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder,
                ) ?: return@withContext null
                mediaCursor.use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn, Long.MAX_VALUE)
                        val data = cursor.getString(dataColumn, "")
                        if (id == Long.MAX_VALUE || data.isBlank() || !File(data).exists()) {
                            continue
                        }
                        val displayName = cursor.getString(displayNameColumn, "")
                        val mimeType = cursor.getString(mineTypeColumn, "")
                        val width = cursor.getInt(widthColumn, 0)
                        val height = cursor.getInt(heightColumn, 0)
                        val orientation = cursor.getInt(orientationColumn, 0)
                        val size = cursor.getLong(sizeColumn, 0L)
                        val bucketId = cursor.getString(bucketIdColumn, "")
                        val bucketDisplayName = cursor.getString(bucketDisplayNameColumn, "")
                        val imageUri = ContentUris.withAppendedId(externalContentUriColumn, id)
                        val mediaResource = MediaResource(
                            id = id,
                            path = data,
                            uri = imageUri,
                            displayName = displayName,
                            mimeType = mimeType,
                            width = width,
                            height = height,
                            orientation = orientation,
                            size = size,
                            bucketId = bucketId,
                            bucketDisplayName = bucketDisplayName
                        )
                        mediaResourceList.add(element = mediaResource)
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            return@withContext mediaResourceList
        }
    }

    suspend fun loadResources(
        context: Context,
        supportedMimeTypes: List<MimeType>
    ): List<MediaResource> {
        return withContext(context = Dispatchers.IO) {
            val selection = if (supportedMimeTypes.isEmpty()) {
                null
            } else {
                val sb = StringBuilder()
                sb.append(MediaStore.Images.Media.MIME_TYPE)
                sb.append(" IN (")
                supportedMimeTypes.forEachIndexed { index, mimeType ->
                    if (index != 0) {
                        sb.append(",")
                    }
                    sb.append("'")
                    sb.append(mimeType.type)
                    sb.append("'")
                }
                sb.append(")")
                sb.toString()
            }
            return@withContext loadResources(
                context = context,
                selection = selection,
                selectionArgs = null
            ) ?: emptyList()
        }
    }

    suspend fun loadResources(context: Context, uri: Uri): MediaResource? {
        return withContext(context = Dispatchers.IO) {
            val id = ContentUris.parseId(uri)
            if (id == -1L) {
                return@withContext null
            }
            val selection = MediaStore.Images.Media._ID + " = " + id
            val resources =
                loadResources(context = context, selection = selection, selectionArgs = null)
            if (resources.isNullOrEmpty() || resources.size != 1) {
                return@withContext null
            }
            return@withContext resources[0]
        }
    }

}

private fun Cursor.getInt(columnName: String, default: Int): Int {
    return try {
        val columnIndex = getColumnIndexOrThrow(columnName)
        getInt(columnIndex)
    } catch (e: Throwable) {
        e.printStackTrace()
        default
    }
}

private fun Cursor.getLong(columnName: String, default: Long): Long {
    return try {
        val columnIndex = getColumnIndexOrThrow(columnName)
        getLong(columnIndex)
    } catch (e: Throwable) {
        e.printStackTrace()
        default
    }
}

private fun Cursor.getString(columnName: String, default: String): String {
    return try {
        val columnIndex = getColumnIndexOrThrow(columnName)
        getString(columnIndex) ?: default
    } catch (e: Throwable) {
        e.printStackTrace()
        default
    }
}