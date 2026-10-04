package github.leavesczy.matisse.internal.logic

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * 图库选择器唯一 ViewModel：网格分页、相册、选中、拍照结果与预览。
 *
 * 列表页与预览页共用 [mediaPagingDataFlow]，预览不持有独立数据源。
 */
internal class MatisseViewModel(
    application: Application,
    private val matisse: Matisse
) : BaseMatisseViewModel(application = application) {

    val maxSelectable = matisse.maxSelectable

    val mediaType = matisse.mediaType

    private val allowMixedMedia = matisse.allowMixedMedia

    val captureStrategy = matisse.captureStrategy

    private val defaultBucketId = DEFAULT_BUCKET_ID

    private val defaultBucket = MatisseSelectedBucket(
        bucketId = defaultBucketId,
        bucketName = "",
        supportsCapture = captureStrategy != null
    )

    private val selectedBucketIdFlow = MutableStateFlow(value = defaultBucketId)

    private val mediaReloadGenerationFlow = MutableStateFlow(value = 0)

    private val selectedMediaById = LinkedHashMap<Long, MatisseMediaItem>()

    private val selectionUiByMediaId = mutableStateMapOf<Long, MatisseMediaSelectState>()

    private val unselectedMediaSelectState = MatisseMediaSelectState(
        isSelected = false,
        positionIndex = -1
    )

    private var mediaBucketsLoaded = false

    private var mediaBucketsLoadJob: Job? = null

    private val readMediaPermissionGrantedFlow = MutableStateFlow<Boolean?>(value = null)

    private var nextSyntheticCapturedMediaId = -1L

    private var capturedMediaItems: List<MatisseMediaItem> = emptyList()

    private var nextPreviewPagerKey = 0L

    val isReadMediaPermissionInitialized: Boolean
        get() = readMediaPermissionGrantedFlow.value != null

    /** 列表页与预览页唯一的分页数据源；未获得读取权限前不查询 MediaStore。 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaPagingDataFlow: Flow<PagingData<MatisseMediaItem>> = combine(
        flow = selectedBucketIdFlow,
        flow2 = mediaReloadGenerationFlow,
        flow3 = readMediaPermissionGrantedFlow
    ) { bucketId, _, granted ->
        bucketId to (granted == true)
    }.flatMapLatest { (bucketId, granted) ->
        if (!granted) {
            return@flatMapLatest flowOf(value = PagingData.empty())
        }
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
        Pager(
            config = PagingConfig(
                pageSize = 40,
                initialLoadSize = 40,
                prefetchDistance = 30,
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

    private var selectionLimitReached by mutableStateOf(value = false)

    var pageViewState by mutableStateOf(
        value = MatissePageViewState(
            matisse = matisse,
            selectedBucket = defaultBucket,
            mediaBuckets = emptyList(),
            isMediaBucketsLoading = false,
            capturedMediaItems = emptyList(),
            placeholderState = MatissePlaceholderState.Pending,
            selectionStateFor = ::selectionStateFor,
            isSelectionLimitReached = ::isSelectionLimitReached,
            onBucketMenuOpen = ::onBucketMenuOpen,
            onBucketClick = ::onBucketClick,
            onMediaClick = ::onMediaClick,
            onToggleMediaSelection = ::onToggleMediaSelection
        )
    )
        private set

    var bottomBarViewState by mutableStateOf(
        value = buildBottomBarViewState()
    )
        private set

    var previewPageViewState by mutableStateOf(
        value = idlePreviewPageViewState()
    )
        private set

    private fun selectionStateFor(mediaId: Long): MatisseMediaSelectState {
        return selectionUiByMediaId[mediaId] ?: unselectedMediaSelectState
    }

    private fun isSelectionLimitReached(): Boolean {
        return selectionLimitReached
    }

    private fun getSelectedMediaItems(): List<MatisseMediaItem> {
        return selectedMediaById.values.toList()
    }

    fun onReadMediaPermissionResult(granted: Boolean) {
        if (readMediaPermissionGrantedFlow.value == granted) {
            return
        }
        cancelMediaBucketsLoad()
        dismissPreviewPage()
        selectedMediaById.clear()
        selectionUiByMediaId.clear()
        capturedMediaItems = emptyList()
        selectionLimitReached = false
        mediaBucketsLoaded = false
        selectedBucketIdFlow.value = defaultBucketId
        // 须在清空拍照项之后再更新权限，新分页源才不会沿用旧的 excludedMediaIds
        readMediaPermissionGrantedFlow.value = granted
        if (granted) {
            pageViewState = pageViewState.copy(
                selectedBucket = defaultBucket,
                mediaBuckets = emptyDefaultBuckets(),
                isMediaBucketsLoading = false,
                capturedMediaItems = emptyList(),
                placeholderState = MatissePlaceholderState.Granted
            )
            bottomBarViewState = buildBottomBarViewState()
        } else {
            setReadMediaPermissionDeniedState()
            bottomBarViewState = buildBottomBarViewState()
            showToast(id = R.string.matisse_error_read_media_permission)
        }
    }

    fun onMediaCaptured(mediaResource: MediaResource) {
        if (!isCapturedMediaAccepted(mediaResource = mediaResource)) {
            return
        }
        cancelMediaBucketsLoad()
        val mediaId = resolveCapturedMediaId(mediaResource = mediaResource)
        val capturedMediaItem = MatisseMediaItem(
            mediaId = mediaId,
            mediaResource = mediaResource
        )
        capturedMediaItems = buildList {
            add(element = capturedMediaItem)
            capturedMediaItems.forEach { existing ->
                if (existing.mediaId != mediaId) {
                    add(element = existing)
                }
            }
        }
        selectedBucketIdFlow.value = defaultBucketId
        mediaReloadGenerationFlow.value += 1
        mediaBucketsLoaded = false
        pageViewState = pageViewState.copy(
            selectedBucket = defaultBucket,
            capturedMediaItems = capturedMediaItems,
            isMediaBucketsLoading = false
        )
    }

    /**
     * 自定义 [github.leavesczy.matisse.CaptureStrategy] 可能返回任意 MIME；
     * 与当前 [mediaType] 不匹配时丢弃，避免绕过 [MediaType.MimeTypes] 等过滤。
     */
    private fun isCapturedMediaAccepted(mediaResource: MediaResource): Boolean {
        return when (val type = mediaType) {
            MediaType.ImageOnly -> mediaResource.isImage
            MediaType.VideoOnly -> mediaResource.isVideo
            MediaType.ImageAndVideo -> mediaResource.isImage || mediaResource.isVideo
            is MediaType.MimeTypes -> type.mimeTypes.contains(element = mediaResource.mimeType)
        }
    }

    /**
     * 仅采纳 MediaStore 权威下的非负 `_ID`；FileProvider 等其它 Uri（含末段为数字的情况）
     * 以及 [ContentUris.parseId] 得到 -1 时，一律使用递减的合成负数 id，避免与相册项冲突。
     */
    private fun resolveCapturedMediaId(mediaResource: MediaResource): Long {
        val uri = mediaResource.uri
        if (uri.authority == MediaStore.AUTHORITY) {
            try {
                val mediaId = ContentUris.parseId(uri)
                if (mediaId >= 0L) {
                    return mediaId
                }
            } catch (_: Throwable) {
            }
        }
        return nextSyntheticCapturedMediaId--
    }

    private fun onBucketMenuOpen() {
        if (mediaBucketsLoaded || pageViewState.isMediaBucketsLoading) {
            return
        }
        if (pageViewState.placeholderState !is MatissePlaceholderState.Granted) {
            return
        }
        loadMediaBucketsAsync()
    }

    private fun cancelMediaBucketsLoad() {
        mediaBucketsLoadJob?.cancel()
        mediaBucketsLoadJob = null
    }

    private fun loadMediaBucketsAsync() {
        cancelMediaBucketsLoad()
        pageViewState = pageViewState.copy(isMediaBucketsLoading = true)
        mediaBucketsLoadJob = viewModelScope.launch {
            val mediaBucketsResult = MediaProvider.loadMediaBuckets(
                context = context,
                mediaType = mediaType
            )
            ensureActive()
            val bucketAggregates = mediaBucketsResult.buckets
            // 「全部」封面取全库 recency 第一项，与「全部」网格中 MediaStore 首项一致
            val newestMedia = MediaProvider.loadMediaInfoPage(
                context = context,
                mediaType = mediaType,
                bucketId = null,
                limit = 1
            ).firstOrNull()
            ensureActive()
            // MediaStore 拍照项已包含在总数中（分页里被排除、以前缀展示）；
            // FileProvider 拍照项不在 MediaStore 中，使用合成的负数 id，需要另外计入
            val nonMediaStoreCapturedCount = capturedMediaItems.count { it.mediaId < 0L }
            val totalItemCount = mediaBucketsResult.totalItemCount + nonMediaStoreCapturedCount
            val mediaBuckets = buildList {
                add(
                    element = MatisseBucketListItem(
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
                        MatisseBucketListItem(
                            bucketId = aggregate.bucketId,
                            bucketName = aggregate.bucketName,
                            itemCount = aggregate.itemCount,
                            coverMedia = aggregate.toCoverMediaResource()
                        )
                    }
                )
            }
            if (pageViewState.placeholderState is MatissePlaceholderState.Granted) {
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
        return MatisseMediaItem(
            mediaId = mediaInfo.mediaId,
            mediaResource = MediaResource(
                uri = mediaInfo.uri,
                mimeType = mediaInfo.mimeType
            )
        )
    }

    private fun setReadMediaPermissionDeniedState() {
        cancelMediaBucketsLoad()
        selectedBucketIdFlow.value = defaultBucketId
        mediaBucketsLoaded = false
        capturedMediaItems = emptyList()
        pageViewState = pageViewState.copy(
            selectedBucket = defaultBucket,
            mediaBuckets = emptyDefaultBuckets(),
            isMediaBucketsLoading = false,
            capturedMediaItems = emptyList(),
            placeholderState = MatissePlaceholderState.NoPermission
        )
    }

    private fun emptyDefaultBuckets(): List<MatisseBucketListItem> {
        return listOf(
            element = MatisseBucketListItem(
                bucketId = defaultBucket.bucketId,
                bucketName = defaultBucket.bucketName,
                itemCount = 0,
                coverMedia = null
            )
        )
    }

    private fun onBucketClick(bucketId: String) {
        val currentPageViewState = pageViewState
        if (currentPageViewState.selectedBucket.bucketId == bucketId) {
            return
        }
        val isDefaultBucket = bucketId == defaultBucketId
        val bucketName = currentPageViewState.mediaBuckets
            .firstOrNull { it.bucketId == bucketId }
            ?.bucketName
            ?: return
        val supportsCapture = isDefaultBucket && defaultBucket.supportsCapture
        selectedBucketIdFlow.value = bucketId
        pageViewState = currentPageViewState.copy(
            selectedBucket = MatisseSelectedBucket(
                bucketId = bucketId,
                bucketName = bucketName,
                supportsCapture = supportsCapture
            )
        )
    }

    private fun onToggleMediaSelection(mediaItem: MatisseMediaItem) {
        if (selectedMediaById.containsKey(key = mediaItem.mediaId)) {
            selectedMediaById.remove(key = mediaItem.mediaId)
        } else {
            if (maxSelectable == 1) {
                clearSelectedMedia()
            } else {
                if (selectedMediaById.size >= maxSelectable) {
                    showToast(text = maxSelectionExceededMessage())
                    return
                } else if (!allowMixedMedia) {
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
        }
        syncSelectionUiState()
        updatePreviewPageIfNeeded()
        bottomBarViewState = buildBottomBarViewState()
    }

    private fun syncSelectionUiState() {
        val selectedIds = selectedMediaById.keys.toSet()
        selectionUiByMediaId.keys
            .filter { mediaId -> !selectedIds.contains(element = mediaId) }
            .forEach { mediaId ->
                selectionUiByMediaId.remove(key = mediaId)
            }
        selectedMediaById.keys.forEachIndexed { index, mediaId ->
            val newState = MatisseMediaSelectState(
                isSelected = true,
                positionIndex = index
            )
            if (selectionUiByMediaId[mediaId] != newState) {
                selectionUiByMediaId[mediaId] = newState
            }
        }
        // 单选时点击其它项会直接替换，不应把未选项展示为禁用态
        selectionLimitReached = maxSelectable > 1 && selectedMediaById.size >= maxSelectable
    }

    private fun maxSelectionExceededMessage(): String {
        val includesImage = mediaType.includesImage
        val includesVideo = mediaType.includesVideo
        val pluralsId = if (includesImage && !includesVideo) {
            R.plurals.matisse_error_max_images
        } else if (!includesImage && includesVideo) {
            R.plurals.matisse_error_max_videos
        } else {
            R.plurals.matisse_error_max_media
        }
        return getQuantityString(
            id = pluralsId,
            quantity = maxSelectable,
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

    private fun idlePreviewPageViewState(): MatissePreviewPageViewState {
        return MatissePreviewPageViewState(
            isVisible = false,
            initialPage = 0,
            selectedMediaCount = 0,
            maxSelectable = maxSelectable,
            source = MatissePreviewSource.Selected(mediaItems = emptyList()),
            previewPagerKey = nextPreviewPagerKey,
            selectionStateFor = { unselectedMediaSelectState },
            isSelectionLimitReached = { false },
            onToggleMediaSelection = {},
            onDismissRequest = {},
            onExitFinished = ::onPreviewExitFinished
        )
    }

    private fun showPreviewPage(
        source: MatissePreviewSource,
        initialPage: Int
    ) {
        nextPreviewPagerKey += 1L
        previewPageViewState = MatissePreviewPageViewState(
            isVisible = true,
            maxSelectable = maxSelectable,
            initialPage = initialPage,
            selectedMediaCount = selectedMediaById.size,
            source = source,
            previewPagerKey = nextPreviewPagerKey,
            selectionStateFor = ::selectionStateFor,
            isSelectionLimitReached = ::isSelectionLimitReached,
            onToggleMediaSelection = ::onToggleMediaSelection,
            onDismissRequest = ::dismissPreviewPage,
            onExitFinished = ::onPreviewExitFinished
        )
    }

    private fun dismissPreviewPage() {
        val currentPreviewPageViewState = previewPageViewState
        if (currentPreviewPageViewState.isVisible) {
            // 退出动画期间保留数据来源，动画结束后再重置，避免内容闪烁
            previewPageViewState = currentPreviewPageViewState.copy(isVisible = false)
        }
    }

    private fun onPreviewExitFinished() {
        if (!previewPageViewState.isVisible) {
            previewPageViewState = idlePreviewPageViewState()
        }
    }

    private fun updatePreviewPageIfNeeded() {
        val currentPreviewPageViewState = previewPageViewState
        if (currentPreviewPageViewState.isVisible) {
            previewPageViewState = currentPreviewPageViewState.copy(
                selectedMediaCount = selectedMediaById.size
            )
        }
    }

    private fun onMediaClick(galleryIndex: Int) {
        showPreviewPage(
            source = MatissePreviewSource.Gallery,
            initialPage = galleryIndex.coerceAtLeast(minimumValue = 0)
        )
    }

    private fun onPreviewClick() {
        val selectedMediaItems = getSelectedMediaItems()
        if (selectedMediaItems.isEmpty()) {
            return
        }
        showPreviewPage(
            source = MatissePreviewSource.Selected(mediaItems = selectedMediaItems),
            initialPage = 0
        )
    }

    fun getSelectedMedia(): List<MediaResource> {
        return getSelectedMediaItems().map {
            it.mediaResource
        }
    }

    private fun clearSelectedMedia() {
        selectedMediaById.clear()
        selectionUiByMediaId.clear()
    }

}
