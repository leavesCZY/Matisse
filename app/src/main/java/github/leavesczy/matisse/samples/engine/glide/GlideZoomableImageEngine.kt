@file:OptIn(ExperimentalGlideComposeApi::class)

package github.leavesczy.matisse.samples.engine.glide

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import kotlinx.parcelize.Parcelize
import me.saket.telephoto.zoomable.glide.ZoomableGlideImage

/**
 * @Author: leavesCZY
 * @Date: 2023/10/6 0:19
 * @Desc:
 */
@Parcelize
class GlideZoomableImageEngine : ImageEngine {

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
            ZoomableGlideImage(
                modifier = Modifier.fillMaxSize(),
                model = mediaResource.uri,
                contentScale = ContentScale.Fit,
                contentDescription = mediaResource.name
            )
        }
    }

}