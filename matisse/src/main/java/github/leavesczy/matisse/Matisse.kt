package github.leavesczy.matisse

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize

/**
 * 图片和视频选择器的启动配置。
 *
 * Matisse 不会在库 Manifest 中声明媒体读取权限。宿主应用需要根据 [mediaType] 声明相应权限。
 * 当设备为 Android 13 及以上且宿主 `targetSdkVersion >= 33` 时，Matisse 按
 * [MediaType.includesImage] / [MediaType.includesVideo] 分别请求 `READ_MEDIA_IMAGES`
 * 和/或 `READ_MEDIA_VIDEO`；其他情况请求 `READ_EXTERNAL_STORAGE`。Android 14 及以上且宿主
 * `targetSdkVersion >= 34` 时，如果宿主还声明了 `READ_MEDIA_VISUAL_USER_SELECTED`，Matisse
 * 会同时请求并接受用户授予的部分媒体访问权限。再次启动选择器时，若当前不是完整访问（包括仅有
 * 部分授权），会重新打开系统授权界面，以便调整可访问的媒体范围；已是完整访问时不会重复弹窗。
 * 未启用部分访问，或未获得部分访问权限时，已请求的图片和/或视频权限必须全部授予后才能进入选择界面。
 *
 * @param maxSelectable 最多可选择的媒体数量，必须大于 0
 * @param imageEngine 图片加载引擎。Matisse 不传递 Coil 或 Glide 依赖，宿主需要根据所选实现添加依赖，具体要求参见 [CoilImageEngine] 与 [GlideImageEngine]
 * @param gridColumns 媒体网格的列数，必须大于 0，默认为 3
 * @param returnOnTap 是否在点击缩略图时立即返回。启用后点击缩略图会立即返回单个 [MediaResource]，不进入预览或多选确认流程，选择器内拍照成功后同样立即返回该照片，并且 [maxSelectable] 必须为 1，默认为 false
 * @param mediaType 需要展示的媒体类型，默认为 [MediaType.ImageOnly]
 * @param allowMixedMedia 是否允许在同一结果中同时选择图片和视频。为 false 时禁止混合选择，默认为 false
 * @param captureStrategy 拍照策略。[mediaType] 必须包含图片（[MediaType.includesImage] 为 true），否则只能为 null。
 * 当 [mediaType] 为 [MediaType.MimeTypes] 时还须包含 `image/jpeg`（内置拍照输出类型）。
 * 自定义策略返回的 MIME 类型与 [mediaType] 不匹配时，拍照结果不会加入列表。默认为 null
 *
 * @throws IllegalArgumentException
 * 当 [maxSelectable] 或 [gridColumns] 小于 1，
 * 或者 [maxSelectable] 大于 1 且 [returnOnTap] 为 true，
 * 或者 [mediaType] 不包含图片且 [captureStrategy] 非空，
 * 或者 [mediaType] 为 [MediaType.MimeTypes] 且未包含 `image/jpeg` 同时 [captureStrategy] 非空时抛出
 */
@Stable
@Parcelize
data class Matisse(
    val maxSelectable: Int,
    val imageEngine: ImageEngine,
    val gridColumns: Int = 3,
    val returnOnTap: Boolean = false,
    val mediaType: MediaType = MediaType.ImageOnly,
    val allowMixedMedia: Boolean = false,
    val captureStrategy: CaptureStrategy? = null
) : Parcelable {

    init {
        if (maxSelectable < 1) {
            throw IllegalArgumentException("maxSelectable should be larger than zero")
        }
        if (maxSelectable > 1 && returnOnTap) {
            throw IllegalArgumentException("when maxSelectable is greater than 1, returnOnTap must be false")
        }
        if (gridColumns < 1) {
            throw IllegalArgumentException("gridColumns should be larger than zero")
        }
        if (captureStrategy != null) {
            if (!mediaType.includesImage) {
                throw IllegalArgumentException("captureStrategy must be null when mediaType does not include image")
            }
            if (mediaType is MediaType.MimeTypes &&
                !mediaType.mimeTypes.contains(element = CAPTURE_OUTPUT_MIME_TYPE)
            ) {
                throw IllegalArgumentException(
                    "when captureStrategy is non-null, MimeTypes must include $CAPTURE_OUTPUT_MIME_TYPE"
                )
            }
        }
    }

}

/**
 * 独立拍照功能的启动配置。通过 [MatisseCaptureContract] 启动后进入拍照流程。
 *
 * @param captureStrategy 用于创建输出 Uri、读取拍照结果及清理无效结果的拍照策略，
 * 可参见 [FileProviderCaptureStrategy]、[MediaStoreCaptureStrategy] 与 [SmartCaptureStrategy]
 */
@Parcelize
data class MatisseCapture(
    val captureStrategy: CaptureStrategy
) : Parcelable

private const val IMAGE_MIME_TYPE_PREFIX = "image/"

private const val VIDEO_MIME_TYPE_PREFIX = "video/"

/** 内置拍照策略写入的 MIME 类型；启用拍照时 [MediaType.MimeTypes] 必须包含此项。 */
internal const val CAPTURE_OUTPUT_MIME_TYPE = "image/jpeg"

@Parcelize
sealed interface MediaType : Parcelable {

    @Parcelize
    data object ImageOnly : MediaType

    @Parcelize
    data object VideoOnly : MediaType

    @Parcelize
    data object ImageAndVideo : MediaType

    /**
     * 按指定 MIME 类型精确查询媒体，例如 `image/png`、`image/gif` 或 `video/mp4`。
     * 选择器仅支持以 `image/` 或 `video/` 开头的类型；其他类型无法匹配对应媒体权限与预览行为。
     *
     * @param mimeTypes 需要与 MediaStore MIME 值精确匹配的完整 MIME 类型集合，不允许为空，
     * 且每项必须以 `image/` 或 `video/` 开头
     * @throws IllegalArgumentException 当 [mimeTypes] 为空或包含不支持的类型时抛出
     */
    @Parcelize
    data class MimeTypes(val mimeTypes: Set<String>) : MediaType {

        init {
            if (mimeTypes.isEmpty()) {
                throw IllegalArgumentException("mimeTypes cannot be empty")
            }
            val hasUnsupportedMimeType = mimeTypes.any { mimeType ->
                !mimeType.startsWith(prefix = IMAGE_MIME_TYPE_PREFIX) &&
                        !mimeType.startsWith(prefix = VIDEO_MIME_TYPE_PREFIX)
            }
            if (hasUnsupportedMimeType) {
                throw IllegalArgumentException("mimeTypes only support image and video")
            }
        }

    }

    val includesImage: Boolean
        get() = when (this) {
            ImageOnly, ImageAndVideo -> {
                true
            }

            VideoOnly -> {
                false
            }

            is MimeTypes -> {
                mimeTypes.any {
                    it.startsWith(prefix = IMAGE_MIME_TYPE_PREFIX)
                }
            }
        }

    val includesVideo: Boolean
        get() = when (this) {
            ImageOnly -> {
                false
            }

            VideoOnly, ImageAndVideo -> {
                true
            }

            is MimeTypes -> {
                mimeTypes.any {
                    it.startsWith(prefix = VIDEO_MIME_TYPE_PREFIX)
                }
            }
        }

}

/**
 * @param uri 媒体 Uri。内置选择和拍照策略返回 `content://` Uri；实际访问范围取决于 Uri 来源及宿主权限
 * @param mimeType 媒体的 MIME 类型，例如 `image/jpeg` 或 `video/mp4`。MediaStore 未提供类型或
 * 自定义调用方传入非标准值时可能为空或无法识别，此时 [isImage] 与 [isVideo] 均为 false
 */
@Stable
@Parcelize
data class MediaResource(
    val uri: Uri,
    val mimeType: String
) : Parcelable {

    val isImage: Boolean
        get() = mimeType.startsWith(prefix = IMAGE_MIME_TYPE_PREFIX)

    val isVideo: Boolean
        get() = mimeType.startsWith(prefix = VIDEO_MIME_TYPE_PREFIX)

}