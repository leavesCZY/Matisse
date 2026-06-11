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

internal class MatisseViewModel(application: Application, matisse: Matisse) :
    MatissePreviewImageViewModel(application = application, matisse = matisse) {

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
            placeholderState = MatissePlaceholderState.Nothing(hasReadMediaPermission = false),
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
            dismissPreviewImagePage()
            dismissVideoPlayerPage()
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
                        MatissePlaceholderState.NoMedia(
                            requestImage = mediaType.includeImage,
                            requestVideo = mediaType.includeVideo
                        )
                    } else {
                        MatissePlaceholderState.Nothing(hasReadMediaPermission = true)
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
                        mimeType = it.mimeType
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

    override fun onPreviewImagePageMediaCheckChanged(mediaExtend: MatisseMediaExtend) {
        onMediaCheckChanged(mediaExtend = mediaExtend)
    }

    private fun onMediaCheckChanged(mediaExtend: MatisseMediaExtend) {
        val selectState = mediaExtend.selectState as MutableState<MatisseMediaSelectState>
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
                        it.media.isImage != mediaExtend.media.isImage
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
        updatePreviewImagePageIfNeed()
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
        showPreviewImagePage(
            initialPage = totalResources.indexOf(element = mediaResource),
            totalResources = totalResources,
            selectedResources = filterSelectedMediaResource()
        )
    }

    private fun onClickPreviewButton() {
        val selected = filterSelectedMediaResource()
        showPreviewImagePage(
            initialPage = 0,
            totalResources = selected,
            selectedResources = selected
        )
    }

    fun filterSelectedMedia(): List<MediaResource> {
        return filterSelectedMediaResource().map {
            it.media
        }
    }

    override fun filterSelectedMediaResource(): List<MatisseMediaExtend> {
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