package github.leavesczy.matisse.internal.logic

import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
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
    val lazyGridState: LazyGridState,
    val captureStrategy: CaptureStrategy?,
    val selectedBucket: MatisseMediaBucket,
    val imageEngine: ImageEngine,
    val onClickMedia: (MediaResource) -> Unit,
    val onMediaCheckChanged: (MediaResource) -> Unit
)

@Stable
internal data class MatisseMediaBucket(
    val id: String,
    val name: String,
    val supportCapture: Boolean,
    val resources: List<MediaResource>
)

@Stable
internal data class MatisseMediaBucketInfo(
    val id: String,
    val name: String,
    val size: Int,
    val firstMedia: MediaResource?
)

@Stable
internal data class MatisseTopBarViewState(
    val title: String,
    val mediaBuckets: List<MatisseMediaBucketInfo>,
    val onClickBucket: suspend (String) -> Unit
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
    val previewResources: List<MediaResource>,
    val selectedResources: List<MediaResource>,
    val onMediaCheckChanged: (MediaResource) -> Unit,
    val onDismissRequest: () -> Unit
)

internal data class MatisseTakePictureContractParams(
    val uri: Uri,
    val extra: Bundle
)