package github.leavesczy.matisse.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.paging.compose.LazyPagingItems
import github.leavesczy.matisse.internal.logic.MatisseMediaItem

/**
 * 列表页与预览页共用的唯一数据源：拍照项前缀 + 同一个 [LazyPagingItems]。
 *
 * 下标 `[0, capturedCount)` 为拍照项，其后为分页项。通过 [get] 读取分页项会向 Paging
 * 上报访问位置并触发预加载，因此无论在列表页还是预览页滑动，加载结果都会同时反映到两边。
 */
@Stable
internal class MatisseGalleryItems(
    val capturedMediaItems: List<MatisseMediaItem>,
    val pagingItems: LazyPagingItems<MatisseMediaItem>
) {

    val capturedCount: Int
        get() = capturedMediaItems.size

    val itemCount: Int
        get() = capturedMediaItems.size + pagingItems.itemCount

    operator fun get(index: Int): MatisseMediaItem? {
        return if (index < capturedCount) {
            capturedMediaItems.getOrNull(index = index)
        } else {
            val pagingIndex = index - capturedCount
            if (pagingIndex < pagingItems.itemCount) {
                pagingItems[pagingIndex]
            } else {
                null
            }
        }
    }

    /**
     * 拍照项与分页项分段加前缀，避免同一 mediaId 同时出现在两段时 key 冲突。
     */
    fun keyAt(index: Int): Any {
        return if (index < capturedCount) {
            "captured_${capturedMediaItems[index].mediaId}"
        } else {
            val mediaId = peek(index = index)?.mediaId
            if (mediaId != null) {
                "media_$mediaId"
            } else {
                "placeholder_$index"
            }
        }
    }

    /** 不触发分页加载。预览组合与 key 计算走这里，避免在组合中调用 [get] 打断该页。 */
    fun peek(index: Int): MatisseMediaItem? {
        return if (index < capturedCount) {
            capturedMediaItems.getOrNull(index = index)
        } else {
            val pagingIndex = index - capturedCount
            if (pagingIndex < pagingItems.itemCount) {
                pagingItems.peek(index = pagingIndex)
            } else {
                null
            }
        }
    }

}

@Composable
internal fun rememberMatisseGalleryItems(
    capturedMediaItems: List<MatisseMediaItem>,
    pagingItems: LazyPagingItems<MatisseMediaItem>
): MatisseGalleryItems {
    return remember(key1 = capturedMediaItems, key2 = pagingItems) {
        MatisseGalleryItems(
            capturedMediaItems = capturedMediaItems,
            pagingItems = pagingItems
        )
    }
}
