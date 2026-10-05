package github.leavesczy.matisse.internal.logic

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import github.leavesczy.matisse.MediaResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * [CoilImageEngine] 与 [GlideImageEngine] 共用的 MediaStore 系统缩略图能力。
 *
 * Android 10 起 MediaProvider 会为图片和视频生成并持久化缓存约 512px 的缩略图，读取它只需一次
 * 跨进程调用和一次小图解码，远比解码原图或抽取视频帧便宜。两个引擎都把它接入各自的加载管线，
 * 由引擎负责内存缓存与 Bitmap 复用：Glide 将编码流交给 Downsampler，Coil 交给 ImageLoader 的 Decoder。
 *
 * 系统缩略图以 [MAX_DIMENSION] 为目标尺寸读取（系统只做整数倍采样，返回位图的长边可能大于该值，
 * 但小于其 2 倍），因此只在目标尺寸不超过该值时使用，更大的目标回退为解码原图，避免放大模糊。
 */
internal object MediaStoreThumbnail {

    /** 使用系统缩略图的目标尺寸上限，同时作为请求系统缩略图时传入的尺寸。 */
    const val MAX_DIMENSION = 512

    private const val MEMORY_CACHE_KEY_PREFIX = "matisse#thumbnail#"

    /**
     * 返回可用于读取系统缩略图的 Uri；系统版本低于 Android 10、非 MediaStore Uri
     * （例如 FileProvider）或既非图片也非视频时返回 null。
     *
     * 选择器查询使用 `MediaStore.Files` 集合，而 MediaProvider 只为 Images / Video 集合的 Uri
     * 提供缩略图，因此按 MIME 类型换算为对应集合下的同一 `_ID`。
     */
    fun thumbnailUri(mediaResource: MediaResource): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }
        val uri = mediaResource.uri
        if (uri.scheme != ContentResolver.SCHEME_CONTENT || uri.authority != MediaStore.AUTHORITY) {
            return null
        }
        val mediaId = try {
            ContentUris.parseId(uri)
        } catch (_: Throwable) {
            return null
        }
        if (mediaId < 0L) {
            return null
        }
        val volumeName = uri.pathSegments.firstOrNull() ?: return null
        val collection = when {
            mediaResource.isImage -> {
                MediaStore.Images.Media.getContentUri(volumeName)
            }

            mediaResource.isVideo -> {
                MediaStore.Video.Media.getContentUri(volumeName)
            }

            else -> {
                return null
            }
        }
        return ContentUris.withAppendedId(collection, mediaId)
    }

    /** 宽、高均不超过 [MAX_DIMENSION] 时走系统缩略图；≤0 视为未知尺寸，同样走系统缩略图。 */
    fun isSizeSupported(width: Int, height: Int): Boolean {
        return width <= MAX_DIMENSION && height <= MAX_DIMENSION
    }

    /** 网格、相册封面与预览占位共用的内存缓存 key，以原始 Uri 区分，不含格子尺寸。 */
    fun memoryCacheKey(mediaResource: MediaResource): String {
        return MEMORY_CACHE_KEY_PREFIX + mediaResource.uri
    }

    /** 打开系统缩略图编码流；协程取消时通过 [CancellationSignal] 中止读取。 */
    suspend fun openStream(
        context: Context,
        thumbnailUri: Uri
    ): ThumbnailStream {
        return runCancellable { cancellationSignal ->
            openStream(
                context = context,
                thumbnailUri = thumbnailUri,
                cancellationSignal = cancellationSignal
            )
        }
    }

    /**
     * 打开系统缩略图的编码数据流，交由图片引擎自行解码以复用其位图池。
     * 调用方负责关闭 [ThumbnailStream.fileDescriptor]，并按 [ThumbnailStream.rotationDegrees] 旋转。
     * 低于 Android 10 时抛出 [UnsupportedOperationException]。
     */
    fun openStream(
        context: Context,
        thumbnailUri: Uri,
        cancellationSignal: CancellationSignal
    ): ThumbnailStream {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw UnsupportedOperationException("MediaStore thumbnail requires Android 10")
        }
        return openStreamApi29(
            context = context,
            thumbnailUri = thumbnailUri,
            cancellationSignal = cancellationSignal
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun openStreamApi29(
        context: Context,
        thumbnailUri: Uri,
        cancellationSignal: CancellationSignal
    ): ThumbnailStream {
        val options = Bundle().apply {
            putParcelable(ContentResolver.EXTRA_SIZE, Point(MAX_DIMENSION, MAX_DIMENSION))
        }
        val fileDescriptor = context.contentResolver.openTypedAssetFile(
            thumbnailUri,
            "image/*",
            options,
            cancellationSignal
        ) ?: throw IllegalStateException("MediaStore returned no thumbnail for $thumbnailUri")
        // 部分缩略图来自 EXIF 内嵌小图，不含原图旋转信息，按 extras 中的角度旋转
        val rotationDegrees =
            fileDescriptor.extras?.getInt(DocumentsContract.EXTRA_ORIENTATION, 0) ?: 0
        return ThumbnailStream(
            fileDescriptor = fileDescriptor,
            rotationDegrees = rotationDegrees
        )
    }

    class ThumbnailStream(
        val fileDescriptor: AssetFileDescriptor,
        val rotationDegrees: Int
    )

    /**
     * 在 IO 线程执行阻塞的 ContentResolver 调用，协程取消时通过 [CancellationSignal]
     * 通知 MediaProvider 中止生成或读取缩略图。
     */
    private suspend fun <T> runCancellable(block: (CancellationSignal) -> T): T {
        val cancellationSignal = CancellationSignal()
        return coroutineScope {
            val deferred = async(context = Dispatchers.IO) {
                block(cancellationSignal)
            }
            try {
                deferred.await()
            } catch (cancellation: CancellationException) {
                cancellationSignal.cancel()
                throw cancellation
            }
        }
    }

}
