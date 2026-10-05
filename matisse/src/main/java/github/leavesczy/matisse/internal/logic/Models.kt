package github.leavesczy.matisse.internal.logic

import androidx.compose.runtime.Stable
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource

@Stable
internal data class MatissePageViewState(
    val matisse: Matisse,
    val selectedBucket: MatisseSelectedBucket,
    val mediaBuckets: List<MatisseBucketListItem>,
    val isMediaBucketsLoading: Boolean,
    val capturedMediaItems: List<MatisseMediaItem>,
    val placeholderState: MatissePlaceholderState,
    val selectionStateFor: (mediaId: Long) -> MatisseMediaSelectState,
    val isSelectionLimitReached: () -> Boolean,
    val onBucketMenuOpen: () -> Unit,
    val onBucketClick: (bucketId: String) -> Unit,
    /** [galleryIndex] 为拍照项与分页项拼接后的下标，与预览页共用同一套下标。 */
    val onMediaClick: (galleryIndex: Int) -> Unit,
    val onToggleMediaSelection: (mediaItem: MatisseMediaItem) -> Unit
) {

    /** 当前相册下实际展示在分页项之前的拍照项。 */
    val visibleCapturedMediaItems: List<MatisseMediaItem>
        get() = if (selectedBucket.supportsCapture) {
            capturedMediaItems
        } else {
            emptyList()
        }

}

/** 「全部」相册的 id；其名称由 UI 按当前语言从资源读取，不在状态中缓存。 */
internal const val DEFAULT_BUCKET_ID = "&__matisseDefaultBucketId__&"

@Stable
internal data class MatisseMediaItem(
    val mediaId: Long,
    val mediaResource: MediaResource
)

@Stable
internal data class MatisseMediaSelectState(
    val isSelected: Boolean,
    val positionIndex: Int
) {

    val positionFormatted = if (positionIndex >= 0) {
        (positionIndex + 1).toString()
    } else {
        null
    }

}

/** [bucketName] 对 [DEFAULT_BUCKET_ID] 为空，展示名由 UI 解析。 */
@Stable
internal data class MatisseSelectedBucket(
    val bucketId: String,
    val bucketName: String,
    val supportsCapture: Boolean
)

/** [bucketName] 对 [DEFAULT_BUCKET_ID] 为空，展示名由 UI 解析。 */
@Stable
internal data class MatisseBucketListItem(
    val bucketId: String,
    val bucketName: String,
    val itemCount: Int,
    val coverMedia: MediaResource?
)

@Stable
internal data class MatisseBottomBarViewState(
    val selectedMediaCount: Int,
    val maxSelectable: Int,
    val isPreviewEnabled: Boolean,
    val onPreviewClick: () -> Unit
)

@Stable
internal sealed interface MatissePreviewSource {

    /** 与列表页共用同一份分页数据：预览中触发的分页加载会同时出现在列表页。 */
    @Stable
    data object Gallery : MatissePreviewSource

    /** 底栏「预览」：打开时已选项的快照。 */
    @Stable
    data class Selected(val mediaItems: List<MatisseMediaItem>) : MatissePreviewSource

}

@Stable
internal data class MatissePreviewPageViewState(
    val isVisible: Boolean,
    val maxSelectable: Int,
    val initialPage: Int,
    val selectedMediaCount: Int,
    val source: MatissePreviewSource,
    /** 每次打开预览递增，用于在退出动画未结束时再次打开也能重置 HorizontalPager。 */
    val previewPagerKey: Long,
    val selectionStateFor: (mediaId: Long) -> MatisseMediaSelectState,
    val isSelectionLimitReached: () -> Boolean,
    val onToggleMediaSelection: (mediaItem: MatisseMediaItem) -> Unit,
    val onDismissRequest: () -> Unit,
    val onExitFinished: () -> Unit
)

/**
 * 列表页占位与网格是否可展示，由读取权限结果驱动，与相册是否为空无关。
 */
@Stable
internal sealed interface MatissePlaceholderState {

    @Stable
    data object Pending : MatissePlaceholderState

    /** 已授权；相册为空时由 UI 另画空态。 */
    @Stable
    data object Granted : MatissePlaceholderState

    @Stable
    data object NoPermission : MatissePlaceholderState

}
