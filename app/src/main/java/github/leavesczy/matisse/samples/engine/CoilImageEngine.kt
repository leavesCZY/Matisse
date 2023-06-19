package github.leavesczy.matisse.samples.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
        AsyncImage(
            modifier = modifier,
            model = mediaResource.uri,
            contentDescription = mediaResource.name,
            contentScale = contentScale,
            filterQuality = FilterQuality.None
        )
    }

}