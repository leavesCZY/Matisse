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
            selectedImageSize = 0,
            maxSelectable = maxSelectable,
            previewResources = emptyList(),
            onMediaCheckChanged = {},
            requestOpenVideo = {},
            onDismissRequest = {}
        )
    )
        private set

    protected fun showPreviewImagePage(
        initialPage: Int,
        totalResources: List<MatisseMediaExtend>,
        selectedResources: List<MatisseMediaExtend>
    ) {
        previewImagePageViewState = MatissePreviewImagePageViewState(
            visible = true,
            maxSelectable = maxSelectable,
            initialPage = initialPage,
            selectedImageSize = selectedResources.size,
            previewResources = totalResources,
            onMediaCheckChanged = ::onPreviewImagePageMediaCheckChanged,
            requestOpenVideo = ::requestOpenVideoPlayerPage,
            onDismissRequest = ::dismissPreviewImagePage
        )
    }

    protected fun dismissPreviewImagePage() {
        val viewState = previewImagePageViewState
        if (viewState.visible) {
            previewImagePageViewState = viewState.copy(
                visible = false,
                onMediaCheckChanged = {},
                requestOpenVideo = {},
                onDismissRequest = {}
            )
        }
    }

    private fun requestOpenVideoPlayerPage(mediaResource: MediaResource) {
        showVideoPlayerPage(videoUri = mediaResource.uri)
    }

    protected fun updatePreviewImagePageIfNeed() {
        val viewState = previewImagePageViewState
        if (viewState.visible) {
            val selectedResources = filterSelectedMediaResource()
            previewImagePageViewState = viewState.copy(selectedImageSize = selectedResources.size)
        }
    }

}