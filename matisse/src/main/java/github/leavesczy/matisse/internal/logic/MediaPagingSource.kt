package github.leavesczy.matisse.internal.logic

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import github.leavesczy.matisse.MediaType
import kotlin.coroutines.cancellation.CancellationException

internal class MediaPagingSource(
    private val context: Context,
    private val mediaType: MediaType,
    private val bucketId: String?,
    private val createMediaItem: MatisseMediaItemFactory
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
                for (mediaInfo in mediaInfoList) {
                    queryOffset += 1
                    if (!loadedMediaIds.add(element = mediaInfo.mediaId)) {
                        continue
                    }
                    mediaItems.add(element = createMediaItem(mediaInfo = mediaInfo))
                    if (mediaItems.size >= pageSize) {
                        break
                    }
                }
                if (mediaInfoList.size < pageSize) {
                    reachedEnd = true
                }
            }
            LoadResult.Page(
                data = mediaItems,
                prevKey = if (startOffset == 0) {
                    null
                } else {
                    (startOffset - pageSize).coerceAtLeast(minimumValue = 0)
                },
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
