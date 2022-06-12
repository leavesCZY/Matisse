package github.leavesczy.matisse.internal.model

import android.net.Uri
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
    val state: MatisseState,
    val allBucket: List<MediaBucket>,
    val selectedBucket: MediaBucket,
    val selectedMediaResources: List<MediaResources>,
    val previewText: String,
    val previewBtnClickable: Boolean,
    val sureText: String,
    val sureBtnClickable: Boolean,
)
