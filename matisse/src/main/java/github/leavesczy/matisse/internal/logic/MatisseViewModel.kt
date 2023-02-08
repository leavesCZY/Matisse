package github.leavesczy.matisse.internal.logic

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
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

    }

    private val permissionRequestingViewState = kotlin.run {
        val defaultBucket = MediaBucket(
            bucketId = DEFAULT_BUCKET_ID,
            bucketDisplayName = getString(R.string.matisse_default_bucket_name),
            bucketDisplayIcon = Uri.EMPTY,
            resources = emptyList(),
            supportCapture = matisse.captureStrategy.isEnabled()
        )
        MatisseViewState(
            matisse = matisse,
            state = MatisseState.PermissionRequesting,
            selectedResources = emptyList(),
            allBucket = listOf(defaultBucket),
            selectedBucket = defaultBucket
        )
    }

    private val permissionDeniedViewState =
        permissionRequestingViewState.copy(state = MatisseState.PermissionDenied)

    private val imageLoadingViewState =
        permissionRequestingViewState.copy(state = MatisseState.ImagesLoading)

    private val imageEmptyViewState =
        permissionRequestingViewState.copy(state = MatisseState.ImagesEmpty)

    var matisseViewState by mutableStateOf(value = permissionRequestingViewState)
        private set

    var bottomBarViewState by mutableStateOf(value = buildBottomBarViewState())
        private set

    var matissePreviewViewState by mutableStateOf(
        value = MatissePreviewViewState(
            matisse = matisse,
            visible = false,
            initialPage = 0,
            selectedResources = emptyList(),
            previewResources = emptyList()
        )
    )
        private set

    fun onRequestReadImagesPermission() {
        matisseViewState = permissionRequestingViewState
    }

    fun onRequestReadImagesPermissionResult(granted: Boolean) {
        if (granted) {
            viewModelScope.launch {
                matisseViewState = imageLoadingViewState
                loadResources()
            }
        } else {
            matisseViewState = permissionDeniedViewState
            showToast(message = getString(R.string.matisse_on_read_external_storage_permission_denied))
        }
    }

    private suspend fun loadResources() {
        val supportedMimeTypes = matisse.supportedMimeTypes
        if (supportedMimeTypes.isEmpty()) {
            matisseViewState = imageEmptyViewState
        } else {
            val allResources = MediaProvider.loadResources(
                context = getApplication(), filterMimeTypes = matisse.supportedMimeTypes
            )
            if (allResources.isEmpty()) {
                matisseViewState = imageEmptyViewState
            } else {
                val allBucket =
                    MediaProvider.groupByBucket(resources = allResources).toMutableList()
                val defaultBucket = MediaBucket(
                    bucketId = DEFAULT_BUCKET_ID,
                    bucketDisplayName = getString(R.string.matisse_default_bucket_name),
                    bucketDisplayIcon = allResources[0].uri,
                    resources = allResources,
                    supportCapture = matisse.captureStrategy.isEnabled()
                )
                allBucket.add(0, defaultBucket)
                matisseViewState = matisseViewState.copy(
                    state = MatisseState.ImagesLoaded,
                    allBucket = allBucket,
                    selectedBucket = defaultBucket
                )
            }
        }
    }

    fun onSelectBucket(bucket: MediaBucket) {
        matisseViewState = matisseViewState.copy(
            selectedBucket = bucket
        )
    }

    fun onMediaCheckChanged(mediaResource: MediaResource) {
        val selectedResources = matisseViewState.selectedResources
        if (selectedResources.size >= matisse.maxSelectable && !selectedResources.contains(
                mediaResource
            )
        ) {
            showToast(
                message = String.format(
                    getString(R.string.matisse_limit_the_number_of_pictures), matisse.maxSelectable
                )
            )
            return
        }
        val selectedResourcesMutable = selectedResources.toMutableList()
        val contains = selectedResourcesMutable.contains(element = mediaResource)
        if (contains) {
            selectedResourcesMutable.remove(element = mediaResource)
        } else {
            selectedResourcesMutable.add(element = mediaResource)
        }
        matisseViewState = matisseViewState.copy(selectedResources = selectedResourcesMutable)
        bottomBarViewState = buildBottomBarViewState()
        if (matissePreviewViewState.visible) {
            matissePreviewViewState =
                matissePreviewViewState.copy(selectedResources = selectedResourcesMutable)
        }
    }

    fun onClickMedia(mediaResource: MediaResource) {
        val previewResources = matisseViewState.selectedBucket.resources
        val selectedResources = matisseViewState.selectedResources
        val initialPage = previewResources.indexOf(element = mediaResource)
        matissePreviewViewState = matissePreviewViewState.copy(
            visible = true,
            initialPage = initialPage,
            selectedResources = selectedResources,
            previewResources = previewResources,
        )
    }

    fun onClickPreviewButton() {
        val selectedResources = matisseViewState.selectedResources.toList()
        if (selectedResources.isNotEmpty()) {
            matissePreviewViewState = matissePreviewViewState.copy(
                visible = true,
                initialPage = 0,
                selectedResources = selectedResources,
                previewResources = selectedResources,
            )
        }
    }

    fun onDismissPreviewPageRequest() {
        matissePreviewViewState = matissePreviewViewState.copy(visible = false)
    }

    private fun buildBottomBarViewState(): MatisseBottomBarViewState {
        val selectedMedia = matisseViewState.selectedResources
        return MatisseBottomBarViewState(
            previewText = String.format(
                getString(R.string.matisse_preview), selectedMedia.size, matisse.maxSelectable
            ),
            sureText = getString(R.string.matisse_sure),
            previewButtonClickable = selectedMedia.isNotEmpty(),
            sureButtonClickable = selectedMedia.isNotEmpty()
        )
    }

    private fun getString(@StringRes strId: Int): String {
        return getApplication<Application>().getString(strId)
    }

    private fun showToast(message: String) {
        Toast.makeText(
            getApplication(), message, Toast.LENGTH_SHORT
        ).show()
    }

}