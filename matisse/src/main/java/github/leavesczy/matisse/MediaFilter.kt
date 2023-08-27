package github.leavesczy.matisse

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: leavesCZY
 * @Desc:
 */
interface MediaFilter : Parcelable {

    /**
     * 要展示的媒体资源类型
     */
    fun supportedMimeTypes(): Set<MimeType>

    /**
     * 用于控制是否要忽略特定的媒体资源
     * 返回 true 则会被忽略，不会展示给用户
     */
    suspend fun ignoreMedia(mediaResource: MediaResource): Boolean

    /**
     * 用于控制是否要默认选中特定的媒体资源
     * 返回 true 则会被默认选中
     */
    suspend fun selectMedia(mediaResource: MediaResource): Boolean

}

/**
 * @param supportedMimeTypes 要展示的媒体资源类型
 * @param ignoredResourceUri 包含在内的 Uri 将会被忽略，不会展示给用户
 * @param selectedResourceUri 包含在内的 Uri 将会被默认选中
 */
@Parcelize
class DefaultMediaFilter(
    private val supportedMimeTypes: Set<MimeType>,
    private val ignoredResourceUri: Set<Uri> = emptySet(),
    private val selectedResourceUri: Set<Uri> = emptySet()
) : MediaFilter {

    override fun supportedMimeTypes(): Set<MimeType> {
        return supportedMimeTypes
    }

    override suspend fun ignoreMedia(mediaResource: MediaResource): Boolean {
        if (ignoredResourceUri.isEmpty()) {
            return false
        }
        return ignoredResourceUri.contains(element = mediaResource.uri)
    }

    override suspend fun selectMedia(mediaResource: MediaResource): Boolean {
        if (selectedResourceUri.isEmpty()) {
            return false
        }
        return selectedResourceUri.contains(element = mediaResource.uri)
    }

}