package github.leavesczy.matisse

import android.net.Uri
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
import androidx.compose.ui.res.stringResource
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlinx.parcelize.Parcelize

/**
 * 基于 Glide Compose 的 [ImageEngine] 实现
 * 需额外添加依赖：`com.github.bumptech.glide:compose`
 */
@Parcelize
class GlideImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        GlideComposeImage(
            modifier = Modifier
                .fillMaxSize(),
            model = mediaResource.uri,
            contentScale = ContentScale.Crop,
            backgroundColor = colorResource(id = R.color.matisse_media_item_background_color)
        )
    }

    @Composable
    override fun Image(mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            GlideComposeImage(
                modifier = Modifier
                    .fillMaxWidth(),
                model = mediaResource.uri,
                contentScale = ContentScale.FillWidth,
                backgroundColor = null
            )
        } else {
            GlideComposeImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                model = mediaResource.uri,
                contentScale = ContentScale.FillWidth,
                backgroundColor = null
            )
        }
    }

}

@Composable
private fun GlideComposeImage(
    modifier: Modifier,
    model: Uri,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    backgroundColor: Color?
) {
    GlideImage(
        modifier = modifier,
        model = model,
        contentScale = contentScale,
        alignment = alignment,
        loading = if (backgroundColor == null) {
            null
        } else {
            placeholder {
                Placeholder(backgroundColor = backgroundColor)
            }
        },
        failure = if (backgroundColor == null) {
            null
        } else {
            placeholder {
                Placeholder(backgroundColor = backgroundColor)
            }
        },
        contentDescription = null
    )
}

@Composable
private fun Placeholder(backgroundColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = backgroundColor)
    )
}