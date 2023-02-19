package github.leavesczy.matisse

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * @Author: CZY
 * @Date: 2022/6/1 17:45
 * @Desc:
 */
/**
 * @param maxSelectable 可以选择的图片最大数量。默认是 1
 * @param supportedMimeTypes 需要显示的图片类型。默认是包含 Gif 在内的所有图片
 * @param captureStrategy 拍照策略。默认不开启拍照功能
 */
@Parcelize
data class Matisse(
    val maxSelectable: Int = 1,
    val supportedMimeTypes: List<MimeType> = ofImage(hasGif = true),
    val captureStrategy: CaptureStrategy = NothingCaptureStrategy
) : Parcelable {

    init {
        assert(value = maxSelectable >= 1)
        assert(value = supportedMimeTypes.isNotEmpty())
    }

    companion object {

        fun ofImage(hasGif: Boolean = true): List<MimeType> {
            return if (hasGif) {
                listOf(elements = MimeType.values())
            } else {
                mutableListOf(elements = MimeType.values()).apply {
                    remove(element = MimeType.GIF)
                }
            }
        }

    }

}

@Parcelize
data class MediaResource(
    private val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val orientation: Int,
    val size: Long,
    val path: String,
    val bucketId: String,
    val bucketDisplayName: String
) : Parcelable {

    @IgnoredOnParcel
    internal val key = id

}

enum class MimeType(val type: String) {
    JPEG("image/jpeg"),
    PNG("image/png"),
    HEIC("image/heic"),
    HEIF("image/heif"),
    BMP("image/x-ms-bmp"),
    WEBP("image/webp"),
    GIF("image/gif");
}