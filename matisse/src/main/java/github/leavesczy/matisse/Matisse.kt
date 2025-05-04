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
 * @param maxSelectable 用于设置最多能选择几个媒体资源
 * @param imageEngine 用于自定义图片加载框架
 * @param fastSelect 用于设置是否免去预览图片和确认选择的流程。值为 true 时 maxSelectable 必须为 1
 * @param mediaType 用于设置要加载的媒体资源类型。默认仅图片
 * @param singleMediaType 用于设置是否允许同时选择图片和视频。默认允许
 * @param mediaFilter 用于设置媒体资源的筛选规则。默认不进行筛选
 * @param captureStrategy 拍照策略。默认不开启拍照功能
 */
@Stable
@Parcelize
data class Matisse(
    val maxSelectable: Int,
    val imageEngine: ImageEngine,
    val fastSelect: Boolean = false,
    val mediaType: MediaType = MediaType.ImageOnly,
    val singleMediaType: Boolean = false,
    val mediaFilter: MediaFilter? = null,
    val captureStrategy: CaptureStrategy? = null
) : Parcelable {

    init {
        if (maxSelectable < 1) {
            throw IllegalArgumentException("maxSelectable must be greater than or equal to 1")
        }
        if (maxSelectable > 1 && fastSelect) {
            throw IllegalArgumentException("when maxSelectable is greater than 1, fastSelect must be false")
        }
    }

}

/**
 * @param captureStrategy 拍照策略
 */
@Parcelize
data class MatisseCapture(val captureStrategy: CaptureStrategy) : Parcelable

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

}

internal const val ImageMimeTypePrefix = "image/"

internal const val VideoMimeTypePrefix = "video/"

@Stable
@Parcelize
data class MediaResource(
    internal val id: Long,
    internal val bucketId: String,
    internal val bucketName: String,
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