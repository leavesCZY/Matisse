package github.leavesczy.matisse.samples.engine

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                model = mediaResource.uri,
                contentScale = ContentScale.FillWidth,
                contentDescription = mediaResource.name
            )
        }
    }

}