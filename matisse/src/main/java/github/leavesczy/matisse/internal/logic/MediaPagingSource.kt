package github.leavesczy.matisse.internal.logic

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import github.leavesczy.matisse.MediaType
import kotlin.coroutines.cancellation.CancellationException

/**
 * 基于 MediaStore OFFSET 的正向分页。
 *
 * 仅支持 append：`prevKey` 恒为 null，避免在排除项导致 query offset 与页大小不一致时
 * 错误计算 prepend 起点。刷新时 [getRefreshKey] 返回 null，从 offset 0 重新加载。
 */
internal class MediaPagingSource(
    private val context: Context,
    private val mediaType: MediaType,
    private val bucketId: String?,
    private val excludedMediaIds: Set<Long>,
    private val createMediaItem: (mediaInfo: MediaProvider.MediaInfo) -> MatisseMediaItem
) : PagingSource<Int, MatisseMediaItem>() {

    private val loadedMediaIds = HashSet<Long>()

    override fun getRefreshKey(state: PagingState<Int, MatisseMediaItem>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MatisseMediaItem> {
        return try {
            if (params is LoadParams.Refresh) {
                loadedMediaIds.clear()
            }
            val startOffset = params.key ?: 0
            val pageSize = params.loadSize
            val mediaItems = ArrayList<MatisseMediaItem>(pageSize)
            var queryOffset = startOffset
            var reachedEnd = false
            var previousEmptyBatchFirstMediaId: Long? = null
            while (mediaItems.size < pageSize && !reachedEnd) {
                val mediaInfoList = MediaProvider.loadMediaInfoPage(
                    context = context,
                    mediaType = mediaType,
                    bucketId = bucketId,
                    limit = pageSize,
                    offset = queryOffset
                )
                if (mediaInfoList.isEmpty()) {
                    reachedEnd = true
                    break
                }
                val batchFirstMediaId = mediaInfoList[0].mediaId
                var acceptedInBatch = 0
                for (mediaInfo in mediaInfoList) {
                    queryOffset += 1
                    if (excludedMediaIds.contains(element = mediaInfo.mediaId)) {
                        continue
                    }
                    if (!loadedMediaIds.add(element = mediaInfo.mediaId)) {
                        continue
                    }
                    mediaItems.add(element = createMediaItem(mediaInfo))
                    acceptedInBatch += 1
                    if (mediaItems.size >= pageSize) {
                        break
                    }
                }
                when {
                    mediaInfoList.size < pageSize -> {
                        reachedEnd = true
                    }

                    acceptedInBatch == 0 && batchFirstMediaId == previousEmptyBatchFirstMediaId -> {
                        // OFFSET 未推进时会反复返回同一批已跳过的数据，避免死循环
                        reachedEnd = true
                    }

                    acceptedInBatch == 0 -> {
                        previousEmptyBatchFirstMediaId = batchFirstMediaId
                    }

                    else -> {
                        previousEmptyBatchFirstMediaId = null
                    }
                }
            }
            LoadResult.Page(
                data = mediaItems,
                prevKey = null,
                nextKey = if (reachedEnd) {
                    null
                } else {
                    queryOffset
                }
            )
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            LoadResult.Error(throwable = throwable)
        }
    }

}
