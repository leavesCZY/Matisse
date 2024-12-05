package github.leavesczy.matisse

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: leavesCZY
 * @Date: 2023/8/21 18:13
 * @Desc:
 */
interface MediaFilter : Parcelable {

    /**
     * 用于控制是否要忽略特定的媒体资源
     * 返回 true 则会被忽略，不会展示给用户
     */
    fun ignoreMedia(mediaResource: MediaResource): Boolean

    /**
     * 用于控制是否要默认选中特定的媒体资源
     * 返回 true 则会被默认选中
     */
    fun selectMedia(mediaResource: MediaResource): Boolean

}

/**
 * @param ignoredMimeType 包含在内的 mimeType 将会被忽略，不会展示给用户
 * @param ignoredResourceUri 包含在内的 Uri 将会被忽略，不会展示给用户
 * @param selectedResourceUri 包含在内的 Uri 将会被默认选中
 */
@Parcelize
class DefaultMediaFilter(
    private val ignoredMimeType: Set<String> = emptySet(),
    private val ignoredResourceUri: Set<Uri> = emptySet(),
    private val selectedResourceUri: Set<Uri> = emptySet()
) : MediaFilter {

    override fun ignoreMedia(mediaResource: MediaResource): Boolean {
        return ignoredMimeType.contains(element = mediaResource.mimeType) ||
                ignoredResourceUri.contains(element = mediaResource.uri)
    }

    override fun selectMedia(mediaResource: MediaResource): Boolean {
        return selectedResourceUri.contains(element = mediaResource.uri)
    }

}