package github.leavesczy.matisse

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import kotlinx.parcelize.Parcelize

/**
 * 基于 Coil 3 的 [ImageEngine] 实现
 * 需额外添加依赖：`io.coil-kt.coil3:coil-compose`
 */
@Parcelize
class CoilImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        CoilComposeImage(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = R.color.matisse_media_item_background_color)),
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
    model: Uri,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center
) {
    AsyncImage(
        modifier = modifier,
        model = model,
        alignment = alignment,
        contentScale = contentScale,
        contentDescription = null
    )
}