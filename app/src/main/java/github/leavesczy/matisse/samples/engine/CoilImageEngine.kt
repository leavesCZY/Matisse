package github.leavesczy.matisse.samples.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import kotlinx.parcelize.Parcelize

/**
 * @Author: leavesCZY
 * @Desc:
 */
@Parcelize
class CoilImageEngine : ImageEngine {

    @Composable
    override fun Image(
        modifier: Modifier,
        mediaResource: MediaResource,
        contentScale: ContentScale
    ) {
        val context = LocalContext.current
        val request = ImageRequest
            .Builder(context = context)
            .crossfade(enable = false)
            .data(data = mediaResource.uri)
            .build()
        AsyncImage(
            modifier = modifier,
            model = request,
            contentDescription = mediaResource.name,
            contentScale = contentScale,
            filterQuality = FilterQuality.None
        )
    }

}