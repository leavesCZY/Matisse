package github.leavesczy.matisse.internal.logic

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.paging.PagingData
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import kotlinx.coroutines.flow.Flow

internal fun interface MatisseBucketClickHandler {
    suspend operator fun invoke(bucketId: String)
}

internal fun interface MatisseMediaClickHandler {
    operator fun invoke(mediaItem: MatisseMediaItem, previewMediaItems: List<MatisseMediaItem>)
}

internal fun interface MatisseMediaCheckChangedHandler {
    operator fun invoke(mediaItem: MatisseMediaItem)
}

internal fun interface MatisseOpenVideoClickHandler {
    operator fun invoke(mediaResource: MediaResource)
}

internal fun interface MatisseMediaItemFactory {
    operator fun invoke(mediaInfo: MediaProvider.MediaInfo): MatisseMediaItem
}

internal fun interface MatisseBucketInfoClickHandler {
    operator fun invoke(bucket: MatisseMediaBucketInfo)
}

internal fun interface MatisseMediaResourceClickHandler {
    operator fun invoke(mediaResource: MediaResource)
}

@Stable
internal data class MatissePageViewState(
    val matisse: Matisse,
    val selectedBucket: MatisseMediaBucket,
    val mediaBuckets: List<MatisseMediaBucketInfo>,
    val isMediaBucketsLoading: Boolean,
    val mediaPagingDataFlow: Flow<PagingData<MatisseMediaItem>>,
    val placeholderState: MatissePlaceholderState,
    val onBucketMenuOpen: () -> Unit,
    val onBucketClick: MatisseBucketClickHandler,
    val onMediaClick: MatisseMediaClickHandler,
    val onMediaCheckChanged: MatisseMediaCheckChangedHandler
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
    val onMediaCheckChanged: MatisseMediaCheckChangedHandler,
    val onOpenVideoClick: MatisseOpenVideoClickHandler,
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

    @Stable
    data class NoMedia(
        val includesImage: Boolean,
        val includesVideo: Boolean
    ) : MatissePlaceholderState()

}
