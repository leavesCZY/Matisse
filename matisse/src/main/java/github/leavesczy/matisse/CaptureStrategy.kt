package github.leavesczy.matisse

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import github.leavesczy.matisse.internal.logic.MediaProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.*
import kotlin.math.max

/**
 * @Author: CZY
 * @Date: 2022/6/6 14:20
 * @Desc:
 */
/**
 * 拍照策略
 */
interface CaptureStrategy : Parcelable {

    /**
     * 是否启用拍照功能
     */
    fun isEnabled(): Boolean

    /**
     * 是否需要申请读取存储卡的权限
     */
    fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean

    /**
     * 获取用于存储拍照结果的 Uri
     */
    suspend fun createImageUri(context: Context): Uri?

    /**
     * 获取拍照结果
     */
    suspend fun loadResource(context: Context, imageUri: Uri): MediaResource?

    /**
     * 当用户取消拍照时调用
     */
    suspend fun onTakePictureCanceled(context: Context, imageUri: Uri)

    /**
     * 生成图片文件名
     */
    fun createImageName(): String {
        val uuid = UUID.randomUUID().toString()
        val randomName = uuid.substring(0, 8)
        return "$randomName.jpg"
    }

}

/**
 *  不开启拍照功能
 */
@Parcelize
object NothingCaptureStrategy : CaptureStrategy {

    override fun isEnabled(): Boolean {
        return false
    }

    override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        return false
    }

    override suspend fun createImageUri(context: Context): Uri? {
        return null
    }

    override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource? {
        return null
    }

    override suspend fun onTakePictureCanceled(context: Context, imageUri: Uri) {

    }

}

/**
 *  通过 FileProvider 来生成拍照所需要的 ImageUri
 *  无需申请权限，所拍的照片不会保存在系统相册里
 *  外部必须配置 FileProvider，并在此处传入 authority
 */
@Parcelize
class FileProviderCaptureStrategy(private val authority: String) : CaptureStrategy {

    @IgnoredOnParcel
    private val uriFileMap = mutableMapOf<Uri, File>()

    override fun isEnabled(): Boolean {
        return true
    }

    override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        return false
    }

    override suspend fun createImageUri(context: Context): Uri? {
        return withContext(context = Dispatchers.IO) {
            try {
                val tempFile = createTempFile(context = context)
                if (tempFile != null) {
                    val uri = FileProvider.getUriForFile(context, authority, tempFile)
                    uriFileMap[uri] = tempFile
                    return@withContext uri
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    private fun createTempFile(context: Context): File? {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, createImageName())
        if (file.createNewFile()) {
            return file
        }
        return null
    }

    override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource {
        return withContext(context = Dispatchers.IO) {
            val imageFile = uriFileMap[imageUri]!!
            uriFileMap.remove(imageUri)
            val imageFilePath = imageFile.absolutePath
            val option = BitmapFactory.Options()
            option.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imageFilePath, option)
            return@withContext MediaResource(
                id = 0,
                uri = imageUri,
                displayName = imageFile.name,
                mimeType = option.outMimeType ?: "",
                width = max(option.outWidth, 0),
                height = max(option.outHeight, 0),
                orientation = 0,
                size = imageFile.length(),
                path = imageFile.absolutePath,
                bucketId = "",
                bucketDisplayName = ""
            )
        }
    }

    override suspend fun onTakePictureCanceled(context: Context, imageUri: Uri) {
        withContext(context = Dispatchers.IO) {
            val imageFile = uriFileMap[imageUri]!!
            uriFileMap.remove(imageUri)
            if (imageFile.exists()) {
                imageFile.delete()
            }
        }
    }

}

/**
 *  通过 MediaStore 来生成拍照所需要的 ImageUri
 *  根据系统版本决定是否需要申请 WRITE_EXTERNAL_STORAGE 权限
 *  所拍的照片会保存在系统相册里
 */
@Parcelize
class MediaStoreCaptureStrategy : CaptureStrategy {

    override fun isEnabled(): Boolean {
        return true
    }

    override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return false
        }
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_DENIED
    }

    override suspend fun createImageUri(context: Context): Uri? {
        return MediaProvider.createImage(context = context, fileName = createImageName())
    }

    override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource? {
        return MediaProvider.loadResources(
            context = context,
            uri = imageUri
        )
    }

    override suspend fun onTakePictureCanceled(context: Context, imageUri: Uri) {
        MediaProvider.deleteImage(context = context, imageUri = imageUri)
    }

}

/**
 * 根据系统版本智能选择拍照策略
 * 既避免需要申请权限，又可以在系统允许的情况下将拍照所得照片存入到系统相册中
 * 系统版本小于 Android 10，则执行 FileProviderCaptureStrategy 策略
 * 系统版本大于等于 Android 10，则执行 MediaStoreCaptureStrategy 策略
 */
@Parcelize
@Suppress("CanBeParameter")
class SmartCaptureStrategy(private val authority: String) : CaptureStrategy {

    @IgnoredOnParcel
    private val proxy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStoreCaptureStrategy()
    } else {
        FileProviderCaptureStrategy(authority = authority)
    }

    override fun isEnabled(): Boolean {
        return proxy.isEnabled()
    }

    override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        return proxy.shouldRequestWriteExternalStoragePermission(context = context)
    }

    override suspend fun createImageUri(context: Context): Uri? {
        return proxy.createImageUri(context = context)
    }

    override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource? {
        return proxy.loadResource(context = context, imageUri = imageUri)
    }

    override suspend fun onTakePictureCanceled(context: Context, imageUri: Uri) {
        proxy.onTakePictureCanceled(context = context, imageUri = imageUri)
    }

}