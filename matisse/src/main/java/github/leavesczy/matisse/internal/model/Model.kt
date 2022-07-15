package github.leavesczy.matisse.internal.model

import android.net.Uri
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResources

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
    val resources: List<MediaResources>,
    val displayResources: List<MediaResources>,
)

internal data class MatisseViewState(
    val matisse: Matisse,
    val state: MatisseState,
    val allBucket: List<MediaBucket>,
    val selectedBucket: MediaBucket,
    val selectedMediaResources: List<MediaResources>,
    val previewText: String,
    val previewBtnClickable: Boolean,
    val sureText: String,
    val sureBtnClickable: Boolean,
)

internal data class MatissePageAction(
    val onClickBackMenu: () -> Unit,
    val onCapture: () -> Unit,
    val isCaptureMediaResources: (MediaResources) -> Boolean,
    val onSelectBucket: (MediaBucket) -> Unit,
    val onMediaCheckChanged: (MediaResources) -> Unit,
    val onClickMedia: (MediaResources) -> Unit,
    val onPreviewSelectedResources: () -> Unit,
    val onSure: () -> Unit,
)

internal data class MatissePreviewViewState(
    val matisse: Matisse,
    val initialPage: Int,
    val previewResource: List<MediaResources>,
    val selectedMediaResources: List<MediaResources>,
    val onMediaCheckChanged: (MediaResources) -> Unit,
)