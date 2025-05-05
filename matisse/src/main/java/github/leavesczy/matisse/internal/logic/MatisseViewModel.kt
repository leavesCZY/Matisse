package github.leavesczy.matisse.internal.logic

import android.app.Application
import android.content.Context
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 19:19
 * @Desc:
 */
internal class MatisseViewModel(application: Application, matisse: Matisse) :
    AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    val maxSelectable = matisse.maxSelectable

    private val imageEngine = matisse.imageEngine

    private val fastSelect = matisse.fastSelect

    val mediaType = matisse.mediaType

    val singleMediaType = matisse.singleMediaType

    val captureStrategy = matisse.captureStrategy

    private val mediaFilter = matisse.mediaFilter

    private val defaultBucketId = "&__matisseDefaultBucketId__&"

    private val defaultBucket = MatisseMediaBucket(
        id = defaultBucketId,
        name = getString(id = R.string.matisse_default_bucket_name),
        supportCapture = true,
        resources = emptyList()
    )

    private val defaultBucketInfo = MatisseMediaBucketInfo(
        id = defaultBucket.id,
        name = defaultBucket.name,
        size = defaultBucket.resources.size,
        firstMedia = defaultBucket.resources.firstOrNull(),
    )

    private val allMediaBuckets = mutableListOf<MatisseMediaBucket>()

    var selectedResources by mutableStateOf(
        value = emptyList<MediaResource>()
    )
        private set

    var matissePageViewState by mutableStateOf(
        value = MatissePageViewState(
            maxSelectable = maxSelectable,
            fastSelect = fastSelect,
            lazyGridState = LazyGridState(),
            captureStrategy = captureStrategy,
            selectedBucket = defaultBucket,
            imageEngine = imageEngine,
            onClickMedia = ::onClickMedia,
            onMediaCheckChanged = ::onMediaCheckChanged
        )
    )
        private set

    var matisseTopBarViewState by mutableStateOf(
        value = MatisseTopBarViewState(
            title = defaultBucket.name,
            mediaBuckets = listOf(element = defaultBucketInfo),
            onClickBucket = ::onClickBucket
        )
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
            maxSelectable = maxSelectable,
            sureButtonText = "",
            sureButtonClickable = false,
            selectedResources = emptyList(),
            previewResources = emptyList(),
            onMediaCheckChanged = {},
            onDismissRequest = {}
        )
    )
        private set

    var loadingDialogVisible by mutableStateOf(value = false)
        private set

    fun requestReadMediaPermissionResult(granted: Boolean) {
        viewModelScope.launch(context = Dispatchers.Main.immediate) {
            showLoadingDialog()
            dismissPreviewPage()
            allMediaBuckets.clear()
            if (granted) {
                val allResources = MediaProvider.loadResources(
                    context = context,
                    mediaType = mediaType,
                    mediaFilter = mediaFilter
                )
                val allBucket = groupByBucket(resources = allResources)
                allMediaBuckets.addAll(elements = allBucket)
                val firstBucket = allBucket[0]
                matissePageViewState = matissePageViewState.copy(selectedBucket = firstBucket)
                matisseTopBarViewState = matisseTopBarViewState.copy(
                    title = firstBucket.name,
                    mediaBuckets = allBucket.map {
                        MatisseMediaBucketInfo(
                            id = it.id,
                            name = it.name,
                            size = it.resources.size,
                            firstMedia = it.resources.first()
                        )
                    }
                )
                val defaultSelected = if (mediaFilter == null || fastSelect) {
                    emptyList()
                } else {
                    allResources.filter {
                        mediaFilter.selectMedia(mediaResource = it)
                    }
                }
                selectedResources = defaultSelected
            } else {
                resetViewState()
                showToast(id = R.string.matisse_read_media_permission_denied)
            }
            matisseBottomBarViewState = buildMatisseBottomBarViewState()
            dismissLoadingDialog()
        }
    }

    private suspend fun groupByBucket(resources: List<MediaResource>): List<MatisseMediaBucket> {
        return withContext(context = Dispatchers.Default) {
            val resourcesMap = linkedMapOf<String, MutableList<MediaResource>>()
            resources.forEach { res ->
                if (res.bucketName.isNotBlank()) {
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
                    element = MatisseMediaBucket(
                        id = defaultBucketId,
                        name = getString(id = R.string.matisse_default_bucket_name),
                        resources = resources,
                        supportCapture = true
                    )
                )
                addAll(
                    elements = resourcesMap.map {
                        MatisseMediaBucket(
                            id = it.key,
                            name = it.value[0].bucketName,
                            resources = it.value,
                            supportCapture = false
                        )
                    }
                )
            }
        }
    }

    private fun resetViewState() {
        selectedResources = emptyList()
        matissePageViewState = matissePageViewState.copy(selectedBucket = defaultBucket)
        matisseTopBarViewState = matisseTopBarViewState.copy(
            title = defaultBucketInfo.name,
            mediaBuckets = listOf(element = defaultBucketInfo),
        )
    }

    private suspend fun onClickBucket(bucketId: String) {
        val selectedBucket = allMediaBuckets.find {
            it.id == bucketId
        } ?: defaultBucket
        matisseTopBarViewState = matisseTopBarViewState.copy(title = selectedBucket.name)
        matissePageViewState = matissePageViewState.copy(selectedBucket = selectedBucket)
        delay(timeMillis = 80)
        matissePageViewState.lazyGridState.animateScrollToItem(index = 0)
    }

    private fun onMediaCheckChanged(mediaResource: MediaResource) {
        val selectedResourcesMutable = selectedResources.toMutableList()
        val alreadySelected = selectedResourcesMutable.contains(element = mediaResource)
        if (alreadySelected) {
            selectedResourcesMutable.remove(element = mediaResource)
        } else {
            when {
                maxSelectable == 1 -> {
                    selectedResourcesMutable.clear()
                }

                selectedResourcesMutable.size >= maxSelectable -> {
                    showToast(
                        text = getString(
                            id = R.string.matisse_limit_the_number_of_media,
                            maxSelectable
                        )
                    )
                    return
                }

                singleMediaType -> {
                    val illegalMediaType = selectedResourcesMutable.any {
                        it.isImage != mediaResource.isImage
                    }
                    if (illegalMediaType) {
                        showToast(id = R.string.matisse_cannot_select_both_picture_and_video_at_the_same_time)
                        return
                    }
                }
            }
            selectedResourcesMutable.add(element = mediaResource)
        }
        selectedResources = selectedResourcesMutable
        matisseBottomBarViewState = buildMatisseBottomBarViewState()
        if (matissePreviewPageViewState.visible) {
            matissePreviewPageViewState = matissePreviewPageViewState.copy(
                selectedResources = selectedResources,
                sureButtonText = getString(
                    id = R.string.matisse_sure,
                    selectedResources.size,
                    maxSelectable
                ),
                sureButtonClickable = selectedResources.isNotEmpty()
            )
        }
    }

    private fun buildMatisseBottomBarViewState(): MatisseBottomBarViewState {
        return MatisseBottomBarViewState(
            previewButtonText = getString(id = R.string.matisse_preview),
            previewButtonClickable = selectedResources.isNotEmpty(),
            onClickPreviewButton = ::onClickPreviewButton,
            sureButtonText = getString(
                id = R.string.matisse_sure,
                selectedResources.size,
                maxSelectable
            ),
            sureButtonClickable = selectedResources.isNotEmpty()
        )
    }

    private fun onClickMedia(mediaResource: MediaResource) {
        previewResource(
            initialMedia = mediaResource,
            previewResources = matissePageViewState.selectedBucket.resources
        )
    }

    private fun onClickPreviewButton() {
        if (selectedResources.isNotEmpty()) {
            previewResource(
                initialMedia = null,
                previewResources = selectedResources
            )
        }
    }

    private fun previewResource(
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
            previewResources.indexOf(element = initialMedia).coerceAtLeast(minimumValue = 0)
        }
        matissePreviewPageViewState = matissePreviewPageViewState.copy(
            visible = true,
            initialPage = initialPage,
            sureButtonText = getString(
                id = R.string.matisse_sure,
                mSelectedResources.size,
                maxSelectable
            ),
            sureButtonClickable = mSelectedResources.isNotEmpty(),
            selectedResources = mSelectedResources,
            previewResources = previewResources,
            onMediaCheckChanged = ::onMediaCheckChanged,
            onDismissRequest = ::dismissPreviewPage
        )
    }

    private fun dismissPreviewPage() {
        if (matissePreviewPageViewState.visible) {
            matissePreviewPageViewState = matissePreviewPageViewState.copy(visible = false)
        }
    }

    private fun showLoadingDialog() {
        loadingDialogVisible = true
    }

    private fun dismissLoadingDialog() {
        loadingDialogVisible = false
    }

    private fun getString(@StringRes id: Int): String {
        return context.getString(id)
    }

    private fun getString(@StringRes id: Int, vararg formatArgs: Any): String {
        return context.getString(id, *formatArgs)
    }

    private fun showToast(@StringRes id: Int) {
        showToast(text = getString(id = id))
    }

    private fun showToast(text: String) {
        if (text.isNotBlank()) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

}