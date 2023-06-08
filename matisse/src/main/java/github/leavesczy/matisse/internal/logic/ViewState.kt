package github.leavesczy.matisse.internal.logic

import android.net.Uri
import androidx.compose.foundation.lazy.grid.LazyGridState
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource

/**
 * @Author: leavesCZY
 * @Date: 2022/5/30 23:24
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
internal data class MatissePageViewState(
    val lazyGridState: LazyGridState,
    val selectedBucket: MediaBucket,
    val onClickMedia: (MediaResource) -> Unit,
    val onMediaCheckChanged: (MediaResource) -> Unit
)

internal data class MediaBucket(
    val id: String,
    val displayName: String,
    val displayIcon: Uri,
    val resources: List<MediaResource>,
    val supportCapture: Boolean
)

internal data class MatisseTopBarViewState(
    val matisse: Matisse,
    val title: String,
    val mediaBuckets: List<MediaBucket>,
    val onSelectBucket: (MediaBucket) -> Unit
)

internal data class MatisseBottomBarViewState(
    val previewButtonText: String,
    val previewButtonClickable: Boolean,
    val onClickPreviewButton: () -> Unit,
    val sureButtonText: String,
    val sureButtonClickable: Boolean
)

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