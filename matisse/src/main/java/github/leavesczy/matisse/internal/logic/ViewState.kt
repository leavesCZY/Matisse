package github.leavesczy.matisse.internal.logic

import android.net.Uri
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
    val supportCapture: Boolean
)

internal data class MatisseViewState(
    val matisse: Matisse,
    val state: MatisseState,
    val allBucket: List<MediaBucket>,
    val selectedBucket: MediaBucket,
    val selectedResources: List<MediaResource>
)

internal data class MatisseBottomBarViewState(
    val previewText: String,
    val sureText: String,
    val previewButtonClickable: Boolean,
    val sureButtonClickable: Boolean
)

internal data class MatissePageAction(
    val onClickBackMenu: () -> Unit,
    val onRequestCapture: () -> Unit,
    val onSureButtonClick: () -> Unit
)

internal data class MatissePreviewViewState(
    val matisse: Matisse,
    val visible: Boolean,
    val initialPage: Int,
    val previewResources: List<MediaResource>,
    val selectedResources: List<MediaResource>
)