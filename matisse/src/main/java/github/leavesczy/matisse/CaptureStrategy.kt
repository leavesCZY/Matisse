package github.leavesczy.matisse

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
 * 拍照流程的存储策略。
 *
 * Matisse 会先调用 [shouldRequestWriteExternalStoragePermission]；在完成必要的存储写入与相机权限处理后，
 * 再调用 [createImageUri] 启动系统相机。相机以成功结果返回后调用 [loadCapturedMedia]；
 * 相机取消、拍照失败、[loadCapturedMedia] 返回 null，或再次启动拍照前清理仍挂起的 Uri 时，会调用
 * [deleteImageUri] 清理已创建的资源。若 [createImageUri] 返回 null，则不会调用 [deleteImageUri]
 * （尚无 Uri 可清理）。
 *
 * 实现会随 [Matisse] 或 [MatisseCapture] 通过 Intent 传递，因此实现类及其成员必须满足
 * [Parcelable] 要求。Matisse 从主线程发起拍照策略调用；实现不得阻塞调用线程，文件和
 * ContentResolver 操作应自行切换到合适的后台调度器。
 */
@Stable
interface CaptureStrategy : Parcelable {

    /**
     * 是否需要在拍照前申请 [Manifest.permission.WRITE_EXTERNAL_STORAGE]。
     *
     * 返回 true 时，宿主必须同时在 Manifest 中声明该权限。Android 10 及以上通常应返回 false。
     *
     * @param context 宿主应用的 Context
     * @return 是否需要在继续拍照前申请存储写入权限
     */
    fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean

    /**
     * 创建供外部相机写入的图片 Uri。
     *
     * Matisse 会通过 `MediaStore.EXTRA_OUTPUT` 传递该 Uri，并授予外部相机临时读写权限。
     *
     * @param context 宿主应用的 Context
     * @return 可写入的图片 Uri；返回 null 会取消本次拍照，且不会调用 [deleteImageUri]
     */
    suspend fun createImageUri(context: Context): Uri?

    /**
     * 在外部相机返回后读取拍照结果。
     *
     * @param context 宿主应用的 Context
     * @param imageUri [createImageUri] 创建的 Uri
     * @return 有效的媒体资源；返回 null 表示结果无效，随后会调用 [deleteImageUri]
     */
    suspend fun loadCapturedMedia(context: Context, imageUri: Uri): MediaResource?

    /**
     * 清理由 [createImageUri] 创建但未产生有效结果的资源。
     *
     * 相机取消、拍照失败、[loadCapturedMedia] 返回 null，以及再次启动拍照前清理仍挂起的 Uri 时会调用。
     * 注意：[createImageUri] 返回 null 时不会调用本方法。
     *
     * @param context 宿主应用的 Context
     * @param imageUri 需要清理的 Uri
     */
    suspend fun deleteImageUri(context: Context, imageUri: Uri)

    /**
     * 生成新图片的文件名。
     *
     * 默认返回格式为 `IMG_yyyyMMdd_HHmmssSSS.jpg` 的名称。
     *
     * @param context 宿主应用的 Context
     * @return 新图片使用的文件名
     */
    suspend fun createImageName(context: Context): String {
        val time = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
        return "IMG_$time.jpg"
    }

}

private const val JPG_MIME_TYPE = "image/jpeg"

/**
 * 使用 [FileProvider] 创建拍照 Uri 的策略。
 *
 * 宿主必须在 Manifest 中配置 FileProvider，并将其 `authority` 传给 [authority]。当前实现会在
 * `context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)` 中创建文件，FileProvider 路径配置
 * 必须能够映射该目录。照片保存在应用专属外部存储目录，不会写入系统相册，也不需要
 * [Manifest.permission.WRITE_EXTERNAL_STORAGE]。
 * 当前内置实现使用 `.jpg` 文件名，并将返回结果的 MIME 类型固定标记为 `image/jpeg`。
 * 读取结果时会校验文件长度大于 0，否则视为无效。
 *
 * 如果宿主在 Manifest 中声明了 [Manifest.permission.CAMERA]，Matisse 会在需要时申请该权限；
 * 未声明时则直接调用系统相机。
 *
 * @param authority 与宿主 FileProvider Manifest 配置完全一致的 authority
 */
@Parcelize
class FileProviderCaptureStrategy(private val authority: String) : CaptureStrategy {

    override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        return false
    }

    override suspend fun createImageUri(context: Context): Uri? {
        return withContext(context = Dispatchers.IO) {
            val tempFile = createTempFile(context = context) ?: return@withContext null
            FileProvider.getUriForFile(
                context,
                authority,
                tempFile
            )
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
        repeat(times = 10) {
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

    override suspend fun deleteImageUri(context: Context, imageUri: Uri) {
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

}

/**
 * 使用 MediaStore 创建拍照 Uri，并将照片写入系统相册的策略。
 *
 * Android 9 及以下，宿主必须在 Manifest 中声明 [Manifest.permission.WRITE_EXTERNAL_STORAGE]，
 * Matisse 会在拍照前申请该权限；Android 10 及以上无需该权限。
 * 当前内置实现使用 `.jpg` 文件名，创建 MediaStore 记录时声明 `image/jpeg`（不设置
 * `IS_PENDING`，以便系统相机可直接写入该 Uri）。相机返回后轮询查询该记录：Android 10
 * 及以上仅匹配非 pending，Android 11 及以上同时排除已移入回收站的记录；查到则返回其 MIME
 * 类型（通常仍为 `image/jpeg`），否则视为无效。
 *
 * 如果宿主在 Manifest 中声明了 [Manifest.permission.CAMERA]，Matisse 会在需要时申请该权限；
 * 未声明时则直接调用系统相机。
 */
@Parcelize
class MediaStoreCaptureStrategy : CaptureStrategy {

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
        return MediaProvider.createImageUri(
            context = context,
            imageName = createImageName(context = context),
            mimeType = JPG_MIME_TYPE
        )
    }

    override suspend fun loadCapturedMedia(context: Context, imageUri: Uri): MediaResource? {
        repeat(times = 10) {
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

    override suspend fun deleteImageUri(context: Context, imageUri: Uri) {
        MediaProvider.deleteMedia(context = context, uri = imageUri)
    }

}

/**
 * 根据系统版本选择存储方式的拍照策略。
 *
 * Android 9 及以下委托给 [fileProviderCaptureStrategy]，照片保存在应用专属外部存储目录；
 * Android 10 及以上委托给 [MediaStoreCaptureStrategy]，照片写入系统相册。
 * 因此，即使宿主仅在新系统上测试，也仍应按照 [FileProviderCaptureStrategy] 的要求完成
 * FileProvider 配置，以兼容 Android 9 及以下设备。
 *
 * @param fileProviderCaptureStrategy Android 9 及以下使用的 FileProvider 策略
 */
@Parcelize
data class SmartCaptureStrategy(private val fileProviderCaptureStrategy: FileProviderCaptureStrategy) :
    CaptureStrategy {

    @IgnoredOnParcel
    private val delegate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStoreCaptureStrategy()
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

    override suspend fun deleteImageUri(context: Context, imageUri: Uri) {
        delegate.deleteImageUri(context = context, imageUri = imageUri)
    }

    override suspend fun createImageName(context: Context): String {
        return delegate.createImageName(context = context)
    }

}
