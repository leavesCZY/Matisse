package github.leavesczy.matisse

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 媒体资源筛选规则，用于控制列表展示与默认选中行为
 */
interface MediaFilter : Parcelable {

    /**
     * 是否忽略该媒体资源
     * 返回 true 则不会展示给用户
     */
    suspend fun shouldIgnoreMedia(mediaResource: MediaResource): Boolean

    /**
     * 是否默认选中该媒体资源
     * 返回 true 则会被默认选中，仅在 [Matisse.fastSelect] 为 false 时生效
     */
    suspend fun shouldSelectMedia(mediaResource: MediaResource): Boolean

}

/**
 * @param ignoredMimeTypes 包含在内的 mimeType 将会被忽略，不会展示给用户
 * @param ignoredMediaUris 包含在内的 Uri 将会被忽略，不会展示给用户
 * @param selectedMediaUris 包含在内的 Uri 将会被默认选中，仅在 [Matisse.fastSelect] 为 false 时生效
 */
@Parcelize
class DefaultMediaFilter(
    private val ignoredMimeTypes: Set<String> = emptySet(),
    private val ignoredMediaUris: Set<Uri> = emptySet(),
    private val selectedMediaUris: Set<Uri> = emptySet()
) : MediaFilter {

    override suspend fun shouldIgnoreMedia(mediaResource: MediaResource): Boolean {
        return ignoredMimeTypes.contains(element = mediaResource.mimeType) ||
                ignoredMediaUris.contains(element = mediaResource.uri)
    }

    override suspend fun shouldSelectMedia(mediaResource: MediaResource): Boolean {
        return selectedMediaUris.contains(element = mediaResource.uri)
    }

}