package github.leavesczy.matisse.internal.logic

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource

@Stable
internal data class MatissePageViewState(
    val matisse: Matisse,
    val selectedBucket: MatisseMediaBucket,
    val mediaBuckets: List<MatisseMediaBucketInfo>,
    val placeholderState: MatissePlaceholderState,
    val onBucketClick: suspend (String) -> Unit,
    val onMediaClick: (MatisseMediaItem) -> Unit,
    val onMediaCheckChanged: (MatisseMediaItem) -> Unit
)

@Stable
internal data class MatisseMediaItem(
    val mediaId: Long,
    val bucketId: String,
    val bucketName: String,
    val mediaResource: MediaResource,
    val selectionState: State<MatisseMediaSelectState>
)

@Stable
internal data class MatisseMediaSelectState(
    val isSelected: Boolean,
    val isEnabled: Boolean,
    val positionIndex: Int
) {

    val positionFormatted = if (positionIndex >= 0) {
        (positionIndex + 1).toString()
    } else {
        null
    }

}

@Stable
internal data class MatisseMediaBucket(
    val bucketId: String,
    val bucketName: String,
    val supportsCapture: Boolean,
    val mediaItems: List<MatisseMediaItem>
)

@Stable
internal data class MatisseMediaBucketInfo(
    val bucketId: String,
    val bucketName: String,
    val itemCount: Int,
    val coverMedia: MediaResource?
)

@Stable
internal data class MatisseBottomBarViewState(
    val selectedMediaCount: Int,
    val maxSelectable: Int,
    val isPreviewEnabled: Boolean,
    val onPreviewClick: () -> Unit
)

@Stable
internal data class MatissePreviewImagePageViewState(
    val visible: Boolean,
    val maxSelectable: Int,
    val initialPage: Int,
    val selectedMediaCount: Int,
    val previewMediaItems: List<MatisseMediaItem>,
    val onMediaCheckChanged: (MatisseMediaItem) -> Unit,
    val onOpenVideoClick: (MediaResource) -> Unit,
    val onDismissRequest: () -> Unit
)

@Stable
internal data class MatisseVideoPlayerPageViewState(
    val visible: Boolean,
    val videoUri: Uri,
    val onDismissRequest: () -> Unit
)

@Stable
internal sealed class MatissePlaceholderState {

    @Stable
    data class Ready(val hasReadMediaPermission: Boolean) : MatissePlaceholderState()

    @Stable
    data object NoPermission : MatissePlaceholderState()

    @Stable
    data class NoMedia(
        val includesImages: Boolean,
        val includesVideos: Boolean
    ) : MatissePlaceholderState()

}