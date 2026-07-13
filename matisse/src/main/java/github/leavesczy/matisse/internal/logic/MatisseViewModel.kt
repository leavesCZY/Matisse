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

    private val selectedMediaById = LinkedHashMap<Long, MatisseMediaItem>()

    var selectionLimitReached by mutableStateOf(value = false)
        private set

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
            selectedMediaById.clear()
            selectionLimitReached = false
            if (granted) {
                val loadResult = prepareMediaPage()
                allMediaItems.addAll(elements = loadResult.mediaItems)
                selectedMediaById.putAll(from = loadResult.selectedMediaById)
                selectionLimitReached = loadResult.selectionLimitReached
                pageViewState = pageViewState.copy(
                    selectedBucket = loadResult.mainMediaBucket,
                    mediaBuckets = loadResult.mediaBuckets,
                    placeholderState = loadResult.placeholderState
                )
            } else {
                setReadMediaPermissionDeniedState()
                showToast(id = R.string.matisse_error_read_media_permission)
            }
            bottomBarViewState = buildBottomBarViewState()
            dismissLoadingDialog()
        }
    }

    private suspend fun prepareMediaPage(): MediaPageLoadResult {
        return withContext(context = Dispatchers.Default) {
            val loadedMediaItems = loadMediaItems()
            val selectedMediaById = applyDefaultMediaSelection(loadedMediaItems = loadedMediaItems)
            val mainMediaBucket = defaultBucket.copy(mediaItems = loadedMediaItems)
            val mediaBuckets = buildList {
                add(
                    element = MatisseMediaBucketInfo(
                        bucketId = mainMediaBucket.bucketId,
                        bucketName = mainMediaBucket.bucketName,
                        itemCount = mainMediaBucket.mediaItems.size,
                        coverMedia = mainMediaBucket.mediaItems.firstOrNull()?.mediaResource
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
            val placeholderState = if (loadedMediaItems.isEmpty()) {
                MatissePlaceholderState.NoMedia(
                    includesImages = mediaType.includeImage,
                    includesVideos = mediaType.includeVideo
                )
            } else {
                MatissePlaceholderState.Ready(hasReadMediaPermission = true)
            }
            MediaPageLoadResult(
                mediaItems = loadedMediaItems,
                selectedMediaById = selectedMediaById,
                selectionLimitReached = selectedMediaById.size >= maxSelectable,
                mainMediaBucket = mainMediaBucket,
                mediaBuckets = mediaBuckets,
                placeholderState = placeholderState
            )
        }
    }

    private suspend fun applyDefaultMediaSelection(
        loadedMediaItems: List<MatisseMediaItem>
    ): LinkedHashMap<Long, MatisseMediaItem> {
        val selectedMediaById = LinkedHashMap<Long, MatisseMediaItem>()
        if (mediaFilter == null || fastSelect) {
            return selectedMediaById
        }
        var positionIndex = 0
        for (media in loadedMediaItems) {
            if (!mediaFilter.selectMedia(mediaResource = media.mediaResource)) {
                continue
            }
            val selectionState = media.selectionState as MutableState<MatisseMediaSelectState>
            selectionState.value = MatisseMediaSelectState(
                isSelected = true,
                isEnabled = true,
                positionIndex = positionIndex
            )
            selectedMediaById[media.mediaId] = media
            positionIndex++
        }
        return selectedMediaById
    }

    private suspend fun loadMediaItems(): List<MatisseMediaItem> {
        val mediaInfoList = MediaProvider.loadMediaInfoList(
            context = context,
            mediaType = mediaType
        )
        if (mediaInfoList.isNullOrEmpty()) {
            return emptyList()
        }
        return mediaInfoList.mapNotNull {
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
            selectedMediaById.remove(key = mediaItem.mediaId)
            selectionState.value = unselectedEnabledMediaSelectState
        } else {
            if (maxSelectable == 1) {
                clearSelectedMedia()
            } else {
                if (selectedMediaById.size >= maxSelectable) {
                    showToast(text = maxSelectionExceededMessage())
                    return
                } else if (singleMediaType) {
                    val wouldMixMediaTypes = selectedMediaById.values.any {
                        it.mediaResource.isImage != mediaItem.mediaResource.isImage
                    }
                    if (wouldMixMediaTypes) {
                        showToast(id = R.string.matisse_error_mixed_media)
                        return
                    }
                }
            }
            selectedMediaById[mediaItem.mediaId] = mediaItem
            selectionState.value = MatisseMediaSelectState(
                isSelected = true,
                isEnabled = true,
                positionIndex = selectedMediaById.size - 1
            )
        }
        updateSelectionOrder()
        updatePreviewImagePageIfNeeded()
        bottomBarViewState = buildBottomBarViewState()
    }

    private fun updateSelectionOrder() {
        selectedMediaById.values.forEachIndexed { index, media ->
            val selectionState = media.selectionState as MutableState<MatisseMediaSelectState>
            val newState = MatisseMediaSelectState(
                isSelected = true,
                isEnabled = true,
                positionIndex = index
            )
            if (selectionState.value != newState) {
                selectionState.value = newState
            }
        }
        selectionLimitReached = selectedMediaById.size >= maxSelectable
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
        val selectedMediaCount = selectedMediaById.size
        return MatisseBottomBarViewState(
            selectedMediaCount = selectedMediaCount,
            maxSelectable = maxSelectable,
            isPreviewEnabled = selectedMediaCount > 0,
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
        return selectedMediaById.values.toList()
    }

    private fun clearSelectedMedia() {
        selectedMediaById.values.forEach { media ->
            val selectionState = media.selectionState as MutableState<MatisseMediaSelectState>
            selectionState.value = unselectedEnabledMediaSelectState
        }
        selectedMediaById.clear()
    }

    private data class MediaPageLoadResult(
        val mediaItems: List<MatisseMediaItem>,
        val selectedMediaById: LinkedHashMap<Long, MatisseMediaItem>,
        val selectionLimitReached: Boolean,
        val mainMediaBucket: MatisseMediaBucket,
        val mediaBuckets: List<MatisseMediaBucketInfo>,
        val placeholderState: MatissePlaceholderState
    )

}