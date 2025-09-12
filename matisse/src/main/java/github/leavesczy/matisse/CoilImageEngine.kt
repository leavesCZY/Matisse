package github.leavesczy.matisse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import kotlinx.parcelize.Parcelize

/**
 * @Author: CZY
 * @Date: 2025/7/23 21:24
 * @Desc:
 */
@Parcelize
class CoilImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        CoilComposeImage(
            modifier = Modifier
                .fillMaxSize(),
            model = mediaResource.uri,
            contentScale = ContentScale.Crop
        )
    }

    @Composable
    override fun Image(mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            CoilComposeImage(
                modifier = Modifier
                    .fillMaxWidth(),
                model = mediaResource.uri,
                contentScale = ContentScale.FillWidth
            )
        } else {
            CoilComposeImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                model = mediaResource.uri,
                contentScale = ContentScale.FillWidth
            )
        }
    }

}

@Composable
private fun CoilComposeImage(
    modifier: Modifier,
    model: Any,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop,
    backgroundColor: Color? = colorResource(id = R.color.matisse_media_item_background_color)
) {
    SubcomposeAsyncImage(
        modifier = modifier,
        model = model,
        alignment = alignment,
        contentScale = contentScale,
        contentDescription = null
    ) {
        val state by painter.state.collectAsState()
        when (state) {
            AsyncImagePainter.State.Empty,
            is AsyncImagePainter.State.Loading,
            is AsyncImagePainter.State.Error -> {
                if (backgroundColor != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = backgroundColor)
                    )
                }
            }

            is AsyncImagePainter.State.Success -> {
                SubcomposeAsyncImageContent()
            }
        }
    }
}