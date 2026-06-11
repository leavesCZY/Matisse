package github.leavesczy.matisse.internal.logic

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource

internal abstract class MatissePreviewImageViewModel(application: Application, matisse: Matisse) :
    MatisseVideoPlayerViewModel(application = application) {

    private val maxSelectable = matisse.maxSelectable

    var previewImagePageViewState by mutableStateOf(
        value = MatissePreviewImagePageViewState(
            visible = false,
            initialPage = 0,
            selectedMediaCount = 0,
            maxSelectable = maxSelectable,
            previewMediaItems = emptyList(),
            onMediaCheckChanged = {},
            onOpenVideoClick = {},
            onDismissRequest = {}
        )
    )
        private set

    protected fun showPreviewImagePage(
        initialPage: Int,
        previewMediaItems: List<MatisseMediaItem>,
        selectedMediaItems: List<MatisseMediaItem>
    ) {
        previewImagePageViewState = MatissePreviewImagePageViewState(
            visible = true,
            maxSelectable = maxSelectable,
            initialPage = initialPage,
            selectedMediaCount = selectedMediaItems.size,
            previewMediaItems = previewMediaItems,
            onMediaCheckChanged = ::onPreviewImagePageMediaCheckChanged,
            onOpenVideoClick = ::openVideoPlayerPage,
            onDismissRequest = ::dismissPreviewImagePage
        )
    }

    protected fun dismissPreviewImagePage() {
        val currentPreviewPageViewState = previewImagePageViewState
        if (currentPreviewPageViewState.visible) {
            previewImagePageViewState = currentPreviewPageViewState.copy(
                visible = false,
                onMediaCheckChanged = {},
                onOpenVideoClick = {},
                onDismissRequest = {}
            )
        }
    }

    private fun openVideoPlayerPage(video: MediaResource) {
        showVideoPlayerPage(videoUri = video.uri)
    }

    protected fun updatePreviewImagePageIfNeeded() {
        val currentPreviewPageViewState = previewImagePageViewState
        if (currentPreviewPageViewState.visible) {
            val selectedMediaItems = getSelectedMediaItems()
            previewImagePageViewState = currentPreviewPageViewState.copy(
                selectedMediaCount = selectedMediaItems.size
            )
        }
    }

}