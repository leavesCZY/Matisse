package github.leavesczy.matisse

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 17:45
 * @Desc:
 */
/**
 * @param maxSelectable 最多允许选择几个媒体资源
 * @param mediaFilter 用于定义媒体资源的加载和过滤规则
 * @param imageEngine 用于实现加载图片的逻辑
 * @param singleMediaType 用于设置是否允许用户同时选择图片和视频。为 true 则用户只能选择一种媒体类型，为 false 则允许同时选择图片和视频
 * @param captureStrategy 拍照策略。默认不开启拍照功能
 */
@Stable
@Parcelize
data class Matisse(
    val maxSelectable: Int,
    val mediaFilter: MediaFilter,
    val imageEngine: ImageEngine,
    val singleMediaType: Boolean = false,
    val captureStrategy: CaptureStrategy? = null
) : Parcelable {

    init {
        assert(value = maxSelectable >= 1)
        assert(value = mediaFilter.supportedMimeTypes().isNotEmpty())
    }

}

/**
 * @param captureStrategy 拍照策略
 */
@Parcelize
data class MatisseCapture(
    val captureStrategy: CaptureStrategy
) : Parcelable

@Stable
@Parcelize
data class MediaResource(
    internal val id: Long,
    internal val bucketId: String,
    internal val bucketName: String,
    val uri: Uri,
    val path: String,
    val name: String,
    val mimeType: String,
) : Parcelable {

    val isImage: Boolean
        get() = mimeType.startsWith(prefix = "image")

    val isVideo: Boolean
        get() = mimeType.startsWith(prefix = "video")

}

enum class MimeType(val type: String) {
    JPEG(type = "image/jpeg"),
    PNG(type = "image/png"),
    WEBP(type = "image/webp"),
    HEIC(type = "image/heic"),
    HEIF(type = "image/heif"),
    BMP(type = "image/x-ms-bmp"),
    GIF(type = "image/gif"),
    MPEG(type = "video/mpeg"),
    MP4(type = "video/mp4"),
    QUICKTIME(type = "video/quicktime"),
    THREEGPP(type = "video/3gpp"),
    THREEGPP2(type = "video/3gpp2"),
    MKV(type = "video/x-matroska"),
    WEBM(type = "video/webm"),
    TS(type = "video/mp2ts"),
    AVI(type = "video/avi");

    companion object {

        fun ofAll(hasGif: Boolean = true): Set<MimeType> {
            return if (hasGif) {
                entries.toSet()
            } else {
                entries.filter { it != GIF }.toSet()
            }
        }

        fun ofImage(hasGif: Boolean = true): Set<MimeType> {
            return if (hasGif) {
                entries.filter { it.isImage }
            } else {
                entries.filter { it.isImage && it != GIF }
            }.toSet()
        }

        fun ofVideo(): Set<MimeType> {
            return entries.filter { it.isVideo }.toSet()
        }

    }

    internal val isImage: Boolean
        get() = type.startsWith(prefix = "image")

    internal val isVideo: Boolean
        get() = type.startsWith(prefix = "video")

}