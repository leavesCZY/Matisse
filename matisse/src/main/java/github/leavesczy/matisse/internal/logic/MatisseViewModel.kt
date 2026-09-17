package github.leavesczy.matisse.internal.logic

import android.app.Application
import android.content.ContentUris
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

internal class MatisseViewModel(application: Application, matisse: Matisse) :
    MatissePreviewViewModel(application = application, matisse = matisse) {

    val maxSelectable = matisse.maxSelectable

    val mediaType = matisse.mediaType

    val singleMediaType = matisse.singleMediaType

    val captureStrategy = matisse.captureStrategy

    private val defaultBucketId = "&__matisseDefaultBucketId__&"

    private val defaultBucket = MatisseMediaBucket(
        bucketId = defaultBucketId,
        bucketName = getString(id = R.string.matisse_bucket_all),
        supportsCapture = captureStrategy != null
    )

    private val selectedBucketIdFlow = MutableStateFlow(value = defaultBucketId)

    private val mediaReloadGenerationFlow = MutableStateFlow(value = 0)

    private val selectedMediaById = LinkedHashMap<Long, MatisseMediaItem>()

    private val selectionStateByMediaId =
        ConcurrentHashMap<Long, MutableState<MatisseMediaSelectState>>()

    private val unselectedMediaSelectState = MatisseMediaSelectState(
        isSelected = false,
        positionIndex = -1
    )

    private var mediaBucketsLoaded = false

    private var readMediaPermissionGranted: Boolean? = null

    private var nextSyntheticCapturedMediaId = -1L

    private var capturedMediaItems: List<MatisseMediaItem> = emptyList()

    val isReadMediaPermissionInitialized: Boolean
        get() = readMediaPermissionGranted != null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mediaPagingDataFlow = combine(
        flow = selectedBucketIdFlow,
        flow2 = mediaReloadGenerationFlow
    ) { bucketId, _ ->
        bucketId
    }.flatMapLatest { bucketId ->
        val queryBucketId = if (bucketId == defaultBucketId) {
            null
        } else {
            bucketId
        }
        val excludedMediaIds = if (bucketId == defaultBucketId) {
            capturedMediaItems.mapTo(destination = HashSet()) { it.mediaId }
        } else {
            emptySet()
        }
        val mediaPageSize = 40
        Pager(
            config = PagingConfig(
                pageSize = mediaPageSize,
                initialLoadSize = mediaPageSize,
                prefetchDistance = mediaPageSize,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                MediaPagingSource(
                    context = context,
                    mediaType = mediaType,
                    bucketId = queryBucketId,
                    excludedMediaIds = excludedMediaIds,
                    createMediaItem = ::createMediaItem
                )
            }
        ).flow
    }.cachedIn(scope = viewModelScope)

    var isSelectionLimitReached by mutableStateOf(value = false)
        private set

    var pageViewState by mutableStateOf(
        value = MatissePageViewState(
            matisse = matisse,
            selectedBucket = defaultBucket,
            mediaBuckets = emptyList(),
            isMediaBucketsLoading = false,
            capturedMediaItems = emptyList(),
            mediaPagingDataFlow = mediaPagingDataFlow,
            placeholderState = MatissePlaceholderState.Ready(hasReadMediaPermission = false),
            onBucketMenuOpen = ::onBucketMenuOpen,
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

    fun onReadMediaPermissionResult(granted: Boolean) {
        if (readMediaPermissionGranted == granted) {
            return
        }
        readMediaPermissionGranted = granted
        viewModelScope.launch {
            dismissPreviewPage()
            dismissVideoPlayerPage()
            selectedMediaById.clear()
            selectionStateByMediaId.clear()
            capturedMediaItems = emptyList()
            isSelectionLimitReached = false
            mediaBucketsLoaded = false
            if (granted) {
                selectedBucketIdFlow.value = defaultBucketId
                mediaReloadGenerationFlow.value += 1
                pageViewState = pageViewState.copy(
                    selectedBucket = defaultBucket,
                    mediaBuckets = listOf(
                        element = MatisseMediaBucketInfo(
                            bucketId = defaultBucket.bucketId,
                            bucketName = defaultBucket.bucketName,
                            itemCount = 0,
                            coverMedia = null
                        )
                    ),
                    isMediaBucketsLoading = false,
                    capturedMediaItems = emptyList(),
                    placeholderState = MatissePlaceholderState.Ready(hasReadMediaPermission = true)
                )
                bottomBarViewState = buildBottomBarViewState()
            } else {
                setReadMediaPermissionDeniedState()
                bottomBarViewState = buildBottomBarViewState()
                showToast(id = R.string.matisse_error_read_media_permission)
            }
        }
    }

    fun onMediaCaptured(mediaResource: MediaResource) {
        val mediaId = resolveCapturedMediaId(mediaResource = mediaResource)
        val selectionState = selectionStateByMediaId.getOrPut(key = mediaId) {
            mutableStateOf(value = unselectedMediaSelectState)
        }
        if (!selectionState.value.isSelected) {
            selectionState.value = unselectedMediaSelectState
        }
        val capturedMediaItem = MatisseMediaItem(
            mediaId = mediaId,
            bucketId = defaultBucketId,
            bucketName = defaultBucket.bucketName,
            mediaResource = mediaResource,
            selectionState = selectionState
        )
        capturedMediaItems = buildList {
            add(element = capturedMediaItem)
            capturedMediaItems.forEach { existing ->
                if (existing.mediaId != mediaId) {
                    add(element = existing)
                }
            }
        }
        pruneUnselectedSelectionStates()
        selectedBucketIdFlow.value = defaultBucketId
        mediaReloadGenerationFlow.value += 1
        mediaBucketsLoaded = false
        pageViewState = pageViewState.copy(
            selectedBucket = defaultBucket,
            capturedMediaItems = capturedMediaItems,
            isMediaBucketsLoading = false
        )
    }

    private fun resolveCapturedMediaId(mediaResource: MediaResource): Long {
        return try {
            ContentUris.parseId(mediaResource.uri)
        } catch (_: Throwable) {
            nextSyntheticCapturedMediaId--
        }
    }

    private fun onBucketMenuOpen() {
        if (mediaBucketsLoaded || pageViewState.isMediaBucketsLoading) {
            return
        }
        if (pageViewState.placeholderState !is MatissePlaceholderState.Ready) {
            return
        }
        loadMediaBucketsAsync()
    }

    private fun loadMediaBucketsAsync() {
        pageViewState = pageViewState.copy(isMediaBucketsLoading = true)
        viewModelScope.launch {
            val bucketAggregates = MediaProvider.loadMediaBuckets(
                context = context,
                mediaType = mediaType
            )
            val newestMedia = MediaProvider.loadMediaInfoPage(
                context = context,
                mediaType = mediaType,
                bucketId = null,
                limit = 1,
                offset = 0
            ).firstOrNull()
            val totalItemCount = bucketAggregates.sumOf { it.itemCount }
            val mediaBuckets = buildList {
                add(
                    element = MatisseMediaBucketInfo(
                        bucketId = defaultBucket.bucketId,
                        bucketName = defaultBucket.bucketName,
                        itemCount = totalItemCount,
                        coverMedia = newestMedia?.let { mediaInfo ->
                            MediaResource(
                                uri = mediaInfo.uri,
                                mimeType = mediaInfo.mimeType
                            )
                        }
                    )
                )
                addAll(
                    elements = bucketAggregates.map { aggregate ->
                        MatisseMediaBucketInfo(
                            bucketId = aggregate.bucketId,
                            bucketName = aggregate.bucketName,
                            itemCount = aggregate.itemCount,
                            coverMedia = aggregate.toCoverMediaResource()
                        )
                    }
                )
            }
            if (pageViewState.placeholderState is MatissePlaceholderState.Ready) {
                mediaBucketsLoaded = true
                pageViewState = pageViewState.copy(
                    mediaBuckets = mediaBuckets,
                    isMediaBucketsLoading = false
                )
            } else {
                pageViewState = pageViewState.copy(isMediaBucketsLoading = false)
            }
        }
    }

    private fun createMediaItem(mediaInfo: MediaProvider.MediaInfo): MatisseMediaItem {
        val mediaResource = MediaResource(
            uri = mediaInfo.uri,
            mimeType = mediaInfo.mimeType
        )
        val selectionState = selectionStateByMediaId.getOrPut(key = mediaInfo.mediaId) {
            mutableStateOf(value = unselectedMediaSelectState)
        }
        return MatisseMediaItem(
            mediaId = mediaInfo.mediaId,
            bucketId = mediaInfo.bucketId,
            bucketName = mediaInfo.bucketName,
            mediaResource = mediaResource,
            selectionState = selectionState
        )
    }

    private fun setReadMediaPermissionDeniedState() {
        selectedBucketIdFlow.value = defaultBucketId
        mediaBucketsLoaded = false
        capturedMediaItems = emptyList()
        pageViewState = pageViewState.copy(
            selectedBucket = defaultBucket,
            mediaBuckets = listOf(
                element = MatisseMediaBucketInfo(
                    bucketId = defaultBucket.bucketId,
                    bucketName = defaultBucket.bucketName,
                    itemCount = 0,
                    coverMedia = null
                )
            ),
            isMediaBucketsLoading = false,
            capturedMediaItems = emptyList(),
            placeholderState = MatissePlaceholderState.NoPermission
        )
    }

    private fun onBucketClick(bucketId: String) {
        val currentPageViewState = pageViewState
        if (currentPageViewState.selectedBucket.bucketId == bucketId) {
            return
        }
        val isDefaultBucket = bucketId == defaultBucketId
        val bucketName = currentPageViewState.mediaBuckets.first {
            it.bucketId == bucketId
        }.bucketName
        val supportsCapture = isDefaultBucket && defaultBucket.supportsCapture
        pruneUnselectedSelectionStates()
        selectedBucketIdFlow.value = bucketId
        pageViewState = currentPageViewState.copy(
            selectedBucket = MatisseMediaBucket(
                bucketId = bucketId,
                bucketName = bucketName,
                supportsCapture = supportsCapture
            )
        )
    }

    private fun pruneUnselectedSelectionStates() {
        val retainedIds = buildSet {
            addAll(elements = selectedMediaById.keys)
            capturedMediaItems.forEach { add(element = it.mediaId) }
        }
        val iterator = selectionStateByMediaId.keys.iterator()
        while (iterator.hasNext()) {
            if (!retainedIds.contains(element = iterator.next())) {
                iterator.remove()
            }
        }
    }

    override fun onPreviewPageMediaCheckChanged(mediaItem: MatisseMediaItem) {
        onMediaCheckChanged(mediaItem = mediaItem)
    }

    private fun onMediaCheckChanged(mediaItem: MatisseMediaItem) {
        val selectionState = mediaItem.selectionState as MutableState<MatisseMediaSelectState>
        if (selectionState.value.isSelected) {
            selectedMediaById.remove(key = mediaItem.mediaId)
            selectionState.value = unselectedMediaSelectState
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
                positionIndex = selectedMediaById.size - 1
            )
        }
        updateSelectionOrder()
        updatePreviewPageIfNeeded()
        bottomBarViewState = buildBottomBarViewState()
    }

    private fun updateSelectionOrder() {
        selectedMediaById.values.forEachIndexed { index, media ->
            val selectionState = media.selectionState as MutableState<MatisseMediaSelectState>
            val newState = MatisseMediaSelectState(
                isSelected = true,
                positionIndex = index
            )
            if (selectionState.value != newState) {
                selectionState.value = newState
            }
        }
        isSelectionLimitReached = selectedMediaById.size >= maxSelectable
    }

    private fun maxSelectionExceededMessage(): String {
        val includesImage = mediaType.includesImage
        val includesVideo = mediaType.includesVideo
        val stringId = if (includesImage && !includesVideo) {
            R.string.matisse_error_max_images
        } else if (!includesImage && includesVideo) {
            R.string.matisse_error_max_videos
        } else {
            R.string.matisse_error_max_media
        }
        return getString(
            id = stringId,
            formatArgs = arrayOf(maxSelectable)
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

    private fun onMediaClick(
        mediaItem: MatisseMediaItem,
        previewMediaItems: List<MatisseMediaItem>
    ) {
        val initialPage = previewMediaItems.indexOfFirst {
            it.mediaId == mediaItem.mediaId
        }.coerceAtLeast(minimumValue = 0)
        showPreviewPage(
            initialPage = initialPage,
            previewMediaItems = previewMediaItems,
            selectedMediaItems = getSelectedMediaItems()
        )
    }

    private fun onPreviewClick() {
        val selectedMediaItems = getSelectedMediaItems()
        showPreviewPage(
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
            selectionState.value = unselectedMediaSelectState
        }
        selectedMediaById.clear()
    }

}
