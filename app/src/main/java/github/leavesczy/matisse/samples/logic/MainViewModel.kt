package github.leavesczy.matisse.samples.logic

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.DefaultMediaFilter
import github.leavesczy.matisse.FileProviderCaptureStrategy
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MatisseCapture
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaStoreCaptureStrategy
import github.leavesczy.matisse.MimeType
import github.leavesczy.matisse.NothingCaptureStrategy
import github.leavesczy.matisse.SmartCaptureStrategy
import github.leavesczy.matisse.samples.engine.CoilImageEngine
import github.leavesczy.matisse.samples.engine.CoilZoomImageEngine
import github.leavesczy.matisse.samples.engine.GlideImageEngine
import github.leavesczy.matisse.samples.engine.GlideZoomImageEngine

class MainViewModel : ViewModel() {

    var darkTheme by mutableStateOf(value = false)
        private set

    var mainPageViewState by mutableStateOf(
        value = MainPageViewState(
            maxSelectable = 3,
            mediaType = MediaType.All,
            supportGif = true,
            captureStrategy = MediaCaptureStrategy.Smart,
            capturePreferences = MediaCapturePreferences.Normal,
            filterStrategy = MediaFilterStrategy.Close,
            imageEngine = MediaImageEngine.Coil,
            mediaList = emptyList(),
            onMaxSelectableChanged = ::onMaxSelectableChanged,
            onMediaTypeChanged = ::onMediaTypeChanged,
            onSupportGifChanged = ::onSupportGifChanged,
            onCaptureStrategyChanged = ::onCaptureStrategyChanged,
            onCapturePreferencesChanged = ::onCapturePreferencesChanged,
            onFilterStrategyChanged = ::onFilterStrategyChanged,
            onImageEngineChanged = ::onImageEngineChanged,
            switchTheme = ::switchTheme,
        )
    )
        private set

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

    private fun onImageEngineChanged(imageEngine: MediaImageEngine) {
        if (mainPageViewState.imageEngine != imageEngine) {
            mainPageViewState = mainPageViewState.copy(imageEngine = imageEngine)
        }
    }

    private fun onCaptureStrategyChanged(captureStrategy: MediaCaptureStrategy) {
        if (mainPageViewState.captureStrategy != captureStrategy) {
            mainPageViewState = mainPageViewState.copy(captureStrategy = captureStrategy)
        }
    }

    private fun onCapturePreferencesChanged(capturePreferences: MediaCapturePreferences) {
        if (mainPageViewState.capturePreferences != capturePreferences) {
            mainPageViewState = mainPageViewState.copy(capturePreferences = capturePreferences)
        }
    }

    private fun onFilterStrategyChanged(filterStrategy: MediaFilterStrategy) {
        if (mainPageViewState.filterStrategy != filterStrategy) {
            mainPageViewState = mainPageViewState.copy(filterStrategy = filterStrategy)
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

    private fun getMediaCaptureStrategy(): CaptureStrategy {
        val fileProviderAuthority = "github.leavesczy.matisse.samples.FileProvider"
        val extra = when (mainPageViewState.capturePreferences) {
            MediaCapturePreferences.Normal -> {
                Bundle.EMPTY
            }

            MediaCapturePreferences.Custom -> {
                val bundle = Bundle()
                bundle.putBoolean("android.intent.extra.USE_FRONT_CAMERA", true)
                bundle.putInt("android.intent.extras.CAMERA_FACING", 1)
                bundle
            }
        }
        return when (mainPageViewState.captureStrategy) {
            MediaCaptureStrategy.Nothing -> {
                NothingCaptureStrategy
            }

            MediaCaptureStrategy.FileProvider -> {
                FileProviderCaptureStrategy(authority = fileProviderAuthority, extra = extra)
            }

            MediaCaptureStrategy.MediaStore -> {
                MediaStoreCaptureStrategy()
            }

            MediaCaptureStrategy.Smart -> {
                SmartCaptureStrategy(authority = fileProviderAuthority, extra = extra)
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

    fun mediaPickerResult(result: List<MediaResource>) {
        if (result.isNotEmpty()) {
            mainPageViewState = mainPageViewState.copy(mediaList = result)
        }
    }

    fun buildMatisseCapture(): MatisseCapture {
        return MatisseCapture(
            captureStrategy = getMediaCaptureStrategy()
        )
    }

    fun buildMatisse(): Matisse {
        val hasGif = mainPageViewState.supportGif
        val mimeTypes = when (mainPageViewState.mediaType) {
            MediaType.All -> {
                MimeType.ofAll(hasGif = hasGif)
            }

            MediaType.Image -> {
                MimeType.ofImage(hasGif = hasGif)
            }

            MediaType.Video -> {
                MimeType.ofVideo()
            }
        }
        val imageEngine = when (mainPageViewState.imageEngine) {
            MediaImageEngine.Coil -> {
                CoilImageEngine()
            }

            MediaImageEngine.CoilZoom -> {
                CoilZoomImageEngine()
            }

            MediaImageEngine.Glide -> {
                GlideImageEngine()
            }

            MediaImageEngine.GlideZoom -> {
                GlideZoomImageEngine()
            }
        }
        val mediaFilter = when (mainPageViewState.filterStrategy) {
            MediaFilterStrategy.Close -> {
                DefaultMediaFilter(supportedMimeTypes = mimeTypes)
            }

            MediaFilterStrategy.IgnoreSelected -> {
                DefaultMediaFilter(
                    supportedMimeTypes = mimeTypes,
                    ignoredResourceUri = mainPageViewState.mediaList.map { it.uri }.toSet()
                )
            }

            MediaFilterStrategy.AttachSelected -> {
                DefaultMediaFilter(
                    supportedMimeTypes = mimeTypes,
                    selectedResourceUri = mainPageViewState.mediaList.map { it.uri }.toSet()
                )
            }
        }
        return Matisse(
            maxSelectable = mainPageViewState.maxSelectable,
            mediaFilter = mediaFilter,
            imageEngine = imageEngine,
            captureStrategy = getMediaCaptureStrategy()
        )
    }

}