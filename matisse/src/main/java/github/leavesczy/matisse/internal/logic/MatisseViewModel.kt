package github.leavesczy.matisse.internal.logic

import android.app.Application
import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val context: Context
        get() = getApplication()

    private val permissionRequestingViewState = kotlin.run {
        val defaultBucket = MediaBucket(
            id = DEFAULT_BUCKET_ID,
            displayName = getString(R.string.matisse_default_bucket_name),
            displayIcon = Uri.EMPTY,
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

    var previewButtonViewState by mutableStateOf(value = buildPreviewButtonViewState())
        private set

    var sureButtonViewState by mutableStateOf(value = buildSureButtonViewState())
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

    fun onRequestReadMediaPermission() {
        matisseViewState = permissionRequestingViewState
    }

    fun onRequestReadMediaPermissionResult(granted: Boolean) {
        viewModelScope.launch(context = Dispatchers.Main.immediate) {
            if (granted) {
                loadResources()
            } else {
                matisseViewState = permissionDeniedViewState
                showToast(message = getString(R.string.matisse_on_read_external_storage_permission_denied))
            }
            previewButtonViewState = buildPreviewButtonViewState()
            sureButtonViewState = buildSureButtonViewState()
            dismissPreviewPage()
        }
    }

    private suspend fun loadResources() {
        matisseViewState = imageLoadingViewState
        val allResources = MediaProvider.loadResources(
            context = context,
            supportedMimeTypes = matisse.mimeTypes
        )
        if (allResources.isEmpty()) {
            matisseViewState = imageEmptyViewState
        } else {
            val defaultBucket = MediaBucket(
                id = DEFAULT_BUCKET_ID,
                displayName = getString(R.string.matisse_default_bucket_name),
                displayIcon = allResources[0].uri,
                resources = allResources,
                supportCapture = matisse.captureStrategy.isEnabled()
            )
            val allBucket = groupByBucket(
                defaultBucket = defaultBucket,
                resources = allResources
            )
            matisseViewState = matisseViewState.copy(
                state = MatisseState.ImagesLoaded,
                allBucket = allBucket,
                selectedBucket = defaultBucket,
                selectedResources = emptyList()
            )
        }
    }

    private suspend fun groupByBucket(
        defaultBucket: MediaBucket,
        resources: List<MediaResource>
    ): List<MediaBucket> {
        return withContext(context = Dispatchers.IO) {
            val resourcesMap = linkedMapOf<String, MutableList<MediaResource>>()
            resources.forEach { res ->
                val bucketDisplayName = res.bucketDisplayName
                if (bucketDisplayName.isNotBlank()) {
                    val bucketId = res.bucketId
                    val list = resourcesMap[bucketId]
                    if (list == null) {
                        resourcesMap[bucketId] = mutableListOf(res)
                    } else {
                        list.add(element = res)
                    }
                }
            }
            val allBucketList = buildList {
                add(element = defaultBucket)
                resourcesMap.forEach {
                    val bucketId = it.key
                    val resourcesList = it.value
                    val bucketDisplayName = resourcesList[0].bucketDisplayName
                    add(
                        element = MediaBucket(
                            id = bucketId,
                            displayName = bucketDisplayName,
                            displayIcon = resourcesList[0].uri,
                            resources = resourcesList,
                            supportCapture = false
                        )
                    )
                }
            }
            return@withContext allBucketList
        }
    }

    fun onSelectBucket(bucket: MediaBucket) {
        matisseViewState = matisseViewState.copy(selectedBucket = bucket)
    }

    fun onMediaCheckChanged(mediaResource: MediaResource) {
        val selectedResources = matisseViewState.selectedResources.toMutableList()
        val alreadySelected = selectedResources.contains(element = mediaResource)
        if (alreadySelected) {
            selectedResources.remove(element = mediaResource)
        } else {
            val maxSelectable = matisse.maxSelectable
            if (maxSelectable == 1) {
                selectedResources.clear()
                selectedResources.add(element = mediaResource)
            } else if (selectedResources.size >= maxSelectable) {
                showToast(
                    message = String.format(
                        getString(R.string.matisse_limit_the_number_of_pictures),
                        matisse.maxSelectable
                    )
                )
                return
            } else {
                selectedResources.add(element = mediaResource)
            }
        }
        matisseViewState = matisseViewState.copy(selectedResources = selectedResources)
        previewButtonViewState = buildPreviewButtonViewState()
        sureButtonViewState = buildSureButtonViewState()
        if (matissePreviewViewState.visible) {
            matissePreviewViewState =
                matissePreviewViewState.copy(selectedResources = selectedResources)
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

    private fun onClickPreviewButton() {
        val selectedResources = matisseViewState.selectedResources.toList()
        if (selectedResources.isNotEmpty()) {
            matissePreviewViewState = matissePreviewViewState.copy(
                visible = true,
                initialPage = 0,
                selectedResources = selectedResources,
                previewResources = selectedResources
            )
        }
    }

    fun dismissPreviewPage() {
        if (matissePreviewViewState.visible) {
            matissePreviewViewState = matissePreviewViewState.copy(visible = false)
        }
    }

    private fun buildPreviewButtonViewState(): MatissePreviewButtonViewState {
        val selectedMedia = matisseViewState.selectedResources
        return MatissePreviewButtonViewState(
            text = getString(R.string.matisse_preview),
            clickable = selectedMedia.isNotEmpty(),
            onClick = ::onClickPreviewButton
        )
    }

    private fun buildSureButtonViewState(): MatisseSureButtonViewState {
        val selectedMedia = matisseViewState.selectedResources
        return MatisseSureButtonViewState(
            text = String.format(
                getString(R.string.matisse_sure),
                selectedMedia.size,
                matisse.maxSelectable
            ),
            clickable = selectedMedia.isNotEmpty()
        )
    }

    private fun getString(@StringRes strId: Int): String {
        return context.getString(strId)
    }

    private fun showToast(message: String) {
        if (message.isNotBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}