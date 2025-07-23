package github.leavesczy.matisse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.GlideSubcomposition
import com.bumptech.glide.integration.compose.RequestState
import kotlinx.parcelize.Parcelize

/**
 * @Author: CZY
 * @Date: 2025/7/23 21:24
 * @Desc:
 */
@Parcelize
class GlideImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        GlideComposeImage(
            modifier = Modifier
                .fillMaxSize(),
            model = mediaResource.uri
        )
    }

    @Composable
    override fun Image(mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            GlideImage(
                modifier = Modifier
                    .fillMaxWidth(),
                model = mediaResource.uri,
                contentScale = ContentScale.FillWidth,
                contentDescription = null
            )
        } else {
            GlideImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                model = mediaResource.uri,
                contentScale = ContentScale.Fit,
                contentDescription = null
            )
        }
    }

}

@Composable
private fun GlideComposeImage(
    modifier: Modifier,
    model: Any,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop,
    backgroundColor: Color = colorResource(id = R.color.matisse_media_item_background_color)
) {
    GlideSubcomposition(
        modifier = modifier,
        model = model
    ) {
        when (state) {
            RequestState.Loading,
            RequestState.Failure -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = backgroundColor)
                )
            }

            is RequestState.Success -> {
                Image(
                    modifier = Modifier
                        .fillMaxSize(),
                    painter = painter,
                    alignment = alignment,
                    contentScale = contentScale,
                    contentDescription = null
                )
            }
        }
    }
}