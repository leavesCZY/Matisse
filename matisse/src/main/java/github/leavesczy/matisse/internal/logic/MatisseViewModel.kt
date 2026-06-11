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
        supportsCapture = captureStrategy != null,
        mediaItems = emptyList()
    )

    private val allMediaItems = mutableListOf<MatisseMediaItem>()

    var pageViewState by mutableStateOf(
        value = MatissePageViewState(
            matisse = matisse,
            selectedBucket = defaultBucket,
            mediaBuckets = emptyList(),
            placeholderState = MatissePlaceholderState.Ready(hasReadMediaPermission = false),
            onBucketClick = ::onBucketClick,
            onMediaClick = ::onMediaClick,
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

    fun onReadMediaPermissionResult(granted: Boolean) {
        viewModelScope.launch(context = Dispatchers.Main.immediate) {
            showLoadingDialog()
            dismissPreviewImagePage()
            dismissVideoPlayerPage()
            allMediaItems.clear()
            if (granted) {
                val loadedMediaItems = loadMediaItems()
                allMediaItems.addAll(elements = loadedMediaItems)
                val allMediaBucket = defaultBucket.copy(mediaItems = loadedMediaItems)
                val allMediaBuckets = buildList {
                    add(
                        element = MatisseMediaBucketInfo(
                            bucketId = allMediaBucket.bucketId,
                            bucketName = allMediaBucket.bucketName,
                            itemCount = allMediaBucket.mediaItems.size,
                            coverMedia = allMediaBucket.mediaItems.firstOrNull()?.mediaResource
                        )
                    )
                    addAll(
                        elements = loadedMediaItems.groupBy {
                            it.bucketId
                        }.mapNotNull {
                            val bucketId = it.key
                            val bucketMediaItems = it.value
                            val firstMediaItem = bucketMediaItems.firstOrNull()
                            val bucketName = firstMediaItem?.bucketName
                            if (bucketName.isNullOrBlank()) {
                                null
                            } else {
                                MatisseMediaBucketInfo(
                                    bucketId = bucketId,
                                    bucketName = bucketName,
                                    itemCount = bucketMediaItems.size,
                                    coverMedia = firstMediaItem.mediaResource
                                )
                            }
                        }
                    )
                }
                pageViewState = pageViewState.copy(
                    selectedBucket = allMediaBucket,
                    mediaBuckets = allMediaBuckets,
                    placeholderState = if (loadedMediaItems.isEmpty()) {
                        MatissePlaceholderState.NoMedia(
                            includesImages = mediaType.includeImage,
                            includesVideos = mediaType.includeVideo
                        )
                    } else {
                        MatissePlaceholderState.Ready(hasReadMediaPermission = true)
                    }
                )
                applyDefaultMediaSelection(loadedMediaItems = loadedMediaItems)
            } else {
                setReadMediaPermissionDeniedState()
                showToast(id = R.string.matisse_error_read_media_permission)
            }
            bottomBarViewState = buildBottomBarViewState()
            dismissLoadingDialog()
        }
    }

    private suspend fun applyDefaultMediaSelection(loadedMediaItems: List<MatisseMediaItem>) {
        val defaultSelectedMediaIds = if (mediaFilter == null || fastSelect) {
            emptySet()
        } else {
            loadedMediaItems.filter {
                mediaFilter.selectMedia(mediaResource = it.mediaResource)
            }.map {
                it.mediaId
            }.toSet()
        }
        if (defaultSelectedMediaIds.isNotEmpty()) {
            var positionIndex = 0
            loadedMediaItems.forEach { media ->
                val selectionState = media.selectionState as MutableState<MatisseMediaSelectState>
                if (defaultSelectedMediaIds.contains(element = media.mediaId)) {
                    selectionState.value = MatisseMediaSelectState(
                        isSelected = true,
                        isEnabled = true,
                        positionIndex = positionIndex
                    )
                    positionIndex++
                } else {
                    selectionState.value = if (defaultSelectedMediaIds.size >= maxSelectable) {
                        unselectedDisabledMediaSelectState
                    } else {
                        unselectedEnabledMediaSelectState
                    }
                }
            }
        }
    }

    private suspend fun loadMediaItems(): List<MatisseMediaItem> {
        return withContext(context = Dispatchers.Default) {
            val mediaInfoList = MediaProvider.loadMediaInfoList(
                context = context,
                mediaType = mediaType
            )
            if (mediaInfoList.isNullOrEmpty()) {
                emptyList()
            } else {
                mediaInfoList.mapNotNull {
                    val mediaResource = MediaResource(
                        uri = it.uri,
                        mimeType = it.mimeType
                    )
                    if (mediaFilter?.ignoreMedia(mediaResource = mediaResource) == true) {
                        null
                    } else {
                        MatisseMediaItem(
                            mediaId = it.mediaId,
                            bucketId = it.bucketId,
                            bucketName = it.bucketName,
                            mediaResource = mediaResource,
                            selectionState = mutableStateOf(value = unselectedEnabledMediaSelectState)
                        )
                    }
                }
            }
        }
    }

    private fun setReadMediaPermissionDeniedState() {
        pageViewState = pageViewState.copy(
            selectedBucket = defaultBucket,
            mediaBuckets = listOf(
                element = MatisseMediaBucketInfo(
                    bucketId = defaultBucket.bucketId,
                    bucketName = defaultBucket.bucketName,
                    itemCount = defaultBucket.mediaItems.size,
                    coverMedia = defaultBucket.mediaItems.firstOrNull()?.mediaResource,
                )
            ),
            placeholderState = MatissePlaceholderState.NoPermission
        )
    }

    private suspend fun onBucketClick(bucketId: String) {
        val currentPageViewState = pageViewState
        val isDefaultBucket = bucketId == defaultBucketId
        val bucketName = currentPageViewState.mediaBuckets.first {
            it.bucketId == bucketId
        }.bucketName
        val supportsCapture = isDefaultBucket && defaultBucket.supportsCapture
        val bucketMediaItems = if (isDefaultBucket) {
            allMediaItems
        } else {
            allMediaItems.filter {
                it.bucketId == bucketId
            }
        }
        pageViewState = currentPageViewState.copy(
            selectedBucket = MatisseMediaBucket(
                bucketId = bucketId,
                bucketName = bucketName,
                supportsCapture = supportsCapture,
                mediaItems = bucketMediaItems
            )
        )
    }

    override fun onPreviewImagePageMediaCheckChanged(mediaItem: MatisseMediaItem) {
        onMediaCheckChanged(mediaItem = mediaItem)
    }

    private fun onMediaCheckChanged(mediaItem: MatisseMediaItem) {
        val selectionState = mediaItem.selectionState as MutableState<MatisseMediaSelectState>
        if (selectionState.value.isSelected) {
            selectionState.value = unselectedEnabledMediaSelectState
        } else {
            if (maxSelectable == 1) {
                resetAllMediaSelectState(state = unselectedEnabledMediaSelectState)
            } else {
                val selectedMediaItems = getSelectedMediaItems()
                if (selectedMediaItems.size >= maxSelectable) {
                    showToast(text = maxSelectionExceededMessage())
                    return
                } else if (singleMediaType) {
                    val wouldMixMediaTypes = selectedMediaItems.any {
                        it.mediaResource.isImage != mediaItem.mediaResource.isImage
                    }
                    if (wouldMixMediaTypes) {
                        showToast(id = R.string.matisse_error_mixed_media)
                        return
                    }
                }
            }
            selectionState.value = MatisseMediaSelectState(
                isSelected = true,
                isEnabled = true,
                positionIndex = getSelectedMediaItems().size
            )
        }
        updateSelectionOrder()
        updatePreviewImagePageIfNeeded()
        bottomBarViewState = buildBottomBarViewState()
    }

    private fun updateSelectionOrder() {
        val selectedMedia = getSelectedMediaItems()
        resetAllMediaSelectState(
            state = if (selectedMedia.size >= maxSelectable) {
                unselectedDisabledMediaSelectState
            } else {
                unselectedEnabledMediaSelectState
            }
        )
        selectedMedia.forEachIndexed { index, media ->
            val selectionState = media.selectionState as MutableState<MatisseMediaSelectState>
            selectionState.value = MatisseMediaSelectState(
                isSelected = true,
                isEnabled = true,
                positionIndex = index
            )
        }
    }

    private fun maxSelectionExceededMessage(): String {
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
        val selected = getSelectedMediaItems()
        return MatisseBottomBarViewState(
            selectedMediaCount = selected.size,
            maxSelectable = maxSelectable,
            isPreviewEnabled = selected.isNotEmpty(),
            onPreviewClick = ::onPreviewClick
        )
    }

    private fun onMediaClick(mediaItem: MatisseMediaItem) {
        val previewMediaItems = pageViewState.selectedBucket.mediaItems
        showPreviewImagePage(
            initialPage = previewMediaItems.indexOf(element = mediaItem),
            previewMediaItems = previewMediaItems,
            selectedMediaItems = getSelectedMediaItems()
        )
    }

    private fun onPreviewClick() {
        val selectedMediaItems = getSelectedMediaItems()
        showPreviewImagePage(
            initialPage = 0,
            previewMediaItems = selectedMediaItems,
            selectedMediaItems = selectedMediaItems
        )
    }

    fun getSelectedMedia(): List<MediaResource> {
        return getSelectedMediaItems().map {
            it.mediaResource
        }
    }

    override fun getSelectedMediaItems(): List<MatisseMediaItem> {
        return allMediaItems.filter {
            it.selectionState.value.isSelected
        }.sortedBy {
            it.selectionState.value.positionIndex
        }
    }

    private fun resetAllMediaSelectState(state: MatisseMediaSelectState) {
        allMediaItems.forEach {
            (it.selectionState as MutableState<MatisseMediaSelectState>).value = state
        }
    }

}