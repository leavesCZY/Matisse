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
internal enum class MatisseState {
    PermissionRequesting,
    PermissionDenied,
    ImagesLoading,
    ImagesLoaded,
    ImagesEmpty;
}

internal data class MediaBucket(
    val bucketId: String,
    val bucketDisplayName: String,
    val bucketDisplayIcon: Uri,
    val resources: List<MediaResource>,
    val supportCapture: Boolean,
)

internal data class MatisseViewState(
    val matisse: Matisse,
    val bottomBarViewState: MatisseBottomBarViewState,
    val lazyGridState: LazyGridState,
    val state: MatisseState,
    val allBucket: List<MediaBucket>,
    val selectedBucket: MediaBucket,
    val selectedResources: List<MediaResource>,
    val onClickMedia: (MediaResource) -> Unit,
    val onSelectBucket: (MediaBucket) -> Unit,
    val onMediaCheckChanged: (MediaResource) -> Unit,
)

internal data class MatisseBottomBarViewState(
    val previewText: String,
    val sureText: String,
    val previewButtonClickable: Boolean,
    val sureButtonClickable: Boolean,
    val onPreviewButtonClick: () -> Unit,
)

internal data class MatissePageAction(
    val onClickBackMenu: () -> Unit,
    val onRequestCapture: () -> Unit,
    val onSureButtonClick: () -> Unit,
)

internal data class MatissePreviewViewState(
    val matisse: Matisse,
    val visible: Boolean,
    val initialPage: Int,
    val previewResources: List<MediaResource>,
    val selectedResources: List<MediaResource>,
    val onMediaCheckChanged: (MediaResource) -> Unit,
    val onDismissRequest: () -> Unit,
)