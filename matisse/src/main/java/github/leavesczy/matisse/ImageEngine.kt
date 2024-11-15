package github.leavesczy.matisse

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.bumptech.glide.integration.compose.GlideImage
import kotlinx.parcelize.Parcelize

/**
 * @Author: leavesCZY
 * @Desc:
 */
@Stable
interface ImageEngine : Parcelable {

    /**
     * 加载缩略图时调用
     */
    @Composable
    fun Thumbnail(mediaResource: MediaResource)

    /**
     * 加载大图时调用
     */
    @Composable
    fun Image(mediaResource: MediaResource)

}

@Parcelize
class GlideImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        GlideImage(
            modifier = Modifier
                .fillMaxSize(),
            model = mediaResource.uri,
            contentScale = ContentScale.Crop,
            contentDescription = mediaResource.name
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
                contentDescription = mediaResource.name
            )
        } else {
            GlideImage(
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

@Parcelize
class CoilImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize(),
            model = mediaResource.uri,
            contentScale = ContentScale.Crop,
            contentDescription = mediaResource.name
        )
    }

    @Composable
    override fun Image(mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth(),
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

@Parcelize
class Coil3ImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        coil3.compose.AsyncImage(
            modifier = Modifier
                .fillMaxSize(),
            model = mediaResource.uri,
            contentScale = ContentScale.Crop,
            contentDescription = mediaResource.name
        )
    }

    @Composable
    override fun Image(mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            coil3.compose.AsyncImage(
                modifier = Modifier
                    .fillMaxWidth(),
                model = mediaResource.uri,
                contentScale = ContentScale.FillWidth,
                contentDescription = mediaResource.name
            )
        } else {
            coil3.compose.AsyncImage(
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