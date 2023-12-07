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
import github.leavesczy.matisse.SmartCaptureStrategy
import github.leavesczy.matisse.samples.engine.coil.CoilImageEngine
import github.leavesczy.matisse.samples.engine.coil.CoilZoomImageEngine
import github.leavesczy.matisse.samples.engine.coil.CoilZoomableImageEngine
import github.leavesczy.matisse.samples.engine.glide.GlideImageEngine
import github.leavesczy.matisse.samples.engine.glide.GlideZoomImageEngine
import github.leavesczy.matisse.samples.engine.glide.GlideZoomableImageEngine

class MainViewModel : ViewModel() {

    private var darkTheme by mutableStateOf(value = false)

    var mainPageViewState by mutableStateOf(
        value = MainPageViewState(
            maxSelectable = 3,
            mediaType = MediaType.All,
            captureStrategy = MediaCaptureStrategy.Smart,
            capturePreferences = MediaCapturePreferences.Normal,
            filterStrategy = MediaFilterStrategy.Nothing,
            imageEngine = MediaImageEngine.Coil,
            mediaList = emptyList(),
            onMaxSelectableChanged = ::onMaxSelectableChanged,
            onMediaTypeChanged = ::onMediaTypeChanged,
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

    private fun getMediaCaptureStrategy(): CaptureStrategy? {
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
                null
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

    fun mediaPickerResult(result: List<MediaResource>?) {
        if (!result.isNullOrEmpty()) {
            mainPageViewState = mainPageViewState.copy(mediaList = result)
        }
    }

    fun buildMatisseCapture(): MatisseCapture? {
        val captureStrategy = getMediaCaptureStrategy() ?: return null
        return MatisseCapture(captureStrategy = captureStrategy)
    }

    fun buildMatisse(): Matisse {
        val mimeTypes = when (mainPageViewState.mediaType) {
            MediaType.All -> {
                MimeType.ofAll(hasGif = true)
            }

            MediaType.Image -> {
                MimeType.ofImage(hasGif = true)
            }

            MediaType.Video -> {
                MimeType.ofVideo()
            }
        }
        val imageEngine = when (mainPageViewState.imageEngine) {
            MediaImageEngine.Coil -> {
                CoilImageEngine()
            }

            MediaImageEngine.CoilZoomable -> {
                CoilZoomableImageEngine()
            }

            MediaImageEngine.CoilZoom -> {
                CoilZoomImageEngine()
            }

            MediaImageEngine.Glide -> {
                GlideImageEngine()
            }

            MediaImageEngine.GlideZoomable -> {
                GlideZoomableImageEngine()
            }

            MediaImageEngine.GlideZoom -> {
                GlideZoomImageEngine()
            }

        }
        val mediaFilter = when (mainPageViewState.filterStrategy) {
            MediaFilterStrategy.Nothing -> {
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