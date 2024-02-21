@file:OptIn(
    ExperimentalGlideComposeApi::class,
    com.github.panpf.zoomimage.compose.glide.internal.ExperimentalGlideComposeApi::class
)

package github.leavesczy.matisse.samples.logic.engine.glide

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.github.panpf.zoomimage.GlideZoomAsyncImage
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import kotlinx.parcelize.Parcelize

/**
 * @Author: leavesCZY
 * @Date: 2023/9/25 16:53
 * @Desc:
 */
@Parcelize
class GlideZoomImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        GlideImage(
            modifier = Modifier.fillMaxSize(),
            model = mediaResource.uri,
            contentScale = ContentScale.Crop,
            contentDescription = mediaResource.name
        )
    }

    @Composable
    override fun Image(mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            GlideImage(
                modifier = Modifier.fillMaxWidth(),
                model = mediaResource.uri,
                contentScale = ContentScale.Fit,
                contentDescription = mediaResource.name
            )
        } else {
            GlideZoomAsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = mediaResource.uri,
                contentScale = ContentScale.Fit,
                contentDescription = mediaResource.name
            )
        }
    }

}