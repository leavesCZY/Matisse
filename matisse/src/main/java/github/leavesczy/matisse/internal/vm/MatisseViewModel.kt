package github.leavesczy.matisse.internal.vm

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResources
import github.leavesczy.matisse.internal.model.MatisseState
import github.leavesczy.matisse.internal.model.MatisseViewState
import github.leavesczy.matisse.internal.model.MediaBucket
import github.leavesczy.matisse.internal.provider.MediaProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 19:19
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
internal class MatisseViewModel(application: Application, private val matisse: Matisse) :
    AndroidViewModel(application) {

    companion object {

        private const val DEFAULT_BUCKET_ID = "&__defaultBucketId__&"

        private val CAPTURE_MEDIA_RESOURCES = MediaResources(
            uri = Uri.EMPTY,
            displayName = "",
            mimeType = "",
            width = 0,
            height = 0,
            orientation = 0,
            size = 0L,
            path = "",
            bucketId = "capture",
            bucketDisplayName = ""
        )

    }

    private val _selectedMediaResources = mutableListOf<MediaResources>()

    val selectedMediaResources: List<MediaResources> = _selectedMediaResources

    private val permissionRequestingViewState = kotlin.run {
        val defaultBucket = MediaBucket(
            bucketId = DEFAULT_BUCKET_ID,
            bucketDisplayName = matisse.theme.topAppBarTheme.defaultBucketName,
            bucketDisplayIcon = Uri.EMPTY,
            resources = emptyList(),
            displayResources = if (matisse.captureStrategy.isEnabled()) {
                listOf(CAPTURE_MEDIA_RESOURCES)
            } else {
                emptyList()
            },
        )
        MatisseViewState(
            matisse = matisse,
            state = MatisseState.PermissionRequesting,
            allBucket = listOf(defaultBucket),
            selectedBucket = defaultBucket,
            selectedMediaResources = selectedMediaResources,
            previewText = getPreviewText(),
            previewBtnClickable = isPreviewBtnClickable(),
            sureText = getSureText(),
            sureBtnClickable = isSureBtnClickable(),
        )
    }

    private val permissionDeniedViewState =
        permissionRequestingViewState.copy(state = MatisseState.PermissionDenied)

    private val imageLoadingViewState =
        permissionRequestingViewState.copy(state = MatisseState.ImagesLoading)

    private val imageEmptyViewState =
        permissionRequestingViewState.copy(state = MatisseState.ImagesEmpty)

    private val _matisseViewState = MutableStateFlow(permissionRequestingViewState)

    val matisseViewState = _matisseViewState.asStateFlow()

    fun onRequestReadExternalStoragePermission() {
        viewModelScope.launch {
            _matisseViewState.emit(permissionRequestingViewState)
        }
    }

    fun onRequestReadExternalStoragePermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                _matisseViewState.emit(imageLoadingViewState)
                loadImages()
            } else {
                _matisseViewState.emit(permissionDeniedViewState)
                val tips = matisse.tips.onReadExternalStorageDenied
                if (tips.isNotBlank()) {
                    Toast.makeText(getApplication(), tips, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun onSelectBucket(bucket: MediaBucket) {
        viewModelScope.launch {
            val copy = _matisseViewState.value.copy(selectedBucket = bucket)
            _matisseViewState.emit(copy)
        }
    }

    fun onMediaCheckChanged(mediaResources: MediaResources) {
        if (_selectedMediaResources.size >= matisse.maxSelectable && !_selectedMediaResources.contains(
                mediaResources
            )
        ) {
            val tips = matisse.tips.onSelectLimit(
                _selectedMediaResources.size,
                matisse.maxSelectable
            )
            if (tips.isNotBlank()) {
                Toast.makeText(getApplication(), tips, Toast.LENGTH_SHORT).show()
            }
            return
        }
        viewModelScope.launch {
            val contains = _selectedMediaResources.contains(mediaResources)
            if (contains) {
                _selectedMediaResources.remove(mediaResources)
            } else {
                _selectedMediaResources.add(mediaResources)
            }
            _matisseViewState.emit(
                _matisseViewState.value.copy(
                    selectedMediaResources = _selectedMediaResources,
                    previewText = getPreviewText(),
                    previewBtnClickable = isPreviewBtnClickable(),
                    sureText = getSureText(),
                    sureBtnClickable = isSureBtnClickable()
                )
            )
        }
    }

    private suspend fun loadImages() {
        val supportedMimeTypes = matisse.supportedMimeTypes
        if (supportedMimeTypes.isEmpty()) {
            _matisseViewState.emit(imageEmptyViewState)
        } else {
            val allResources = MediaProvider.loadResources(
                context = getApplication<Application>(),
                filterMimeTypes = matisse.supportedMimeTypes
            )
            if (allResources.isEmpty()) {
                _matisseViewState.emit(imageEmptyViewState)
            } else {
                val allBucket =
                    MediaProvider.groupByBucket(resources = allResources).toMutableList()
                val defaultBucket = MediaBucket(
                    bucketId = DEFAULT_BUCKET_ID,
                    bucketDisplayName = matisse.theme.topAppBarTheme.defaultBucketName,
                    bucketDisplayIcon = allResources[0].uri,
                    resources = allResources,
                    displayResources = if (matisse.captureStrategy.isEnabled()) {
                        mutableListOf(CAPTURE_MEDIA_RESOURCES).apply {
                            addAll(allResources)
                        }
                    } else {
                        allResources
                    }
                )
                allBucket.add(0, defaultBucket)
                _matisseViewState.emit(
                    MatisseViewState(
                        matisse = matisse,
                        state = MatisseState.ImagesLoaded,
                        allBucket = allBucket,
                        selectedBucket = defaultBucket,
                        selectedMediaResources = selectedMediaResources,
                        previewText = getPreviewText(),
                        previewBtnClickable = isPreviewBtnClickable(),
                        sureText = getSureText(),
                        sureBtnClickable = isSureBtnClickable(),
                    )
                )
            }
        }
    }

    private fun getPreviewText(): String {
        return matisse.theme.previewButtonTheme.textBuilder(
            _selectedMediaResources.size,
            matisse.maxSelectable
        )
    }

    private fun getSureText(): String {
        return matisse.theme.sureButtonTheme.textBuilder(
            _selectedMediaResources.size,
            matisse.maxSelectable
        )
    }

    private fun isPreviewBtnClickable(): Boolean {
        return _selectedMediaResources.isNotEmpty()
    }

    private fun isSureBtnClickable(): Boolean {
        return _selectedMediaResources.isNotEmpty()
    }

    fun filterSelectedBucketResources(): List<MediaResources> {
        val selectedBucket = _matisseViewState.value.selectedBucket
        return if (selectedBucket.bucketId == DEFAULT_BUCKET_ID && matisse.captureStrategy.isEnabled()) {
            return selectedBucket.resources.toMutableList().apply {
                remove(CAPTURE_MEDIA_RESOURCES)
            }
        } else {
            selectedBucket.resources
        }
    }

    fun isCaptureMediaResources(mediaResources: MediaResources): Boolean {
        return mediaResources == CAPTURE_MEDIA_RESOURCES
    }

}