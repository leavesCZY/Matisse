package github.leavesczy.matisse

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize

/**
 * 图片和视频选择器的启动配置。
 *
 * Matisse 不会在库 Manifest 中声明媒体读取权限。宿主应用需要根据 [mediaType] 声明相应权限。
 * 当设备为 Android 13 及以上且宿主 `targetSdkVersion >= 33` 时，Matisse 请求
 * [mediaType] 实际包含的 `READ_MEDIA_IMAGES` 和/或 `READ_MEDIA_VIDEO`；其他情况请求
 * `READ_EXTERNAL_STORAGE`。Android 14 及以上且宿主 `targetSdkVersion >= 34` 时，如果宿主还声明了
 * `READ_MEDIA_VISUAL_USER_SELECTED`，Matisse 会同时请求并接受用户授予的部分媒体访问权限。
 * 再次启动选择器时，部分授权会重新打开系统授权界面，以便调整可访问的媒体范围。
 * 未获得部分访问权限时，同时请求的图片和视频权限必须全部授予后才能进入选择界面。
 *
 * @param maxSelectable 最多可选择的媒体数量，必须大于 0
 * @param imageEngine 图片加载引擎。Matisse 不传递 Coil 或 Glide 依赖，宿主需要根据所选实现添加依赖，
 * 具体要求参见 [CoilImageEngine] 与 [GlideImageEngine]
 * @param gridColumns 媒体网格的列数，必须大于 0，默认为 4
 * @param fastSelect 是否启用快速选择。启用后点击缩略图会立即返回单个 [MediaResource]，
 * 不进入预览或多选确认流程，并且 [maxSelectable] 必须为 1，默认为 false
 * @param mediaType 需要展示的媒体类型，默认为 [MediaType.ImageOnly]
 * @param singleMediaType 是否禁止同时选择图片和视频。为 false 时允许在同一结果中混合图片和视频，
 * 默认为 true
 * @param captureStrategy 拍照策略。传入非空值且媒体读取权限已授予时，在“全部”相册中显示拍照入口。
 * 拍照成功后立即结束选择器：若当前存在未达到 [maxSelectable] 且符合 [singleMediaType] 限制的已选项，
 * 返回“已选项 + 新照片”，否则仅返回新照片。拍照入口不受 [mediaType] 限制，因此即使选择
 * [MediaType.VideoOnly] 也会显示入口并返回图片。默认为 null
 *
 * @throws IllegalArgumentException 当 [maxSelectable] 或 [gridColumns] 小于 1，或者
 * [maxSelectable] 大于 1 且 [fastSelect] 为 true 时抛出
 */
@Stable
@Parcelize
data class Matisse(
    val maxSelectable: Int,
    val imageEngine: ImageEngine,
    val gridColumns: Int = 4,
    val fastSelect: Boolean = false,
    val mediaType: MediaType = MediaType.ImageOnly,
    val singleMediaType: Boolean = true,
    val captureStrategy: CaptureStrategy? = null
) : Parcelable {

    init {
        if (maxSelectable < 1) {
            throw IllegalArgumentException("maxSelectable should be larger than zero")
        }
        if (maxSelectable > 1 && fastSelect) {
            throw IllegalArgumentException("when maxSelectable is greater than 1, fastSelect must be false")
        }
        if (gridColumns < 1) {
            throw IllegalArgumentException("gridColumns should be larger than zero")
        }
    }

}

/**
 * 独立拍照功能的启动配置。通过 [MatisseCaptureContract] 启动后会立即打开系统相机，
 * 不显示媒体选择界面。
 *
 * 如果宿主在 Manifest 中声明了 `CAMERA`，Matisse 会按需申请相机权限；存储权限、输出位置和
 * FileProvider 配置要求由具体 [CaptureStrategy] 决定。
 *
 * @param captureStrategy 用于创建输出 Uri、读取拍照结果及清理无效结果的拍照策略，
 * 可参见 [FileProviderCaptureStrategy]、[MediaStoreCaptureStrategy] 与 [SmartCaptureStrategy]
 */
@Parcelize
data class MatisseCapture(
    val captureStrategy: CaptureStrategy
) : Parcelable

/**
 * 选择器需要查询和展示的媒体类型。
 */
@Parcelize
sealed interface MediaType : Parcelable {

    /** 仅查询图片。 */
    @Parcelize
    data object ImageOnly : MediaType

    /** 仅查询视频。 */
    @Parcelize
    data object VideoOnly : MediaType

    /** 同时查询图片和视频。 */
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
    data class MultipleMimeType(val mimeTypes: Set<String>) : MediaType {

        init {
            if (mimeTypes.isEmpty()) {
                throw IllegalArgumentException("mimeTypes cannot be empty")
            }
            val hasUnsupportedMimeType = mimeTypes.any { mimeType ->
                !mimeType.startsWith(prefix = ImageMimeTypePrefix) &&
                        !mimeType.startsWith(prefix = VideoMimeTypePrefix)
            }
            if (hasUnsupportedMimeType) {
                throw IllegalArgumentException("mimeTypes only support image and video")
            }
        }

    }

    /** 当前类型是否可能包含图片；[MultipleMimeType] 中存在以 `image/` 开头的值时为 true。 */
    val includeImage: Boolean
        get() = when (this) {
            ImageOnly, ImageAndVideo -> {
                true
            }

            VideoOnly -> {
                false
            }

            is MultipleMimeType -> {
                mimeTypes.any {
                    it.startsWith(prefix = ImageMimeTypePrefix)
                }
            }
        }

    /** 当前类型是否可能包含视频；[MultipleMimeType] 中存在以 `video/` 开头的值时为 true。 */
    val includeVideo: Boolean
        get() = when (this) {
            ImageOnly -> {
                false
            }

            VideoOnly, ImageAndVideo -> {
                true
            }

            is MultipleMimeType -> {
                mimeTypes.any {
                    it.startsWith(prefix = VideoMimeTypePrefix)
                }
            }
        }

}

internal const val ImageMimeTypePrefix = "image/"

internal const val VideoMimeTypePrefix = "video/"

/**
 * 选择器返回的媒体资源。
 *
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

    /** [mimeType] 是否以 `image/` 开头。 */
    val isImage: Boolean
        get() = mimeType.startsWith(prefix = ImageMimeTypePrefix)

    /** [mimeType] 是否以 `video/` 开头。 */
    val isVideo: Boolean
        get() = mimeType.startsWith(prefix = VideoMimeTypePrefix)

}