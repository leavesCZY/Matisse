package github.leavesczy.matisse.internal.logic

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 */
internal class MatisseViewModel(application: Application, matisse: Matisse) :
    BaseMatisseViewModel(application = application) {

    val maxSelectable = matisse.maxSelectable

    private val fastSelect = matisse.fastSelect

    val mediaType = matisse.mediaType

    val singleMediaType = matisse.singleMediaType

    val captureStrategy = matisse.captureStrategy

    private val mediaFilter = matisse.mediaFilter

    private val defaultBucketId = "&__matisseDefaultBucketId__&"

    private val defaultBucket = MatisseMediaBucket(
        bucketId = defaultBucketId,
        bucketName = getString(id = R.string.matisse_bucket_all),
        supportCapture = captureStrategy != null,
        resources = emptyList()
    )

    private val allMediaResources = mutableListOf<MatisseMediaExtend>()

    var pageViewState by mutableStateOf(
        value = MatissePageViewState(
            matisse = matisse,
            selectedBucket = defaultBucket,
            mediaBucketsInfo = emptyList(),
            placeholderState = null,
            onClickBucket = ::onClickBucket,
            onClickMedia = ::onClickMedia,
            onMediaCheckChanged = ::onMediaCheckChanged
        )
    )
        private set

    var bottomBarViewState by mutableStateOf(
        value = buildBottomBarViewState()
    )
        private set

    var previewPageViewState by mutableStateOf(
        value = MatissePreviewPageViewState(
            visible = false,
            initialPage = 0,
            selectedImageSize = 0,
            maxSelectable = maxSelectable,
            previewResources = emptyList(),
            onMediaCheckChanged = {},
            onDismissRequest = {}
        )
    )
        private set

    private val unselectedDisabledMediaSelectState = MatisseMediaSelectState(
        isSelected = false,
        isEnabled = false,
        positionIndex = -1
    )

    private val unselectedEnabledMediaSelectState = MatisseMediaSelectState(
        isSelected = false,
        isEnabled = true,
        positionIndex = -1
    )

    fun requestReadMediaPermissionResult(granted: Boolean) {
        viewModelScope.launch(context = Dispatchers.Main.immediate) {
            showLoadingDialog()
            dismissPreviewPage()
            allMediaResources.clear()
            if (granted) {
                val allResources = loadMediaResources()
                allMediaResources.addAll(elements = allResources)
                val collectBucket = defaultBucket.copy(resources = allResources)
                val allMediaBuckets = buildList {
                    add(
                        element = MatisseMediaBucketInfo(
                            bucketId = collectBucket.bucketId,
                            bucketName = collectBucket.bucketName,
                            size = collectBucket.resources.size,
                            firstMedia = collectBucket.resources.firstOrNull()?.media
                        )
                    )
                    addAll(
                        elements = allResources.groupBy {
                            it.bucketId
                        }.mapNotNull {
                            val bucketId = it.key
                            val resources = it.value
                            val firstResource = resources.firstOrNull()
                            val bucketName = firstResource?.bucketName
                            if (bucketName.isNullOrBlank()) {
                                null
                            } else {
                                MatisseMediaBucketInfo(
                                    bucketId = bucketId,
                                    bucketName = bucketName,
                                    size = resources.size,
                                    firstMedia = firstResource.media
                                )
                            }
                        }
                    )
                }
                pageViewState = pageViewState.copy(
                    selectedBucket = collectBucket,
                    mediaBucketsInfo = allMediaBuckets,
                    placeholderState = if (allResources.isEmpty()) {
                        val includeImage = mediaType.includeImage
                        val includeVideo = mediaType.includeVideo
                        if (includeImage && includeVideo) {
                            MatissePlaceholderState.NoMedia
                        } else if (includeVideo) {
                            MatissePlaceholderState.NoVideo
                        } else {
                            MatissePlaceholderState.NoImage
                        }
                    } else {
                        null
                    }
                )
                defaultSelectedResources(allMediaResources = allResources)
            } else {
                setNotReadMediaPermissionViewState()
                showToast(id = R.string.matisse_error_read_media_permission)
            }
            bottomBarViewState = buildBottomBarViewState()
            dismissLoadingDialog()
        }
    }

    private suspend fun defaultSelectedResources(allMediaResources: List<MatisseMediaExtend>) {
        val defaultSelectedMediaIds = if (mediaFilter == null || fastSelect) {
            emptySet()
        } else {
            allMediaResources.filter {
                mediaFilter.selectMedia(mediaResource = it.media)
            }.map {
                it.mediaId
            }.toSet()
        }
        if (defaultSelectedMediaIds.isNotEmpty()) {
            var positionIndex = 0
            allMediaResources.forEach { media ->
                val selectState = media.selectState as MutableState<MatisseMediaSelectState>
                if (defaultSelectedMediaIds.contains(element = media.mediaId)) {
                    selectState.value = MatisseMediaSelectState(
                        isSelected = true,
                        isEnabled = true,
                        positionIndex = positionIndex
                    )
                    positionIndex++
                } else {
                    selectState.value = if (defaultSelectedMediaIds.size >= maxSelectable) {
                        unselectedDisabledMediaSelectState
                    } else {
                        unselectedEnabledMediaSelectState
                    }
                }
            }
        }
    }

    private suspend fun loadMediaResources(): List<MatisseMediaExtend> {
        return withContext(context = Dispatchers.Default) {
            val resourcesInfo = MediaProvider.loadResources(
                context = context,
                mediaType = mediaType
            )
            if (resourcesInfo.isNullOrEmpty()) {
                emptyList()
            } else {
                resourcesInfo.mapNotNull {
                    val media = MediaResource(
                        uri = it.uri,
                        path = it.path,
                        name = it.name,
                        mimeType = it.mimeType,
                        size = it.size
                    )
                    if (mediaFilter?.ignoreMedia(mediaResource = media) == true) {
                        null
                    } else {
                        MatisseMediaExtend(
                            mediaId = it.mediaId,
                            bucketId = it.bucketId,
                            bucketName = it.bucketName,
                            media = media,
                            selectState = mutableStateOf(value = unselectedEnabledMediaSelectState)
                        )
                    }
                }
            }
        }
    }

    private fun setNotReadMediaPermissionViewState() {
        pageViewState = pageViewState.copy(
            selectedBucket = defaultBucket,
            mediaBucketsInfo = listOf(
                element = MatisseMediaBucketInfo(
                    bucketId = defaultBucket.bucketId,
                    bucketName = defaultBucket.bucketName,
                    size = defaultBucket.resources.size,
                    firstMedia = defaultBucket.resources.firstOrNull()?.media,
                )
            ),
            placeholderState = MatissePlaceholderState.NoPermission
        )
    }

    private suspend fun onClickBucket(bucketId: String) {
        val viewState = pageViewState
        val isDefaultBucket = bucketId == defaultBucketId
        val bucketName = viewState.mediaBucketsInfo.first {
            it.bucketId == bucketId
        }.bucketName
        val supportCapture = isDefaultBucket && defaultBucket.supportCapture
        val resources = if (isDefaultBucket) {
            allMediaResources
        } else {
            allMediaResources.filter {
                it.bucketId == bucketId
            }
        }
        pageViewState = viewState.copy(
            selectedBucket = MatisseMediaBucket(
                bucketId = bucketId,
                bucketName = bucketName,
                supportCapture = supportCapture,
                resources = resources
            )
        )
    }

    private fun onMediaCheckChanged(mediaResource: MatisseMediaExtend) {
        val selectState = mediaResource.selectState as MutableState<MatisseMediaSelectState>
        if (selectState.value.isSelected) {
            selectState.value = unselectedEnabledMediaSelectState
        } else {
            if (maxSelectable == 1) {
                resetAllMediaSelectState(state = unselectedEnabledMediaSelectState)
            } else {
                val selectedResources = filterSelectedMediaResource()
                if (selectedResources.size >= maxSelectable) {
                    showToast(text = textWhenTheQuantityExceedsTheLimit())
                    return
                } else if (singleMediaType) {
                    val illegalMediaType = selectedResources.any {
                        it.media.isImage != mediaResource.media.isImage
                    }
                    if (illegalMediaType) {
                        showToast(id = R.string.matisse_error_mixed_media)
                        return
                    }
                }
            }
            selectState.value = MatisseMediaSelectState(
                isSelected = true,
                isEnabled = true,
                positionIndex = filterSelectedMediaResource().size
            )
        }
        rearrangeMediaPosition()
        updatePreviewPageIfNeed()
        bottomBarViewState = buildBottomBarViewState()
    }

    private fun rearrangeMediaPosition() {
        val selectedMedia = filterSelectedMediaResource()
        resetAllMediaSelectState(
            state = if (selectedMedia.size >= maxSelectable) {
                unselectedDisabledMediaSelectState
            } else {
                unselectedEnabledMediaSelectState
            }
        )
        selectedMedia.forEachIndexed { index, media ->
            val selectState = media.selectState as MutableState<MatisseMediaSelectState>
            selectState.value = MatisseMediaSelectState(
                isSelected = true,
                isEnabled = true,
                positionIndex = index
            )
        }
    }

    private fun updatePreviewPageIfNeed() {
        val viewState = previewPageViewState
        if (viewState.visible) {
            val selectedResources = filterSelectedMediaResource()
            previewPageViewState = viewState.copy(selectedImageSize = selectedResources.size)
        }
    }

    private fun textWhenTheQuantityExceedsTheLimit(): String {
        val includeImage = mediaType.includeImage
        val includeVideo = mediaType.includeVideo
        val stringId = if (includeImage && !includeVideo) {
            R.string.matisse_error_max_images
        } else if (!includeImage && includeVideo) {
            R.string.matisse_error_max_videos
        } else {
            R.string.matisse_error_max_media
        }
        return getString(
            id = stringId,
            maxSelectable
        )
    }

    private fun buildBottomBarViewState(): MatisseBottomBarViewState {
        val selected = filterSelectedMediaResource()
        return MatisseBottomBarViewState(
            selectedImageSize = selected.size,
            maxSelectable = maxSelectable,
            previewButtonClickable = selected.isNotEmpty(),
            onClickPreviewButton = ::onClickPreviewButton
        )
    }

    private fun onClickMedia(mediaResource: MatisseMediaExtend) {
        val totalResources = pageViewState.selectedBucket.resources
        previewResource(
            initialPage = totalResources.indexOf(element = mediaResource),
            totalResources = totalResources,
            selectedResources = filterSelectedMediaResource()
        )
    }

    private fun onClickPreviewButton() {
        val selected = filterSelectedMediaResource()
        previewResource(
            initialPage = 0,
            totalResources = selected,
            selectedResources = selected
        )
    }

    private fun previewResource(
        initialPage: Int,
        totalResources: List<MatisseMediaExtend>,
        selectedResources: List<MatisseMediaExtend>
    ) {
        previewPageViewState = previewPageViewState.copy(
            visible = true,
            initialPage = initialPage,
            selectedImageSize = selectedResources.size,
            previewResources = totalResources,
            onMediaCheckChanged = ::onMediaCheckChanged,
            onDismissRequest = ::dismissPreviewPage
        )
    }

    private fun dismissPreviewPage() {
        val viewState = previewPageViewState
        if (viewState.visible) {
            previewPageViewState = viewState.copy(
                visible = false,
                onMediaCheckChanged = {},
                onDismissRequest = {}
            )
        }
    }

    fun filterSelectedMedia(): List<MediaResource> {
        return filterSelectedMediaResource().map {
            it.media
        }
    }

    private fun filterSelectedMediaResource(): List<MatisseMediaExtend> {
        return allMediaResources.filter {
            it.selectState.value.isSelected
        }.sortedBy {
            it.selectState.value.positionIndex
        }
    }

    private fun resetAllMediaSelectState(state: MatisseMediaSelectState) {
        allMediaResources.forEach {
            (it.selectState as MutableState<MatisseMediaSelectState>).value = state
        }
    }

}