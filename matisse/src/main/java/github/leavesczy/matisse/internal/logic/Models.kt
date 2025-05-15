package github.leavesczy.matisse.internal.logic

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource

/**
 * @Author: leavesCZY
 * @Date: 2022/5/30 23:24
 * @Desc:
 */
@Stable
internal data class MatissePageViewState(
    val maxSelectable: Int,
    val fastSelect: Boolean,
    val gridColumns: Int,
    val imageEngine: ImageEngine,
    val captureStrategy: CaptureStrategy?,
    val mediaBucketsInfo: List<MatisseMediaBucketInfo>,
    val selectedBucket: MatisseMediaBucket,
    val lazyGridState: LazyGridState,
    val onClickBucket: suspend (String) -> Unit,
    val onClickMedia: (MatisseMediaExtend) -> Unit,
    val onMediaCheckChanged: (MatisseMediaExtend) -> Unit
)

@Stable
internal data class MatisseMediaExtend(
    val mediaId: Long,
    val bucketId: String,
    val bucketName: String,
    val media: MediaResource,
    val selectState: State<MatisseMediaSelectState>
)

@Stable
internal data class MatisseMediaSelectState(
    val isSelected: Boolean,
    val isEnabled: Boolean,
    val positionIndex: Int
) {

    val positionFormatted = run {
        if (positionIndex >= 0) {
            (positionIndex + 1).toString()
        } else {
            null
        }
    }

}

@Stable
internal data class MatisseMediaBucket(
    val bucketId: String,
    val bucketName: String,
    val supportCapture: Boolean,
    val resources: List<MatisseMediaExtend>
)

@Stable
internal data class MatisseMediaBucketInfo(
    val bucketId: String,
    val bucketName: String,
    val size: Int,
    val firstMedia: MediaResource?
)

@Stable
internal data class MatisseBottomBarViewState(
    val previewButtonText: String,
    val previewButtonClickable: Boolean,
    val onClickPreviewButton: () -> Unit,
    val sureButtonText: String,
    val sureButtonClickable: Boolean
)

@Stable
internal data class MatissePreviewPageViewState(
    val visible: Boolean,
    val initialPage: Int,
    val maxSelectable: Int,
    val sureButtonText: String,
    val sureButtonClickable: Boolean,
    val previewResources: List<MatisseMediaExtend>,
    val onMediaCheckChanged: (MatisseMediaExtend) -> Unit,
    val onDismissRequest: () -> Unit
)