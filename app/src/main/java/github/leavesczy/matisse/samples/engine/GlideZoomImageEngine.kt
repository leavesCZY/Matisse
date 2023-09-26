package github.leavesczy.matisse.samples.engine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.DecodeFormat
import com.github.panpf.zoomimage.GlideZoomAsyncImage
import com.github.panpf.zoomimage.compose.glide.internal.ExperimentalGlideComposeApi
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

    @OptIn(com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi::class)
    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        GlideImage(
            modifier = Modifier
                .fillMaxSize(),
            model = mediaResource.uri,
            contentDescription = mediaResource.name,
            contentScale = ContentScale.Crop,
            requestBuilderTransform = {
                it.format(DecodeFormat.PREFER_RGB_565)
            }
        )
    }

    @OptIn(
        ExperimentalGlideComposeApi::class,
        com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi::class
    )
    @Composable
    override fun Image(mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            GlideImage(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight(align = Alignment.CenterVertically),
                model = mediaResource.uri,
                contentDescription = mediaResource.name,
                contentScale = ContentScale.FillWidth,
                requestBuilderTransform = {
                    it.format(DecodeFormat.PREFER_RGB_565)
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                GlideZoomAsyncImage(
                    modifier = Modifier
                        .align(alignment = Alignment.Center)
                        .fillMaxSize(),
                    model = mediaResource.uri,
                    contentDescription = mediaResource.name,
                    contentScale = ContentScale.FillWidth,
                    requestBuilderTransform = {
                        it.format(DecodeFormat.PREFER_RGB_565)
                    }
                )
            }
        }
    }

}