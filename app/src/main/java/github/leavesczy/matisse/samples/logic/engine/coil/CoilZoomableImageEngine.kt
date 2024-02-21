package github.leavesczy.matisse.samples.logic.engine.coil

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import kotlinx.parcelize.Parcelize
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage

/**
 * @Author: leavesCZY
 * @Date: 2023/10/6 0:19
 * @Desc:
 */
@Parcelize
class CoilZoomableImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = mediaResource.uri,
            contentScale = ContentScale.Crop,
            contentDescription = mediaResource.name
        )
    }

    @Composable
    override fun Image(mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            AsyncImage(
                modifier = Modifier.fillMaxWidth(),
                model = mediaResource.uri,
                contentScale = ContentScale.FillWidth,
                contentDescription = mediaResource.name
            )
        } else {
            ZoomableAsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = mediaResource.uri,
                contentScale = ContentScale.FillWidth,
                contentDescription = mediaResource.name
            )
        }
    }

}