package github.leavesczy.matisse.internal.logic

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.grid.LazyGridState
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

    private val defaultBucket = MediaBucket(
        id = DEFAULT_BUCKET_ID,
        displayName = getString(R.string.matisse_default_bucket_name),
        displayIcon = Uri.EMPTY,
        resources = emptyList(),
        supportCapture = matisse.captureStrategy.isEnabled()
    )

    private val nothingMatissePageViewState = MatissePageViewState(
        lazyGridState = LazyGridState(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0
        ),
        selectedBucket = defaultBucket,
        onClickMedia = ::onClickMedia,
        onMediaCheckChanged = ::onMediaCheckChanged
    )

    private val nothingMatisseTopBarViewState = MatisseTopBarViewState(
        matisse = matisse,
        title = defaultBucket.displayName,
        mediaBuckets = emptyList(),
        onClickBucket = ::onClickBucket
    )

    var selectedResources by mutableStateOf(
        value = emptyList<MediaResource>()
    )
        private set

    var matissePageViewState by mutableStateOf(
        value = nothingMatissePageViewState
    )
        private set

    var matisseTopBarViewState by mutableStateOf(
        value = nothingMatisseTopBarViewState
    )
        private set

    var matisseBottomBarViewState by mutableStateOf(
        value = buildMatisseBottomBarViewState()
    )
        private set

    var matissePreviewPageViewState by mutableStateOf(
        value = MatissePreviewPageViewState(
            visible = false,
            initialPage = 0,
            sureButtonText = "",
            sureButtonClickable = false,
            selectedResources = emptyList(),
            previewResources = emptyList(),
            onDismissRequest = ::dismissPreviewPage,
            onMediaCheckChanged = ::onMediaCheckChanged
        )
    )
        private set

    fun requestReadMediaPermissionResult(granted: Boolean) {
        viewModelScope.launch(context = Dispatchers.Main.immediate) {
            dismissPreviewPage()
            matisseBottomBarViewState = buildMatisseBottomBarViewState()
            selectedResources = emptyList()
            if (granted) {
                val resources = MediaProvider.loadResources(
                    context = context,
                    supportedMimeTypes = matisse.mimeTypes
                )
                val allBucket = groupByBucket(
                    resources = resources
                )
                matissePageViewState = matissePageViewState.copy(
                    selectedBucket = allBucket[0]
                )
                matisseTopBarViewState = matisseTopBarViewState.copy(
                    mediaBuckets = allBucket
                )
            } else {
                matissePageViewState = nothingMatissePageViewState
                matisseTopBarViewState = nothingMatisseTopBarViewState
                showToast(message = getString(R.string.matisse_on_read_external_storage_permission_denied))
            }
        }
    }

    private fun onClickBucket(mediaBucket: MediaBucket) {
        if (matissePageViewState.selectedBucket != mediaBucket) {
            matisseTopBarViewState = matisseTopBarViewState.copy(title = mediaBucket.displayName)
            matissePageViewState = matissePageViewState.copy(
                selectedBucket = mediaBucket,
                lazyGridState = LazyGridState(
                    firstVisibleItemIndex = 0,
                    firstVisibleItemScrollOffset = 0
                )
            )
        }
    }

    private suspend fun groupByBucket(resources: List<MediaResource>): List<MediaBucket> {
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
            buildList {
                add(
                    element = MediaBucket(
                        id = DEFAULT_BUCKET_ID,
                        displayName = getString(R.string.matisse_default_bucket_name),
                        displayIcon = if (resources.isEmpty()) {
                            Uri.EMPTY
                        } else {
                            resources[0].uri
                        },
                        resources = resources,
                        supportCapture = matisse.captureStrategy.isEnabled()
                    )
                )
                resourcesMap.forEach {
                    val bucketId = it.key
                    val resourcesList = it.value
                    val firstResource = resourcesList[0]
                    add(
                        element = MediaBucket(
                            id = bucketId,
                            displayName = firstResource.bucketDisplayName,
                            displayIcon = firstResource.uri,
                            resources = resourcesList,
                            supportCapture = false
                        )
                    )
                }
            }
        }
    }

    private fun onMediaCheckChanged(mediaResource: MediaResource) {
        val selectedResourcesMutable = selectedResources.toMutableList()
        val alreadySelected = selectedResourcesMutable.contains(element = mediaResource)
        if (alreadySelected) {
            selectedResourcesMutable.remove(element = mediaResource)
        } else {
            val maxSelectable = matisse.maxSelectable
            if (maxSelectable == 1) {
                selectedResourcesMutable.clear()
                selectedResourcesMutable.add(element = mediaResource)
            } else if (selectedResourcesMutable.size >= maxSelectable) {
                showToast(
                    message = String.format(
                        getString(R.string.matisse_limit_the_number_of_pictures),
                        maxSelectable
                    )
                )
                return
            } else {
                selectedResourcesMutable.add(element = mediaResource)
            }
        }
        selectedResources = selectedResourcesMutable
        matisseBottomBarViewState = buildMatisseBottomBarViewState()
        if (matissePreviewPageViewState.visible) {
            matissePreviewPageViewState = matissePreviewPageViewState.copy(
                selectedResources = selectedResources,
                sureButtonText = String.format(
                    getString(R.string.matisse_sure),
                    selectedResources.size,
                    matisse.maxSelectable
                ),
                sureButtonClickable = selectedResources.isNotEmpty()
            )
        }
    }

    private fun previewImage(
        initialMedia: MediaResource?,
        previewResources: List<MediaResource>
    ) {
        if (previewResources.isEmpty()) {
            return
        }
        val mSelectedResources = selectedResources
        val initialPage = if (initialMedia == null) {
            0
        } else {
            previewResources.indexOf(element = initialMedia)
        }.coerceAtLeast(minimumValue = 0)
        matissePreviewPageViewState = matissePreviewPageViewState.copy(
            visible = true,
            initialPage = initialPage,
            sureButtonText = String.format(
                getString(R.string.matisse_sure),
                mSelectedResources.size,
                matisse.maxSelectable
            ),
            sureButtonClickable = mSelectedResources.isNotEmpty(),
            selectedResources = mSelectedResources,
            previewResources = previewResources,
        )
    }

    private fun onClickMedia(mediaResource: MediaResource) {
        previewImage(
            initialMedia = mediaResource,
            previewResources = matissePageViewState.selectedBucket.resources
        )
    }

    private fun onClickPreviewButton() {
        if (selectedResources.isNotEmpty()) {
            previewImage(
                initialMedia = null,
                previewResources = selectedResources
            )
        }
    }

    private fun dismissPreviewPage() {
        if (matissePreviewPageViewState.visible) {
            matissePreviewPageViewState = matissePreviewPageViewState.copy(visible = false)
        }
    }

    private fun buildMatisseBottomBarViewState(): MatisseBottomBarViewState {
        return MatisseBottomBarViewState(
            previewButtonText = getString(R.string.matisse_preview),
            previewButtonClickable = selectedResources.isNotEmpty(),
            onClickPreviewButton = ::onClickPreviewButton,
            sureButtonText = String.format(
                getString(R.string.matisse_sure),
                selectedResources.size,
                matisse.maxSelectable
            ),
            sureButtonClickable = selectedResources.isNotEmpty()
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