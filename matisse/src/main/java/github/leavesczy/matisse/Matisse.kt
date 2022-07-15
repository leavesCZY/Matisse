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
 * @param theme 主题。默认是日间主题
 * @param supportedMimeTypes 需要显示的图片类型。默认是包含 Gif 在内的所有图片
 * @param maxSelectable 可以选择的最大图片数量。默认是 1
 * @param spanCount 显示图片列表时的列表。默认是 4
 * @param tips 权限被拒绝、图片数量超限时的 Toast 提示
 * @param captureStrategy 拍照策略。默认不开启拍照功能
 */
data class Matisse(
    val theme: MatisseTheme = LightMatisseTheme,
    val supportedMimeTypes: List<MimeType> = ofImage(hasGif = true),
    val maxSelectable: Int = 1,
    val spanCount: Int = 4,
    val tips: MatisseTips = defaultMatisseTips,
    val captureStrategy: CaptureStrategy = NothingCaptureStrategy
) {

    companion object {

        fun ofImage(hasGif: Boolean = true): List<MimeType> {
            return if (hasGif) {
                listOf(*MimeType.values())
            } else {
                mutableListOf(*MimeType.values()).apply {
                    remove(MimeType.GIF)
                }
            }
        }

    }

}

@Parcelize
data class MediaResources(
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
    internal val key = bucketId + uri.toString()

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

data class MatisseTips(
    val onReadExternalStorageDenied: String,
    val onWriteExternalStorageDenied: String,
    val onCameraDenied: String,
    val onSelectLimit: (selectedSize: Int, maxSelectable: Int) -> String,
)

private val defaultMatisseTips = MatisseTips(onReadExternalStorageDenied = "请授予存储访问权限后重试",
    onWriteExternalStorageDenied = "请授予存储写入权限后重试",
    onCameraDenied = "请授予拍照权限后重试",
    onSelectLimit = { _: Int, maxSelectable: Int ->
        "最多只能选择${maxSelectable}张图片"
    }
)