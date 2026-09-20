package github.leavesczy.matisse.internal.logic

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.paging.PagingData
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import kotlinx.coroutines.flow.Flow

@Stable
internal data class MatissePageViewState(
    val matisse: Matisse,
    val selectedBucket: MatisseMediaBucket,
    val mediaBuckets: List<MatisseMediaBucketInfo>,
    val isMediaBucketsLoading: Boolean,
    val capturedMediaItems: List<MatisseMediaItem>,
    val mediaPagingDataFlow: Flow<PagingData<MatisseMediaItem>>,
    val placeholderState: MatissePlaceholderState,
    val selectionStateOf: (mediaId: Long) -> MatisseMediaSelectState,
    val onBucketMenuOpen: () -> Unit,
    val onBucketClick: (bucketId: String) -> Unit,
    val onMediaClick: (mediaItem: MatisseMediaItem, previewMediaItems: List<MatisseMediaItem>) -> Unit,
    val onMediaCheckChanged: (mediaItem: MatisseMediaItem) -> Unit
)

@Stable
internal data class MatisseMediaItem(
    val mediaId: Long,
    val bucketId: String,
    val bucketName: String,
    val mediaResource: MediaResource
)

@Stable
internal data class MatisseMediaSelectState(
    val isSelected: Boolean,
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
    val supportsCapture: Boolean
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
internal data class MatissePreviewPageViewState(
    val isVisible: Boolean,
    val maxSelectable: Int,
    val initialPage: Int,
    val selectedMediaCount: Int,
    val previewMediaItems: List<MatisseMediaItem>,
    val selectionStateOf: (mediaId: Long) -> MatisseMediaSelectState,
    val onMediaCheckChanged: (mediaItem: MatisseMediaItem) -> Unit,
    val onOpenVideoClick: (mediaResource: MediaResource) -> Unit,
    val onDismissRequest: () -> Unit
)

@Stable
internal data class MatisseVideoPlayerPageViewState(
    val isVisible: Boolean,
    val videoUri: Uri,
    val onDismissRequest: () -> Unit
)

@Stable
internal sealed class MatissePlaceholderState {

    @Stable
    data class Ready(val hasReadMediaPermission: Boolean) : MatissePlaceholderState()

    @Stable
    data object NoPermission : MatissePlaceholderState()

}
