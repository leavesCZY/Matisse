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
import github.leavesczy.matisse.samples.logic.engine.coil.CoilImageEngine
import github.leavesczy.matisse.samples.logic.engine.coil.CoilZoomImageEngine
import github.leavesczy.matisse.samples.logic.engine.coil.CoilZoomableImageEngine
import github.leavesczy.matisse.samples.logic.engine.glide.GlideImageEngine
import github.leavesczy.matisse.samples.logic.engine.glide.GlideZoomImageEngine
import github.leavesczy.matisse.samples.logic.engine.glide.GlideZoomableImageEngine

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 12:01
 * @Desc:
 */
class MainViewModel : ViewModel() {

    private var darkTheme by mutableStateOf(value = false)

    var mainPageViewState by mutableStateOf(
        value = MainPageViewState(
            maxSelectable = 3,
            singleMediaType = false,
            captureStrategy = MediaCaptureStrategy.Smart,
            capturePreferencesCustom = false,
            filterStrategy = MediaFilterStrategy.Nothing,
            imageEngine = MediaImageEngine.Glide,
            mediaList = emptyList(),
            onMaxSelectableChanged = ::onMaxSelectableChanged,
            onSingleMediaTypeChanged = ::onSingleMediaTypeChanged,
            onCaptureStrategyChanged = ::onCaptureStrategyChanged,
            onCapturePreferencesCustomChanged = ::onCapturePreferencesCustomChanged,
            onFilterStrategyChanged = ::onFilterStrategyChanged,
            onImageEngineChanged = ::onImageEngineChanged,
            switchTheme = ::switchTheme,
        )
    )
        private set

    private fun onMaxSelectableChanged(maxSelectable: Int) {
        mainPageViewState = mainPageViewState.copy(maxSelectable = maxSelectable)
    }

    private fun onSingleMediaTypeChanged(singleType: Boolean) {
        mainPageViewState = mainPageViewState.copy(singleMediaType = singleType)
    }

    private fun onImageEngineChanged(imageEngine: MediaImageEngine) {
        mainPageViewState = mainPageViewState.copy(imageEngine = imageEngine)
    }

    private fun onCaptureStrategyChanged(captureStrategy: MediaCaptureStrategy) {
        mainPageViewState = mainPageViewState.copy(captureStrategy = captureStrategy)
    }

    private fun onCapturePreferencesCustomChanged(custom: Boolean) {
        mainPageViewState = mainPageViewState.copy(capturePreferencesCustom = custom)
    }

    private fun onFilterStrategyChanged(filterStrategy: MediaFilterStrategy) {
        mainPageViewState = mainPageViewState.copy(filterStrategy = filterStrategy)
    }

    private fun switchTheme() {
        darkTheme = !darkTheme
        if (darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun getMediaCaptureStrategy(): CaptureStrategy? {
        val fileProviderAuthority = "github.leavesczy.matisse.samples.FileProvider"
        val captureExtra = if (mainPageViewState.capturePreferencesCustom) {
            val bundle = Bundle()
            bundle.putBoolean("android.intent.extra.USE_FRONT_CAMERA", true)
            bundle.putInt("android.intent.extras.CAMERA_FACING", 1)
            bundle
        } else {
            Bundle.EMPTY
        }
        return when (mainPageViewState.captureStrategy) {
            MediaCaptureStrategy.Smart -> {
                SmartCaptureStrategy(authority = fileProviderAuthority, extra = captureExtra)
            }

            MediaCaptureStrategy.FileProvider -> {
                FileProviderCaptureStrategy(authority = fileProviderAuthority, extra = captureExtra)
            }

            MediaCaptureStrategy.MediaStore -> {
                MediaStoreCaptureStrategy(extra = captureExtra)
            }

            MediaCaptureStrategy.Close -> {
                null
            }
        }
    }

    fun buildMatisse(mimeTypes: Set<MimeType>): Matisse {
        val imageEngine = when (mainPageViewState.imageEngine) {
            MediaImageEngine.Glide -> {
                GlideImageEngine()
            }

            MediaImageEngine.Coil -> {
                CoilImageEngine()
            }

            MediaImageEngine.GlideZoomable -> {
                GlideZoomableImageEngine()
            }


            MediaImageEngine.CoilZoomable -> {
                CoilZoomableImageEngine()
            }

            MediaImageEngine.GlideZoom -> {
                GlideZoomImageEngine()
            }

            MediaImageEngine.CoilZoom -> {
                CoilZoomImageEngine()
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
            singleMediaType = mainPageViewState.singleMediaType,
            captureStrategy = getMediaCaptureStrategy()
        )
    }

    fun buildMatisseCapture(): MatisseCapture? {
        val captureStrategy = getMediaCaptureStrategy() ?: return null
        return MatisseCapture(captureStrategy = captureStrategy)
    }

    fun takePictureResult(result: MediaResource?) {
        if (result != null) {
            mainPageViewState = mainPageViewState.copy(mediaList = listOf(element = result))
        }
    }

    fun mediaPickerResult(result: List<MediaResource>?) {
        if (!result.isNullOrEmpty()) {
            mainPageViewState = mainPageViewState.copy(mediaList = result)
        }
    }

}