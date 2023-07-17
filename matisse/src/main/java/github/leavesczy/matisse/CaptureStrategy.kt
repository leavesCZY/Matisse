package github.leavesczy.matisse

import android.Manifest
import android.annotation.SuppressLint
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
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * @Author: CZY
 * @Date: 2022/6/6 14:20
 * @Desc:
 */
/**
 * 拍照策略
 */
@Stable
interface CaptureStrategy : Parcelable {

    /**
     * 是否启用拍照功能
     */
    fun isEnabled(): Boolean

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
    @SuppressLint("SimpleDateFormat")
    suspend fun createImageName(context: Context): String {
        return withContext(context = Dispatchers.IO) {
            val date = Date()
            val time = SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(date)
            val imageName = "IMG_" + time.format(date)
            return@withContext "$imageName.jpg"
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
 *  通过 FileProvider 生成 ImageUri
 *  外部必须配置 FileProvider，通过 authority 来实例化 FileProviderCaptureStrategy
 *  此策略无需申请任何权限，所拍的照片不会保存在系统相册里
 */
@Parcelize
data class FileProviderCaptureStrategy(
    private val authority: String,
    private val extra: Bundle = Bundle.EMPTY
) : CaptureStrategy {

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

    private suspend fun createTempFile(context: Context): File? {
        return withContext(context = Dispatchers.IO) {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(storageDir, createImageName(context = context))
            if (file.createNewFile()) {
                file
            } else {
                null
            }
        }
    }

    override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource {
        return withContext(context = Dispatchers.IO) {
            val imageFile = uriFileMap[imageUri]!!
            uriFileMap.remove(key = imageUri)
            val imageFilePath = imageFile.absolutePath
            val displayName = imageFile.name
            val mimeType = "image/jpeg"
            return@withContext MediaResource(
                id = 0,
                bucketId = "",
                bucketName = "",
                uri = imageUri,
                path = imageFilePath,
                name = displayName,
                mimeType = mimeType
            )
        }
    }

    override suspend fun onTakePictureCanceled(context: Context, imageUri: Uri) {
        withContext(context = Dispatchers.IO) {
            val imageFile = uriFileMap[imageUri]
            if (imageFile != null && imageFile.exists()) {
                imageFile.delete()
            }
            uriFileMap.remove(key = imageUri)
        }
    }

    override fun getCaptureExtra(): Bundle {
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
        return MediaProvider.createImage(
            context = context,
            imageName = createImageName(context = context),
            mimeType = "image/jpeg"
        )
    }

    override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource? {
        return MediaProvider.loadResources(context = context, uri = imageUri)
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
    private val authority: String,
    private val extra: Bundle = Bundle.EMPTY
) : CaptureStrategy {

    @IgnoredOnParcel
    private val proxy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStoreCaptureStrategy(extra = extra)
    } else {
        FileProviderCaptureStrategy(authority = authority, extra = extra)
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

    override suspend fun createImageName(context: Context): String {
        return proxy.createImageName(context = context)
    }

    override fun getCaptureExtra(): Bundle {
        return proxy.getCaptureExtra()
    }

}