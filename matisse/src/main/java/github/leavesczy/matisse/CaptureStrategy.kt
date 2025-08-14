package github.leavesczy.matisse

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import github.leavesczy.matisse.internal.logic.MediaProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @Author: leavesCZY
 * @Date: 2022/6/6 14:20
 * @Desc:
 */
/**
 * 拍照策略
 */
@Stable
interface CaptureStrategy : Parcelable {

    /**
     * 是否需要申请 WRITE_EXTERNAL_STORAGE 权限
     */
    fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean

    /**
     * 生成图片 Uri
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
     * 生成图片名
     */
    suspend fun createImageName(context: Context): String {
        return withContext(context = Dispatchers.Default) {
            val time = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
            "IMG_$time.jpg"
        }
    }

    /**
     * 用于为相机设置启动参数
     * 返回值会传递给启动相机的 Intent
     */
    fun getCaptureExtra(): Bundle {
        return Bundle.EMPTY
    }

}

private const val JPG_MIME_TYPE = "image/jpeg"

/**
 *  通过 FileProvider 生成 ImageUri
 *  外部必须配置 FileProvider，通过 authority 来实例化 FileProviderCaptureStrategy
 *  此策略无需申请任何权限，所拍的照片不会保存在系统相册里
 */
@Parcelize
open class FileProviderCaptureStrategy(
    private val authority: String,
    private val extra: Bundle = Bundle.EMPTY
) : CaptureStrategy {

    @IgnoredOnParcel
    private val uriFileMap = mutableMapOf<Uri, File>()

    final override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        return false
    }

    final override suspend fun createImageUri(context: Context): Uri? {
        return withContext(context = Dispatchers.Main.immediate) {
            val tempFile = createTempFile(context = context)
            if (tempFile != null) {
                val uri = FileProvider.getUriForFile(context, authority, tempFile)
                uriFileMap[uri] = tempFile
                uri
            } else {
                null
            }
        }
    }

    private suspend fun createTempFile(context: Context): File? {
        return withContext(context = Dispatchers.IO) {
            val picturesDirectory = getAuthorityDirectory(context = context)
            val file = File(picturesDirectory, createImageName(context = context))
            if (file.createNewFile()) {
                file
            } else {
                null
            }
        }
    }

    protected open fun getAuthorityDirectory(context: Context): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
    }

    final override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource {
        return withContext(context = Dispatchers.Main.immediate) {
            val imageFile = uriFileMap[imageUri]!!
            uriFileMap.remove(key = imageUri)
            MediaResource(
                uri = imageUri,
                path = imageFile.absolutePath,
                name = imageFile.name,
                mimeType = JPG_MIME_TYPE
            )
        }
    }

    final override suspend fun onTakePictureCanceled(context: Context, imageUri: Uri) {
        withContext(context = Dispatchers.Main.immediate) {
            val imageFile = uriFileMap[imageUri]
            uriFileMap.remove(key = imageUri)
            withContext(context = Dispatchers.IO) {
                if (imageFile != null && imageFile.exists()) {
                    imageFile.delete()
                }
            }
        }
    }

    final override fun getCaptureExtra(): Bundle {
        return extra
    }

}

/**
 *  通过 MediaStore 生成 ImageUri
 *  根据系统版本决定是否需要申请 WRITE_EXTERNAL_STORAGE 权限
 *  所拍的照片会保存在系统相册中
 */
@Parcelize
data class MediaStoreCaptureStrategy(private val extra: Bundle = Bundle.EMPTY) : CaptureStrategy {

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
        return MediaProvider.createImage(
            context = context,
            imageName = createImageName(context = context),
            mimeType = JPG_MIME_TYPE
        )
    }

    override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource? {
        return withContext(context = Dispatchers.Default) {
            repeat(times = 10) {
                val result = loadResources(context = context, uri = imageUri)
                if (result != null) {
                    return@withContext result
                }
                delay(timeMillis = 50L)
            }
            return@withContext null
        }
    }

    private suspend fun loadResources(context: Context, uri: Uri): MediaResource? {
        val resource = MediaProvider.loadResources(context = context, uri = uri)
        return if (resource == null) {
            null
        } else {
            MediaResource(
                uri = resource.uri,
                path = resource.path,
                name = resource.name,
                mimeType = resource.mimeType
            )
        }
    }

    override suspend fun onTakePictureCanceled(context: Context, imageUri: Uri) {
        MediaProvider.deleteMedia(context = context, uri = imageUri)
    }

    override fun getCaptureExtra(): Bundle {
        return extra
    }

}

/**
 * 根据系统版本智能选择拍照策略
 * 当系统版本小于 Android 10 时，执行 FileProviderCaptureStrategy 策略
 * 当系统版本大于等于 Android 10 时，执行 MediaStoreCaptureStrategy 策略
 * 既避免需要申请权限，又可以在系统允许的情况下将照片存入到系统相册中
 */
@Parcelize
data class SmartCaptureStrategy(
    private val fileProviderCaptureStrategy: FileProviderCaptureStrategy
) : CaptureStrategy {

    @IgnoredOnParcel
    private val proxy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStoreCaptureStrategy(extra = fileProviderCaptureStrategy.getCaptureExtra())
    } else {
        fileProviderCaptureStrategy
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

    override suspend fun createImageName(context: Context): String {
        return proxy.createImageName(context = context)
    }

    override fun getCaptureExtra(): Bundle {
        return proxy.getCaptureExtra()
    }

}