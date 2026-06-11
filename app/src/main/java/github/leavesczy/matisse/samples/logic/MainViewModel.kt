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
import github.leavesczy.matisse.FileProviderCaptureStrategy
import github.leavesczy.matisse.GlideImageEngine
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MatisseCapture
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaStoreCaptureStrategy
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.SmartCaptureStrategy

class MainViewModel : ViewModel() {

    var pageViewState by mutableStateOf(
        value = MainPageViewState(
            darkTheme = false,
            gridColumns = 4,
            maxSelectable = 3,
            fastSelect = false,
            singleMediaType = false,
            imageEngine = MediaImageEngine.Coil,
            mediaFilterStrategy = MediaFilterStrategy.None,
            captureStrategy = MediaCaptureStrategy.Smart,
            useFrontCamera = false,
            pickedMediaList = emptyList(),
            onGridColumnsChanged = ::onGridColumnsChanged,
            onMaxSelectableChanged = ::onMaxSelectableChanged,
            onFastSelectChanged = ::onFastSelectChanged,
            onSingleMediaTypeChanged = ::onSingleMediaTypeChanged,
            onImageEngineChanged = ::onImageEngineChanged,
            onMediaFilterStrategyChanged = ::onMediaFilterStrategyChanged,
            onCaptureStrategyChanged = ::onCaptureStrategyChanged,
            onUseFrontCameraChanged = ::onUseFrontCameraChanged,
            onThemeToggled = ::onThemeToggled
        )
    )
        private set

    private fun onGridColumnsChanged(gridColumns: Int) {
        pageViewState = pageViewState.copy(gridColumns = gridColumns)
    }

    private fun onMaxSelectableChanged(maxSelectable: Int) {
        val currentPageViewState = pageViewState
        val fastSelect = if (currentPageViewState.fastSelect) {
            maxSelectable == 1
        } else {
            false
        }
        pageViewState = currentPageViewState.copy(
            maxSelectable = maxSelectable,
            fastSelect = fastSelect
        )
    }

    private fun onFastSelectChanged(fastSelect: Boolean) {
        val currentPageViewState = pageViewState
        val maxSelectable = if (fastSelect) {
            1
        } else {
            currentPageViewState.maxSelectable
        }
        pageViewState = currentPageViewState.copy(
            maxSelectable = maxSelectable,
            fastSelect = fastSelect
        )
    }

    private fun onSingleMediaTypeChanged(singleMediaType: Boolean) {
        pageViewState = pageViewState.copy(singleMediaType = singleMediaType)
    }

    private fun onImageEngineChanged(imageEngine: MediaImageEngine) {
        pageViewState = pageViewState.copy(imageEngine = imageEngine)
    }

    private fun onMediaFilterStrategyChanged(mediaFilterStrategy: MediaFilterStrategy) {
        pageViewState = pageViewState.copy(mediaFilterStrategy = mediaFilterStrategy)
    }

    private fun onCaptureStrategyChanged(captureStrategy: MediaCaptureStrategy) {
        pageViewState = pageViewState.copy(captureStrategy = captureStrategy)
    }

    private fun onUseFrontCameraChanged(useFrontCamera: Boolean) {
        pageViewState = pageViewState.copy(useFrontCamera = useFrontCamera)
    }

    private fun onThemeToggled() {
        val currentPageViewState = pageViewState
        val darkTheme = !currentPageViewState.darkTheme
        pageViewState = currentPageViewState.copy(darkTheme = darkTheme)
        if (darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun resolveCaptureStrategy(): CaptureStrategy? {
        val currentPageViewState = pageViewState
        val fileProviderAuthority = "github.leavesczy.matisse.samples.FileProvider"
        val captureExtra = if (currentPageViewState.useFrontCamera) {
            val bundle = Bundle()
            bundle.putBoolean("android.intent.extra.USE_FRONT_CAMERA", true)
            bundle.putInt("android.intent.extras.CAMERA_FACING", 1)
            bundle
        } else {
            Bundle.EMPTY
        }
        return when (currentPageViewState.captureStrategy) {
            MediaCaptureStrategy.Smart -> {
                SmartCaptureStrategy(
                    fileProviderCaptureStrategy = FileProviderCaptureStrategy(
                        authority = fileProviderAuthority,
                        extra = captureExtra
                    )
                )
            }

            MediaCaptureStrategy.FileProvider -> {
                FileProviderCaptureStrategy(
                    authority = fileProviderAuthority,
                    extra = captureExtra
                )
            }

            MediaCaptureStrategy.MediaStore -> {
                MediaStoreCaptureStrategy(extra = captureExtra)
            }

            MediaCaptureStrategy.Disabled -> {
                null
            }
        }
    }

    fun buildMatisse(mediaType: MediaType): Matisse {
        val currentPageViewState = pageViewState
        val imageEngine = when (currentPageViewState.imageEngine) {
            MediaImageEngine.Coil -> {
                CoilImageEngine()
            }

            MediaImageEngine.Glide -> {
                GlideImageEngine()
            }
        }
        val ignoredMediaUris: Set<Uri>
        val selectedMediaUris: Set<Uri>
        when (currentPageViewState.mediaFilterStrategy) {
            MediaFilterStrategy.None -> {
                ignoredMediaUris = emptySet()
                selectedMediaUris = emptySet()
            }

            MediaFilterStrategy.ExcludePrevious -> {
                ignoredMediaUris = currentPageViewState.pickedMediaList.map { it.uri }.toSet()
                selectedMediaUris = emptySet()
            }

            MediaFilterStrategy.PreselectPrevious -> {
                ignoredMediaUris = emptySet()
                selectedMediaUris = currentPageViewState.pickedMediaList.map { it.uri }.toSet()
            }
        }
        val mediaFilter = DefaultMediaFilter(
            ignoredMimeTypes = emptySet(),
            ignoredMediaUris = ignoredMediaUris,
            selectedMediaUris = selectedMediaUris
        )
        return Matisse(
            gridColumns = currentPageViewState.gridColumns,
            maxSelectable = currentPageViewState.maxSelectable,
            fastSelect = currentPageViewState.fastSelect,
            mediaType = mediaType,
            mediaFilter = mediaFilter,
            imageEngine = imageEngine,
            singleMediaType = currentPageViewState.singleMediaType,
            captureStrategy = resolveCaptureStrategy()
        )
    }

    fun buildMatisseCapture(): MatisseCapture? {
        val captureStrategy = resolveCaptureStrategy() ?: return null
        return MatisseCapture(captureStrategy = captureStrategy)
    }

    fun onTakePictureResult(mediaResource: MediaResource?) {
        if (mediaResource != null) {
            pageViewState = pageViewState.copy(pickedMediaList = listOf(element = mediaResource))
        }
    }

    fun onMediaPickerResult(result: List<MediaResource>?) {
        if (!result.isNullOrEmpty()) {
            pageViewState = pageViewState.copy(pickedMediaList = result)
        }
    }

}