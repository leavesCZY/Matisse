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
 * @param maxSelectable 最多能选择几个媒体资源
 * @param imageEngine 图片加载框架
 * @param gridColumns 一行要显示几个媒体资源。默认值为 4
 * @param fastSelect 是否要点击媒体资源后立即返回，值为 true 时 maxSelectable 必须为 1。默认不立即返回
 * @param mediaType 要加载的媒体资源类型。默认仅图片
 * @param singleMediaType 是否允许同时选择图片和视频。默认允许
 * @param mediaFilter 媒体资源的筛选规则。默认不进行筛选
 * @param captureStrategy 拍照策略。默认不开启拍照功能
 */
@Stable
@Parcelize
data class Matisse(
    val maxSelectable: Int,
    val imageEngine: ImageEngine,
    val gridColumns: Int = 4,
    val fastSelect: Boolean = false,
    val mediaType: MediaType = MediaType.ImageOnly,
    val singleMediaType: Boolean = false,
    val mediaFilter: MediaFilter? = null,
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
 * @param captureStrategy 拍照策略
 */
@Parcelize
data class MatisseCapture(
    val captureStrategy: CaptureStrategy
) : Parcelable

@Parcelize
sealed interface MediaType : Parcelable {

    @Parcelize
    data object ImageOnly : MediaType

    @Parcelize
    data object VideoOnly : MediaType

    @Parcelize
    data object ImageAndVideo : MediaType

    @Parcelize
    data class MultipleMimeType(val mimeTypes: Set<String>) : MediaType {

        init {
            if (mimeTypes.isEmpty()) {
                throw IllegalArgumentException("mimeTypes cannot be empty")
            }
        }

    }

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

@Stable
@Parcelize
data class MediaResource(
    val uri: Uri,
    val path: String,
    val name: String,
    val mimeType: String
) : Parcelable {

    val isImage: Boolean
        get() = mimeType.startsWith(prefix = ImageMimeTypePrefix)

    val isVideo: Boolean
        get() = mimeType.startsWith(prefix = VideoMimeTypePrefix)

}