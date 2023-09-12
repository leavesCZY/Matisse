package github.leavesczy.matisse.samples.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.DecodeFormat
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import kotlinx.parcelize.Parcelize

@Parcelize
class GlideImageEngine : ImageEngine {

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    override fun Image(
        modifier: Modifier,
        mediaResource: MediaResource,
        contentScale: ContentScale
    ) {
        GlideImage(
            modifier = modifier,
            model = mediaResource.uri,
            contentDescription = mediaResource.name,
            contentScale = contentScale,
            requestBuilderTransform = {
                it.format(DecodeFormat.PREFER_RGB_565)
            }
        )
    }

}