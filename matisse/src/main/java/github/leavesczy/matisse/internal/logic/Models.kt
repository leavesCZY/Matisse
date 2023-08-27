package github.leavesczy.matisse.internal.logic

import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import github.leavesczy.matisse.MediaResource

/**
 * @Author: leavesCZY
 * @Date: 2022/5/30 23:24
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
@Stable
internal data class MatissePageViewState(
    val lazyGridState: LazyGridState,
    val selectedBucket: MediaBucket,
    val onClickMedia: (MediaResource) -> Unit,
    val onMediaCheckChanged: (MediaResource) -> Unit
)

@Stable
internal data class MediaBucket(
    val id: String,
    val name: String,
    val resources: List<MediaResource>,
    val supportCapture: Boolean
)

@Stable
internal data class MatisseTopBarViewState(
    val title: String,
    val mediaBuckets: List<MediaBucket>,
    val onClickBucket: (MediaBucket) -> Unit
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