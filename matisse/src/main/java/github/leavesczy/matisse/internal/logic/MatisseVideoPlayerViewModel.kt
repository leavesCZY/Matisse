package github.leavesczy.matisse.internal.logic

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal abstract class MatisseVideoPlayerViewModel(application: Application) :
    BaseMatisseViewModel(application = application) {

    var videoPlayerPageViewState by mutableStateOf(
        value = MatisseVideoPlayerPageViewState(
            isVisible = false,
            videoUri = Uri.EMPTY,
            onDismissRequest = {}
        )
    )
        private set

    protected fun showVideoPlayerPage(videoUri: Uri) {
        videoPlayerPageViewState = MatisseVideoPlayerPageViewState(
            isVisible = true,
            videoUri = videoUri,
            onDismissRequest = ::dismissVideoPlayerPage
        )
    }

    protected fun dismissVideoPlayerPage() {
        videoPlayerPageViewState = videoPlayerPageViewState.copy(
            isVisible = false,
            onDismissRequest = {}
        )
    }

}