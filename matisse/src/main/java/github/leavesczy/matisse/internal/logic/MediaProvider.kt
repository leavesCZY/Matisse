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

    suspend fun deleteMedia(context: Context, uri: Uri) {
        withContext(context = Dispatchers.IO) {
            try {
                context.contentResolver.delete(uri, null, null)
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
            val idColumn = MediaStore.MediaColumns._ID
            val dataColumn = MediaStore.MediaColumns.DATA
            val displayNameColumn = MediaStore.MediaColumns.DISPLAY_NAME
            val mineTypeColumn = MediaStore.MediaColumns.MIME_TYPE
            val bucketIdColumn = MediaStore.MediaColumns.BUCKET_ID
            val bucketDisplayNameColumn = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
            val dateModifiedColumn = MediaStore.MediaColumns.DATE_MODIFIED
            val projection = arrayOf(
                idColumn,
                dataColumn,
                displayNameColumn,
                mineTypeColumn,
                bucketIdColumn,
                bucketDisplayNameColumn
            )
            val contentUri = MediaStore.Files.getContentUri("external")
            val sortOrder = "$dateModifiedColumn DESC"
            val mediaResourceList = mutableListOf<MediaResource>()
            try {
                val mediaCursor = context.contentResolver.query(
                    contentUri,
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
                        val bucketId = cursor.getString(bucketIdColumn, "")
                        val bucketDisplayName = cursor.getString(bucketDisplayNameColumn, "")
                        val uri = ContentUris.withAppendedId(contentUri, id)
                        val mediaResource = MediaResource(
                            id = id,
                            path = data,
                            uri = uri,
                            displayName = displayName,
                            mimeType = mimeType,
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
        mimeTypes: List<MimeType>
    ): List<MediaResource> {
        return withContext(context = Dispatchers.IO) {
            val selection = StringBuilder()
            selection.append(MediaStore.MediaColumns.MIME_TYPE)
            selection.append(" IN (")
            mimeTypes.forEachIndexed { index, mimeType ->
                if (index != 0) {
                    selection.append(",")
                }
                selection.append("'")
                selection.append(mimeType.type)
                selection.append("'")
            }
            selection.append(")")
            return@withContext loadResources(
                context = context,
                selection = selection.toString(),
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
            val selection = MediaStore.MediaColumns._ID + " = " + id
            val resources = loadResources(
                context = context,
                selection = selection,
                selectionArgs = null
            )
            if (resources.isNullOrEmpty() || resources.size != 1) {
                return@withContext null
            }
            return@withContext resources[0]
        }
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