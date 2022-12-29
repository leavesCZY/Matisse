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
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            val mediaResourceList = mutableListOf<MediaResource>()
            try {
                val mediaCursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder,
                ) ?: return@withContext null
                mediaCursor.use { cursor ->
                    while (cursor.moveToNext()) {
                        val data = cursor.getString(MediaStore.Images.Media.DATA)
                        if (data.isBlank() || !File(data).exists()) {
                            continue
                        }
                        val id = cursor.getLong(MediaStore.Images.Media._ID)
                        val displayName = cursor.getString(MediaStore.Images.Media.DISPLAY_NAME)
                        val mimeType = cursor.getString(MediaStore.Images.Media.MIME_TYPE)
                        val width = cursor.getInt(MediaStore.Images.Media.WIDTH)
                        val height = cursor.getInt(MediaStore.Images.Media.HEIGHT)
                        val size = cursor.getLong(MediaStore.Images.Media.SIZE)
                        val orientation = cursor.getInt(MediaStore.Images.Media.ORIENTATION)
                        val bucketId = cursor.getString(MediaStore.Images.Media.BUCKET_ID)
                        val bucketDisplayName =
                            cursor.getString(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        val mediaResource = MediaResource(
                            id = id,
                            uri = contentUri,
                            displayName = displayName,
                            mimeType = mimeType,
                            width = width,
                            height = height,
                            orientation = orientation,
                            path = data,
                            size = size,
                            bucketId = bucketId,
                            bucketDisplayName = bucketDisplayName,
                        )
                        mediaResourceList.add(mediaResource)
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
        filterMimeTypes: List<MimeType>
    ): List<MediaResource> {
        return withContext(context = Dispatchers.IO) {
            val selection = if (filterMimeTypes.isEmpty()) {
                null
            } else {
                val sb = StringBuilder()
                sb.append(MediaStore.Images.Media.MIME_TYPE)
                sb.append(" IN (")
                filterMimeTypes.forEachIndexed { index, mimeType ->
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

    suspend fun groupByBucket(resources: List<MediaResource>): List<MediaBucket> {
        return withContext(context = Dispatchers.IO) {
            val resourcesMap = linkedMapOf<String, MutableList<MediaResource>>()
            resources.forEach { res ->
                val bucketId = res.bucketId
                val list = resourcesMap[bucketId]
                if (list == null) {
                    resourcesMap[bucketId] = mutableListOf(res)
                } else {
                    list.add(res)
                }
            }
            val allMediaBucketResource = mutableListOf<MediaBucket>()
            resourcesMap.forEach {
                val resourcesList = it.value
                if (resourcesList.isNotEmpty()) {
                    val bucketId = it.key
                    val bucketDisplayName = resourcesList[0].bucketDisplayName
                    allMediaBucketResource.add(
                        MediaBucket(
                            bucketId = bucketId,
                            bucketDisplayName = bucketDisplayName,
                            bucketDisplayIcon = resourcesList[0].uri,
                            resources = resourcesList,
                            supportCapture = false
                        )
                    )
                }
            }
            return@withContext allMediaBucketResource
        }
    }

}


private fun Cursor.getInt(columnName: String): Int {
    val columnIndex = getColumnIndexOrThrow(columnName)
    return getInt(columnIndex)
}

private fun Cursor.getLong(columnName: String): Long {
    val columnIndex = getColumnIndexOrThrow(columnName)
    return getLong(columnIndex)
}

private fun Cursor.getString(columnName: String): String {
    val columnIndex = getColumnIndexOrThrow(columnName)
    return getString(columnIndex)
}