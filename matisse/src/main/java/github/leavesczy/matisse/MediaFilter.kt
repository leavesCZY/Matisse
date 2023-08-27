package github.leavesczy.matisse

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: leavesCZY
 * @Desc:
 */
interface MediaFilter : Parcelable {

    fun supportedMimeTypes(): Set<MimeType>

    suspend fun ignoreMedia(mediaResource: MediaResource): Boolean

    suspend fun selectMedia(mediaResource: MediaResource): Boolean

}

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