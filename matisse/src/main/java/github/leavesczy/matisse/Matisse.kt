package github.leavesczy.matisse

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: CZY
 * @Date: 2022/6/1 17:45
 * @Desc:
 */
/**
 * @param maxSelectable 最多允许选择几个媒体资源
 * @param mimeTypes 要展示的媒体类型
 * @param captureStrategy 拍照策略。默认不开启拍照功能
 */
@Parcelize
data class Matisse(
    val maxSelectable: Int,
    val mimeTypes: List<MimeType>,
    val imageEngine: ImageEngine,
    val captureStrategy: CaptureStrategy = NothingCaptureStrategy
) : Parcelable {

    init {
        assert(value = maxSelectable >= 1)
        assert(value = mimeTypes.isNotEmpty())
    }

}

@Parcelize
data class MatisseCapture(val captureStrategy: CaptureStrategy) : Parcelable

@Parcelize
data class MediaResource(
    internal val id: Long,
    internal val bucketId: String,
    internal val bucketDisplayName: String,
    val uri: Uri,
    val path: String,
    val displayName: String,
    val mimeType: String,
) : Parcelable

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

    internal val isImage = type.startsWith("image/")

    internal val isVideo = type.startsWith("video/")

    companion object {

        fun ofAll(): List<MimeType> {
            return MimeType.values().toList()
        }

        fun ofImage(hasGif: Boolean): List<MimeType> {
            return if (hasGif) {
                listOf(JPEG, PNG, WEBP, HEIC, HEIF, BMP, GIF)
            } else {
                listOf(JPEG, PNG, WEBP, HEIC, HEIF, BMP)
            }
        }

        fun ofVideo(): List<MimeType> {
            return listOf(MPEG, MP4, QUICKTIME, THREEGPP, THREEGPP2, MKV, WEBM, TS, AVI)
        }

    }

}