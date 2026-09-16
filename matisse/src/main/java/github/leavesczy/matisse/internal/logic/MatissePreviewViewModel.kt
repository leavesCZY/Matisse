package github.leavesczy.matisse.internal.logic

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource

internal abstract class MatissePreviewViewModel(application: Application, matisse: Matisse) :
    MatisseVideoPlayerViewModel(application = application) {

    private val maxSelectable = matisse.maxSelectable

    var previewPageViewState by mutableStateOf(
        value = MatissePreviewPageViewState(
            isVisible = false,
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

    protected fun showPreviewPage(
        initialPage: Int,
        previewMediaItems: List<MatisseMediaItem>,
        selectedMediaItems: List<MatisseMediaItem>
    ) {
        previewPageViewState = MatissePreviewPageViewState(
            isVisible = true,
            maxSelectable = maxSelectable,
            initialPage = initialPage,
            selectedMediaCount = selectedMediaItems.size,
            previewMediaItems = previewMediaItems,
            onMediaCheckChanged = ::onPreviewPageMediaCheckChanged,
            onOpenVideoClick = ::openVideoPlayerPage,
            onDismissRequest = ::dismissPreviewPage
        )
    }

    protected fun dismissPreviewPage() {
        val currentPreviewPageViewState = previewPageViewState
        if (currentPreviewPageViewState.isVisible) {
            previewPageViewState = currentPreviewPageViewState.copy(
                isVisible = false,
                onMediaCheckChanged = {},
                onOpenVideoClick = {},
                onDismissRequest = {}
            )
        }
    }

    private fun openVideoPlayerPage(video: MediaResource) {
        showVideoPlayerPage(videoUri = video.uri)
    }

    protected fun updatePreviewPageIfNeeded() {
        val currentPreviewPageViewState = previewPageViewState
        if (currentPreviewPageViewState.isVisible) {
            val selectedMediaItems = getSelectedMediaItems()
            previewPageViewState = currentPreviewPageViewState.copy(
                selectedMediaCount = selectedMediaItems.size
            )
        }
    }

}
