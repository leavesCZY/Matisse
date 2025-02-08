package github.leavesczy.matisse.samples.logic

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.Coil3ImageEngine
import github.leavesczy.matisse.CoilImageEngine
import github.leavesczy.matisse.DefaultMediaFilter
import github.leavesczy.matisse.FileProviderCaptureStrategy
import github.leavesczy.matisse.GlideImageEngine
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MatisseCapture
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaStoreCaptureStrategy
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.SmartCaptureStrategy

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 12:01
 * @Desc:
 */
class MainViewModel : ViewModel() {

    private var darkTheme by mutableStateOf(value = false)

    var mainPageViewState by mutableStateOf(
        value = MainPageViewState(
            maxSelectable = 4,
            fastSelect = false,
            singleMediaType = false,
            includeGif = true,
            imageEngine = MediaImageEngine.Coil3,
            filterStrategy = MediaFilterStrategy.Nothing,
            captureStrategy = MediaCaptureStrategy.Smart,
            capturePreferencesCustom = false,
            mediaList = emptyList(),
            onMaxSelectableChanged = ::onMaxSelectableChanged,
            onFastSelectChanged = ::onFastSelectChanged,
            onSingleMediaTypeChanged = ::onSingleMediaTypeChanged,
            onIncludeGifChanged = ::onIncludeGifChanged,
            onImageEngineChanged = ::onImageEngineChanged,
            onFilterStrategyChanged = ::onFilterStrategyChanged,
            onCaptureStrategyChanged = ::onCaptureStrategyChanged,
            onCapturePreferencesCustomChanged = ::onCapturePreferencesCustomChanged,
            switchTheme = ::switchTheme
        )
    )
        private set

    private fun onMaxSelectableChanged(maxSelectable: Int) {
        val fastSelect = if (mainPageViewState.fastSelect) {
            maxSelectable == 1
        } else {
            false
        }
        mainPageViewState = mainPageViewState.copy(
            maxSelectable = maxSelectable,
            fastSelect = fastSelect
        )
    }

    private fun onFastSelectChanged(fastSelect: Boolean) {
        val maxSelectable = if (fastSelect) {
            1
        } else {
            mainPageViewState.maxSelectable
        }
        mainPageViewState = mainPageViewState.copy(
            maxSelectable = maxSelectable,
            fastSelect = fastSelect
        )
    }

    private fun onSingleMediaTypeChanged(singleType: Boolean) {
        mainPageViewState = mainPageViewState.copy(singleMediaType = singleType)
    }

    private fun onIncludeGifChanged(hasGif: Boolean) {
        mainPageViewState = mainPageViewState.copy(includeGif = hasGif)
    }

    private fun onImageEngineChanged(imageEngine: MediaImageEngine) {
        mainPageViewState = mainPageViewState.copy(imageEngine = imageEngine)
    }

    private fun onFilterStrategyChanged(filterStrategy: MediaFilterStrategy) {
        mainPageViewState = mainPageViewState.copy(filterStrategy = filterStrategy)
    }

    private fun onCaptureStrategyChanged(captureStrategy: MediaCaptureStrategy) {
        mainPageViewState = mainPageViewState.copy(captureStrategy = captureStrategy)
    }

    private fun onCapturePreferencesCustomChanged(custom: Boolean) {
        mainPageViewState = mainPageViewState.copy(capturePreferencesCustom = custom)
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

    fun buildMatisse(mediaType: MediaType): Matisse {
        val viewState = mainPageViewState
        val imageEngine = when (viewState.imageEngine) {
            MediaImageEngine.Coil3 -> {
                Coil3ImageEngine()
            }

            MediaImageEngine.Coil2 -> {
                CoilImageEngine()
            }

            MediaImageEngine.Glide -> {
                GlideImageEngine()
            }
        }
        val ignoredMimeType = if (viewState.includeGif) {
            emptySet()
        } else {
            setOf(element = "image/gif")
        }
        val ignoredResourceUri: Set<Uri>
        val selectedResourceUri: Set<Uri>
        when (viewState.filterStrategy) {
            MediaFilterStrategy.Nothing -> {
                ignoredResourceUri = emptySet()
                selectedResourceUri = emptySet()
            }

            MediaFilterStrategy.IgnoreSelected -> {
                ignoredResourceUri = viewState.mediaList.map { it.uri }.toSet()
                selectedResourceUri = emptySet()
            }

            MediaFilterStrategy.AttachSelected -> {
                ignoredResourceUri = emptySet()
                selectedResourceUri = viewState.mediaList.map { it.uri }.toSet()
            }
        }
        val mediaFilter = DefaultMediaFilter(
            ignoredMimeType = ignoredMimeType,
            ignoredResourceUri = ignoredResourceUri,
            selectedResourceUri = selectedResourceUri
        )
        return Matisse(
            maxSelectable = viewState.maxSelectable,
            fastSelect = viewState.fastSelect,
            mediaType = mediaType,
            mediaFilter = mediaFilter,
            imageEngine = imageEngine,
            singleMediaType = viewState.singleMediaType,
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