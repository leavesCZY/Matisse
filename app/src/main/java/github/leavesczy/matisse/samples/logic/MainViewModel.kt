package github.leavesczy.matisse.samples.logic

import android.app.Application
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.CoilImageEngine
import github.leavesczy.matisse.FileProviderCaptureStrategy
import github.leavesczy.matisse.GlideImageEngine
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MatisseCapture
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaStoreCaptureStrategy
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.SmartCaptureStrategy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application = application) {

    private val testImageInserter = TestImageInserter(
        contentResolver = application.contentResolver
    )

    var pageViewState by mutableStateOf(
        value = MainPageViewState(
            darkTheme = false,
            gridColumns = 3,
            maxSelectable = 3,
            returnOnTap = false,
            allowMixedMedia = true,
            imageEngine = MediaImageEngine.Coil,
            captureStrategy = MediaCaptureStrategy.Smart,
            isInsertingPagingTestImages = false,
            isInsertingImageEngineTestImages = false,
            pickedMediaList = emptyList(),
            onGridColumnsChanged = ::onGridColumnsChanged,
            onMaxSelectableChanged = ::onMaxSelectableChanged,
            onReturnOnTapChanged = ::onReturnOnTapChanged,
            onAllowMixedMediaChanged = ::onAllowMixedMediaChanged,
            onImageEngineChanged = ::onImageEngineChanged,
            onCaptureStrategyChanged = ::onCaptureStrategyChanged,
            onToggleTheme = ::onToggleTheme
        )
    )
        private set

    private fun onGridColumnsChanged(gridColumns: Int) {
        pageViewState = pageViewState.copy(gridColumns = gridColumns)
    }

    private fun onMaxSelectableChanged(maxSelectable: Int) {
        val currentPageViewState = pageViewState
        val returnOnTap = if (currentPageViewState.returnOnTap) {
            maxSelectable == 1
        } else {
            false
        }
        pageViewState = currentPageViewState.copy(
            maxSelectable = maxSelectable,
            returnOnTap = returnOnTap
        )
    }

    private fun onReturnOnTapChanged(returnOnTap: Boolean) {
        val currentPageViewState = pageViewState
        val maxSelectable = if (returnOnTap) {
            1
        } else {
            currentPageViewState.maxSelectable
        }
        pageViewState = currentPageViewState.copy(
            maxSelectable = maxSelectable,
            returnOnTap = returnOnTap
        )
    }

    private fun onAllowMixedMediaChanged(allowMixedMedia: Boolean) {
        pageViewState = pageViewState.copy(allowMixedMedia = allowMixedMedia)
    }

    private fun onImageEngineChanged(imageEngine: MediaImageEngine) {
        pageViewState = pageViewState.copy(imageEngine = imageEngine)
    }

    private fun onCaptureStrategyChanged(captureStrategy: MediaCaptureStrategy) {
        pageViewState = pageViewState.copy(captureStrategy = captureStrategy)
    }

    private fun onToggleTheme() {
        val currentPageViewState = pageViewState
        val darkTheme = !currentPageViewState.darkTheme
        pageViewState = currentPageViewState.copy(darkTheme = darkTheme)
        if (darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun insertPagingTestImages() {
        if (pageViewState.isInsertingPagingTestImages ||
            pageViewState.isInsertingImageEngineTestImages
        ) {
            return
        }
        pageViewState = pageViewState.copy(isInsertingPagingTestImages = true)
        viewModelScope.launch {
            try {
                val insertedCount = withContext(context = Dispatchers.IO) {
                    testImageInserter.insertPagingTestImages()
                }
                Toast.makeText(
                    getApplication(),
                    "已插入 $insertedCount / ${TestImageInserter.PAGING_TEST_IMAGE_COUNT} 张测试图片",
                    Toast.LENGTH_LONG
                ).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                exception.printStackTrace()
                Toast.makeText(getApplication(), "插入测试图片失败", Toast.LENGTH_LONG).show()
            } finally {
                pageViewState = pageViewState.copy(isInsertingPagingTestImages = false)
            }
        }
    }

    fun insertImageEngineTestImages() {
        if (pageViewState.isInsertingImageEngineTestImages ||
            pageViewState.isInsertingPagingTestImages
        ) {
            return
        }
        pageViewState = pageViewState.copy(isInsertingImageEngineTestImages = true)
        viewModelScope.launch {
            try {
                val insertedCount = withContext(context = Dispatchers.IO) {
                    testImageInserter.insertImageEngineTestImages()
                }
                Toast.makeText(
                    getApplication(),
                    "已插入 $insertedCount / ${TestImageInserter.IMAGE_ENGINE_TEST_SPEC_COUNT} 张多尺寸测试图片",
                    Toast.LENGTH_LONG
                ).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                exception.printStackTrace()
                Toast.makeText(getApplication(), "插入多尺寸图片失败", Toast.LENGTH_LONG).show()
            } finally {
                pageViewState = pageViewState.copy(isInsertingImageEngineTestImages = false)
            }
        }
    }

    private fun resolveCaptureStrategy(): CaptureStrategy? {
        val currentPageViewState = pageViewState
        val fileProviderAuthority = "github.leavesczy.matisse.samples.FileProvider"
        return when (currentPageViewState.captureStrategy) {
            MediaCaptureStrategy.Smart -> {
                SmartCaptureStrategy(
                    fileProviderCaptureStrategy = FileProviderCaptureStrategy(
                        authority = fileProviderAuthority
                    )
                )
            }

            MediaCaptureStrategy.FileProvider -> {
                FileProviderCaptureStrategy(
                    authority = fileProviderAuthority
                )
            }

            MediaCaptureStrategy.MediaStore -> {
                MediaStoreCaptureStrategy()
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
        return Matisse(
            gridColumns = currentPageViewState.gridColumns,
            maxSelectable = currentPageViewState.maxSelectable,
            returnOnTap = currentPageViewState.returnOnTap,
            mediaType = mediaType,
            imageEngine = imageEngine,
            allowMixedMedia = currentPageViewState.allowMixedMedia,
            captureStrategy = if (mediaType.includesImage) {
                resolveCaptureStrategy()
            } else {
                null
            }
        )
    }

    fun buildMatisseCapture(): MatisseCapture? {
        val captureStrategy = resolveCaptureStrategy() ?: return null
        return MatisseCapture(captureStrategy = captureStrategy)
    }

    fun onCaptureResult(mediaResource: MediaResource?) {
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