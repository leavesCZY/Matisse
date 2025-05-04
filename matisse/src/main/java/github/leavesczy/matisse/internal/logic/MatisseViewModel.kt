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
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaType
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
internal class MatisseViewModel(application: Application, private val matisse: Matisse) :
    AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    val maxSelectable: Int
        get() = matisse.maxSelectable

    private val fastSelect: Boolean
        get() = matisse.fastSelect

    private val imageEngine: ImageEngine
        get() = matisse.imageEngine

    val mediaType: MediaType
        get() = matisse.mediaType

    val singleMediaType: Boolean
        get() = matisse.singleMediaType

    val captureStrategy: CaptureStrategy?
        get() = matisse.captureStrategy

    private val defaultBucketId = "&__matisseDefaultBucketId__&"

    private val defaultBucket = MatisseMediaBucket(
        id = defaultBucketId,
        name = getString(R.string.matisse_default_bucket_name),
        supportCapture = true,
        resources = emptyList()
    )

    private val defaultBucketInfo = MatisseMediaBucketInfo(
        id = defaultBucket.id,
        name = defaultBucket.name,
        bucketSize = defaultBucket.resources.size,
        firstMedia = defaultBucket.resources.firstOrNull(),
    )

    private val mediaBuckets = mutableListOf<MatisseMediaBucket>()

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
            onDismissRequest = ::dismissPreviewPage,
            onMediaCheckChanged = ::onMediaCheckChanged
        )
    )
        private set

    var loadingDialogVisible by mutableStateOf(value = false)
        private set

    fun requestReadMediaPermissionResult(granted: Boolean) {
        viewModelScope.launch(context = Dispatchers.Main.immediate) {
            showLoadingDialog()
            dismissPreviewPage()
            mediaBuckets.clear()
            val mediaFilter = matisse.mediaFilter
            if (granted) {
                val allResources = MediaProvider.loadResources(
                    context = context,
                    mediaType = mediaType,
                    mediaFilter = mediaFilter
                )
                val allBucket = groupByBucket(resources = allResources)
                mediaBuckets.addAll(elements = allBucket)
                val firstBucket = allBucket[0]
                matissePageViewState = matissePageViewState.copy(selectedBucket = firstBucket)
                matisseTopBarViewState = matisseTopBarViewState.copy(
                    title = firstBucket.name,
                    mediaBuckets = allBucket.map {
                        MatisseMediaBucketInfo(
                            id = it.id,
                            name = it.name,
                            bucketSize = it.resources.size,
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
                resetViewStateIfNeeded()
                showToast(message = getString(R.string.matisse_read_media_permission_denied))
            }
            matisseBottomBarViewState = buildMatisseBottomBarViewState()
            dismissLoadingDialog()
        }
    }

    private fun resetViewStateIfNeeded() {
        selectedResources = emptyList()
        matissePageViewState = matissePageViewState.copy(selectedBucket = defaultBucket)
        matisseTopBarViewState = matisseTopBarViewState.copy(
            title = defaultBucketInfo.name,
            mediaBuckets = listOf(element = defaultBucketInfo),
        )
    }

    private suspend fun onClickBucket(mediaBucketInfo: MatisseMediaBucketInfo) {
        matisseTopBarViewState = matisseTopBarViewState.copy(title = mediaBucketInfo.name)
        matissePageViewState = matissePageViewState.copy(selectedBucket = mediaBuckets.first {
            it.id == mediaBucketInfo.id
        })
        delay(timeMillis = 20)
        matissePageViewState.lazyGridState.animateScrollToItem(index = 0)
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
                        name = getString(R.string.matisse_default_bucket_name),
                        resources = resources,
                        supportCapture = true
                    )
                )
                resourcesMap.forEach {
                    val bucketId = it.key
                    val resourceList = it.value
                    val bucketName = resourceList[0].bucketName
                    add(
                        element = MatisseMediaBucket(
                            id = bucketId,
                            name = bucketName,
                            resources = resourceList,
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
            when {
                maxSelectable == 1 -> {
                    selectedResourcesMutable.clear()
                }

                selectedResourcesMutable.size >= maxSelectable -> {
                    showToast(
                        message = String.format(
                            getString(R.string.matisse_limit_the_number_of_media), maxSelectable
                        )
                    )
                    return
                }

                singleMediaType -> {
                    val illegalMediaType = selectedResourcesMutable.any {
                        it.isImage != mediaResource.isImage
                    }
                    if (illegalMediaType) {
                        showToast(message = getString(R.string.matisse_cannot_select_both_picture_and_video_at_the_same_time))
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
                sureButtonText = String.format(
                    getString(R.string.matisse_sure),
                    selectedResources.size,
                    maxSelectable
                ),
                sureButtonClickable = selectedResources.isNotEmpty()
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
            sureButtonText = String.format(
                getString(R.string.matisse_sure),
                mSelectedResources.size,
                maxSelectable
            ),
            sureButtonClickable = mSelectedResources.isNotEmpty(),
            selectedResources = mSelectedResources,
            previewResources = previewResources,
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
                maxSelectable
            ),
            sureButtonClickable = selectedResources.isNotEmpty()
        )
    }

    private fun showLoadingDialog() {
        loadingDialogVisible = true
    }

    private fun dismissLoadingDialog() {
        loadingDialogVisible = false
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