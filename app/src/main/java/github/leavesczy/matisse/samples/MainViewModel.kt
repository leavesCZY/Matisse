package github.leavesczy.matisse.samples

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.FileProviderCaptureStrategy
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaStoreCaptureStrategy
import github.leavesczy.matisse.MimeType
import github.leavesczy.matisse.NothingCaptureStrategy
import github.leavesczy.matisse.SmartCaptureStrategy

class MainViewModel : ViewModel() {

    private val fileProviderAuthority = "github.leavesczy.matisse.samples.FileProvider"

    var darkTheme by mutableStateOf(value = false)
        private set

    var mainPageViewState by mutableStateOf(
        value = MainPageViewState(
            maxSelectable = 3,
            mediaType = MediaType.All,
            supportGif = true,
            captureStrategy = MediaCaptureStrategy.Smart,
            mediaList = emptyList(),
            onMaxSelectableChanged = ::onMaxSelectableChanged,
            onMediaTypeChanged = ::onMediaTypeChanged,
            onSupportGifChanged = ::onSupportGifChanged,
            onCaptureStrategyChanged = ::onCaptureStrategyChanged,
            switchTheme = ::switchTheme,
        )
    )
        private set

    init {
        setDefaultNightMode(darkTheme = darkTheme)
    }

    private fun onMaxSelectableChanged(maxSelectable: Int) {
        if (mainPageViewState.maxSelectable != maxSelectable) {
            mainPageViewState = mainPageViewState.copy(maxSelectable = maxSelectable)
        }
    }

    private fun onMediaTypeChanged(mediaType: MediaType) {
        if (mainPageViewState.mediaType != mediaType) {
            mainPageViewState = mainPageViewState.copy(mediaType = mediaType)
        }
    }

    private fun onSupportGifChanged(supportGif: Boolean) {
        if (mainPageViewState.supportGif != supportGif) {
            mainPageViewState = mainPageViewState.copy(supportGif = supportGif)
        }
    }

    private fun onCaptureStrategyChanged(captureStrategy: MediaCaptureStrategy) {
        if (mainPageViewState.captureStrategy != captureStrategy) {
            mainPageViewState = mainPageViewState.copy(captureStrategy = captureStrategy)
        }
    }

    private fun switchTheme() {
        darkTheme = !darkTheme
        setDefaultNightMode(darkTheme = darkTheme)
    }

    private fun setDefaultNightMode(darkTheme: Boolean) {
        if (darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun getMediaCaptureStrategy(): CaptureStrategy {
        return when (mainPageViewState.captureStrategy) {
            MediaCaptureStrategy.Nothing -> {
                NothingCaptureStrategy
            }

            MediaCaptureStrategy.FileProvider -> {
                FileProviderCaptureStrategy(authority = fileProviderAuthority)
            }

            MediaCaptureStrategy.MediaStore -> {
                MediaStoreCaptureStrategy()
            }

            MediaCaptureStrategy.Smart -> {
                SmartCaptureStrategy(authority = fileProviderAuthority)
            }
        }
    }

    fun takePictureResult(result: MediaResource?) {
        if (result != null) {
            mainPageViewState = mainPageViewState.copy(
                mediaList = listOf(result)
            )
        }
    }

    fun imagePickerResult(result: List<MediaResource>) {
        if (result.isNotEmpty()) {
            mainPageViewState = mainPageViewState.copy(mediaList = result)
        }
    }

    fun buildMatisse(): Matisse {
        val mimeTypes = when (mainPageViewState.mediaType) {
            MediaType.All -> {
                MimeType.ofAll()
            }

            MediaType.Image -> {
                MimeType.ofImage(hasGif = mainPageViewState.supportGif)
            }

            MediaType.Video -> {
                MimeType.ofVideo()
            }
        }
        return Matisse(
            maxSelectable = mainPageViewState.maxSelectable,
            mimeTypes = mimeTypes,
            captureStrategy = getMediaCaptureStrategy(),
            imageEngine = CoilImageEngine()
        )
    }

}