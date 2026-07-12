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
 * 拍照策略，定义 Uri 创建、结果读取与取消清理等行为
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
    suspend fun loadCapturedMedia(context: Context, imageUri: Uri): MediaResource?

    /**
     * 当用户取消拍照时调用
     */
    suspend fun onTakePictureCancelled(context: Context, imageUri: Uri)

    /**
     * 生成图片名
     */
    suspend fun createImageName(context: Context): String {
        val time = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
        return "IMG_$time.jpg"
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
 * 通过 FileProvider 生成 ImageUri
 * 外部必须配置 FileProvider，并通过 authority 来实例化 [FileProviderCaptureStrategy]
 * 无需申请 WRITE_EXTERNAL_STORAGE 权限；若宿主 App 在 Manifest 中声明了 CAMERA，则会在运行时按需申请相机权限
 * 所拍的照片保存在应用私有目录，不会写入系统相册
 */
@Parcelize
class FileProviderCaptureStrategy(
    private val authority: String,
    private val extra: Bundle = Bundle.EMPTY
) : CaptureStrategy {

    override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        return false
    }

    override suspend fun createImageUri(context: Context): Uri? {
        return withContext(context = Dispatchers.IO) {
            val tempFile = createTempFile(context = context) ?: return@withContext null
            FileProvider.getUriForFile(context, authority, tempFile)
        }
    }

    private suspend fun createTempFile(context: Context): File? {
        return withContext(context = Dispatchers.IO) {
            val picturesDirectory =
                getAuthorityDirectory(context = context) ?: return@withContext null
            val file = File(picturesDirectory, createImageName(context = context))
            if (file.createNewFile()) {
                file
            } else {
                null
            }
        }
    }

    override suspend fun loadCapturedMedia(context: Context, imageUri: Uri): MediaResource? {
        repeat(times = 5) {
            val imageFile = resolveImageFile(context = context, imageUri = imageUri)
            if (imageFile != null && imageFile.length() > 0L) {
                return MediaResource(
                    uri = imageUri,
                    mimeType = JPG_MIME_TYPE
                )
            }
            delay(timeMillis = 50L)
        }
        return null
    }

    override suspend fun onTakePictureCancelled(context: Context, imageUri: Uri) {
        withContext(context = Dispatchers.IO) {
            val imageFile = resolveImageFile(context = context, imageUri = imageUri)
            if (imageFile != null && imageFile.exists()) {
                imageFile.delete()
            }
        }
    }

    private fun getAuthorityDirectory(context: Context): File? {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    }

    private suspend fun resolveImageFile(context: Context, imageUri: Uri): File? {
        return withContext(context = Dispatchers.IO) {
            val fileName = imageUri.lastPathSegment
            val directory = getAuthorityDirectory(context = context)
            if (imageUri.authority != authority || fileName.isNullOrBlank() || directory == null) {
                return@withContext null
            }
            val file = File(directory, fileName)
            if (file.isFile && file.exists()) {
                file
            } else {
                null
            }
        }
    }

    override fun getCaptureExtra(): Bundle {
        return extra
    }

}

/**
 * 通过 MediaStore 生成 ImageUri
 * Android 10 以下需要申请 WRITE_EXTERNAL_STORAGE 权限；Android 10 及以上无需该权限
 * 若宿主 App 在 Manifest 中声明了 CAMERA，则会在运行时按需申请相机权限
 * 所拍的照片会写入系统相册
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

    override suspend fun loadCapturedMedia(context: Context, imageUri: Uri): MediaResource? {
        repeat(times = 5) {
            val resource = MediaProvider.loadMediaInfo(context = context, uri = imageUri)
            if (resource != null) {
                return MediaResource(
                    uri = resource.uri,
                    mimeType = resource.mimeType
                )
            }
            delay(timeMillis = 50L)
        }
        return null
    }

    override suspend fun onTakePictureCancelled(context: Context, imageUri: Uri) {
        MediaProvider.deleteMedia(context = context, uri = imageUri)
    }

    override fun getCaptureExtra(): Bundle {
        return extra
    }

}

/**
 * 根据系统版本智能选择拍照策略
 * 当系统版本小于 Android 10 时，委托 [FileProviderCaptureStrategy]
 * 当系统版本大于等于 Android 10 时，委托 [MediaStoreCaptureStrategy]
 * Android 10 及以上无需申请 WRITE_EXTERNAL_STORAGE 权限，照片会写入系统相册
 */
@Parcelize
data class SmartCaptureStrategy(
    private val fileProviderCaptureStrategy: FileProviderCaptureStrategy
) : CaptureStrategy {

    @IgnoredOnParcel
    private val delegate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStoreCaptureStrategy(extra = fileProviderCaptureStrategy.getCaptureExtra())
    } else {
        fileProviderCaptureStrategy
    }

    override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        return delegate.shouldRequestWriteExternalStoragePermission(context = context)
    }

    override suspend fun createImageUri(context: Context): Uri? {
        return delegate.createImageUri(context = context)
    }

    override suspend fun loadCapturedMedia(context: Context, imageUri: Uri): MediaResource? {
        return delegate.loadCapturedMedia(context = context, imageUri = imageUri)
    }

    override suspend fun onTakePictureCancelled(context: Context, imageUri: Uri) {
        delegate.onTakePictureCancelled(context = context, imageUri = imageUri)
    }

    override suspend fun createImageName(context: Context): String {
        return delegate.createImageName(context = context)
    }

    override fun getCaptureExtra(): Bundle {
        return delegate.getCaptureExtra()
    }

}