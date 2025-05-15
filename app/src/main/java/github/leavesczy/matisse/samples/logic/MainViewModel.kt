package github.leavesczy.matisse.samples.logic

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.CoilImageEngine
import github.leavesczy.matisse.DefaultMediaFilter
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

    var pageViewState by mutableStateOf(
        value = MainPageViewState(
            gridColumns = 4,
            maxSelectable = 3,
            fastSelect = false,
            singleMediaType = false,
            imageEngine = MediaImageEngine.Coil,
            filterStrategy = MediaFilterStrategy.Nothing,
            captureStrategy = MediaCaptureStrategy.Smart,
            capturePreferencesCustom = false,
            mediaList = emptyList(),
            onGridColumnsChanged = ::onGridColumnsChanged,
            onMaxSelectableChanged = ::onMaxSelectableChanged,
            onFastSelectChanged = ::onFastSelectChanged,
            onSingleMediaTypeChanged = ::onSingleMediaTypeChanged,
            onImageEngineChanged = ::onImageEngineChanged,
            onFilterStrategyChanged = ::onFilterStrategyChanged,
            onCaptureStrategyChanged = ::onCaptureStrategyChanged,
            onCapturePreferencesCustomChanged = ::onCapturePreferencesCustomChanged,
            switchTheme = ::switchTheme
        )
    )
        private set

    private fun onGridColumnsChanged(gridColumns: Int) {
        pageViewState = pageViewState.copy(gridColumns = gridColumns)
    }

    private fun onMaxSelectableChanged(maxSelectable: Int) {
        val viewState = pageViewState
        val fastSelect = if (viewState.fastSelect) {
            maxSelectable == 1
        } else {
            false
        }
        pageViewState = viewState.copy(
            maxSelectable = maxSelectable,
            fastSelect = fastSelect
        )
    }

    private fun onFastSelectChanged(fastSelect: Boolean) {
        val viewState = pageViewState
        val maxSelectable = if (fastSelect) {
            1
        } else {
            viewState.maxSelectable
        }
        pageViewState = viewState.copy(
            maxSelectable = maxSelectable,
            fastSelect = fastSelect
        )
    }

    private fun onSingleMediaTypeChanged(singleType: Boolean) {
        pageViewState = pageViewState.copy(singleMediaType = singleType)
    }

    private fun onImageEngineChanged(imageEngine: MediaImageEngine) {
        pageViewState = pageViewState.copy(imageEngine = imageEngine)
    }

    private fun onFilterStrategyChanged(filterStrategy: MediaFilterStrategy) {
        pageViewState = pageViewState.copy(filterStrategy = filterStrategy)
    }

    private fun onCaptureStrategyChanged(captureStrategy: MediaCaptureStrategy) {
        pageViewState = pageViewState.copy(captureStrategy = captureStrategy)
    }

    private fun onCapturePreferencesCustomChanged(custom: Boolean) {
        pageViewState = pageViewState.copy(capturePreferencesCustom = custom)
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
        val viewState = pageViewState
        val fileProviderAuthority = "github.leavesczy.matisse.samples.FileProvider"
        val captureExtra = if (viewState.capturePreferencesCustom) {
            val bundle = Bundle()
            bundle.putBoolean("android.intent.extra.USE_FRONT_CAMERA", true)
            bundle.putInt("android.intent.extras.CAMERA_FACING", 1)
            bundle
        } else {
            Bundle.EMPTY
        }
        return when (viewState.captureStrategy) {
            MediaCaptureStrategy.Smart -> {
                SmartCaptureStrategy(
                    fileProviderCaptureStrategy = CustomFileProviderCaptureStrategy(
                        authority = fileProviderAuthority,
                        extra = captureExtra
                    )
                )
            }

            MediaCaptureStrategy.FileProvider -> {
                CustomFileProviderCaptureStrategy(
                    authority = fileProviderAuthority,
                    extra = captureExtra
                )
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
        val viewState = pageViewState
        val imageEngine = when (viewState.imageEngine) {
            MediaImageEngine.Coil -> {
                CoilImageEngine()
            }

            MediaImageEngine.Glide -> {
                GlideImageEngine()
            }
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
            ignoredMimeType = emptySet(),
            ignoredResourceUri = ignoredResourceUri,
            selectedResourceUri = selectedResourceUri
        )
        return Matisse(
            gridColumns = viewState.gridColumns,
            maxSelectable = viewState.maxSelectable,
            fastSelect = viewState.fastSelect,
            mediaType = mediaType,
            mediaFilter = mediaFilter,
            imageEngine = imageEngine,
            singleMediaType = viewState.singleMediaType,
            captureStrategy = getMediaCaptureStrategy()
        )
    }

    fun buildMediaCaptureStrategy(): MatisseCapture? {
        val captureStrategy = getMediaCaptureStrategy() ?: return null
        return MatisseCapture(captureStrategy = captureStrategy)
    }

    fun takePictureResult(result: MediaResource?) {
        if (result != null) {
            pageViewState = pageViewState.copy(mediaList = listOf(element = result))
        }
    }

    fun mediaPickerResult(result: List<MediaResource>?) {
        if (!result.isNullOrEmpty()) {
            pageViewState = pageViewState.copy(mediaList = result)
        }
    }

}