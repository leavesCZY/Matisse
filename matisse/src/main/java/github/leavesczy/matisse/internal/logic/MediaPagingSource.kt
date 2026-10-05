package github.leavesczy.matisse.internal.logic

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import github.leavesczy.matisse.MediaType
import kotlin.coroutines.cancellation.CancellationException

/**
 * 基于 MediaStore 排序位置（keyset）的正向分页，key 为上一页最后消费的一条媒体的排序位置。
 *
 * 仅支持 append：`prevKey` 恒为 null。刷新时 [getRefreshKey] 返回 null，从头重新加载。
 */
internal class MediaPagingSource(
    private val context: Context,
    private val mediaType: MediaType,
    private val bucketId: String?,
    private val excludedMediaIds: Set<Long>,
    private val createMediaItem: (mediaInfo: MediaProvider.MediaInfo) -> MatisseMediaItem
) : PagingSource<MediaProvider.MediaPageKey, MatisseMediaItem>() {

    private companion object {
        private const val MAX_FETCH_ROUNDS = 16
    }

    private val loadedMediaIds = HashSet<Long>()

    override fun getRefreshKey(state: PagingState<MediaProvider.MediaPageKey, MatisseMediaItem>): MediaProvider.MediaPageKey? {
        return null
    }

    override suspend fun load(params: LoadParams<MediaProvider.MediaPageKey>): LoadResult<MediaProvider.MediaPageKey, MatisseMediaItem> {
        return try {
            if (params is LoadParams.Refresh) {
                loadedMediaIds.clear()
            }
            val pageSize = params.loadSize
            val mediaItems = ArrayList<MatisseMediaItem>(pageSize)
            var pageKey = params.key
            var reachedEnd = false
            var fetchRounds = 0
            while (mediaItems.size < pageSize && !reachedEnd) {
                fetchRounds += 1
                if (fetchRounds > MAX_FETCH_ROUNDS) {
                    throw IllegalStateException("MediaStore page fetch exceeded $MAX_FETCH_ROUNDS rounds")
                }
                val batchStartKey = pageKey
                val mediaInfoList = MediaProvider.loadMediaInfoPage(
                    context = context,
                    mediaType = mediaType,
                    bucketId = bucketId,
                    limit = pageSize,
                    after = pageKey
                )
                var consumedInBatch = 0
                var addedInBatch = 0
                for (mediaInfo in mediaInfoList) {
                    consumedInBatch += 1
                    pageKey = mediaInfo.pageKey
                    if (excludedMediaIds.contains(element = mediaInfo.mediaId)) {
                        continue
                    }
                    if (!loadedMediaIds.add(element = mediaInfo.mediaId)) {
                        continue
                    }
                    addedInBatch += 1
                    mediaItems.add(element = createMediaItem(mediaInfo))
                    if (mediaItems.size >= pageSize) {
                        break
                    }
                }
                // 本批不足 pageSize 且已全部消费才视为到达末尾；本页已满时剩余行留给下一页
                if (mediaInfoList.size < pageSize && consumedInBatch == mediaInfoList.size) {
                    reachedEnd = true
                } else if (
                    addedInBatch == 0 &&
                    mediaInfoList.size >= pageSize &&
                    pageKey == batchStartKey
                ) {
                    // after 未生效时会反复返回同一批，游标停在原地
                    throw IllegalStateException("MediaStore page cursor did not advance")
                }
            }
            LoadResult.Page(
                data = mediaItems,
                prevKey = null,
                nextKey = if (reachedEnd) {
                    null
                } else {
                    pageKey
                }
            )
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            LoadResult.Error(throwable = throwable)
        }
    }

}
