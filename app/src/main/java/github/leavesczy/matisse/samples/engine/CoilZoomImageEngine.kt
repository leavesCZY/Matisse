package github.leavesczy.matisse.samples.engine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import kotlinx.parcelize.Parcelize

/**
 * @Author: leavesCZY
 * @Date: 2023/9/25 16:53
 * @Desc:
 */
@Parcelize
class CoilZoomImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize(),
            model = mediaResource.uri,
            contentDescription = mediaResource.name,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.None
        )
    }

    @Composable
    override fun Image(mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight(align = Alignment.CenterVertically),
                model = mediaResource.uri,
                contentDescription = mediaResource.name,
                contentScale = ContentScale.FillWidth,
                filterQuality = FilterQuality.None
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CoilZoomAsyncImage(
                    modifier = Modifier
                        .align(alignment = Alignment.Center)
                        .fillMaxSize(),
                    model = mediaResource.uri,
                    contentDescription = mediaResource.name,
                    contentScale = ContentScale.FillWidth,
                    filterQuality = FilterQuality.None
                )
            }
        }
    }

}