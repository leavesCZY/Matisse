package github.leavesczy.matisse

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.bumptech.glide.integration.compose.GlideImage
import kotlinx.parcelize.Parcelize

/**
 * @Author: leavesCZY
 * @Date: 2023/6/7 23:11
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
            contentDescription = null
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

@Parcelize
class CoilImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        val context = LocalContext.current
        val imageRequest = remember(key1 = mediaResource.uri) {
            val uri = mediaResource.uri
            val memoryCacheKey = uri.toString()
            ImageRequest.Builder(context = context)
                .data(data = uri)
                .memoryCacheKey(key = memoryCacheKey)
                .placeholderMemoryCacheKey(key = memoryCacheKey)
                .crossfade(enable = true)
                .build()
        }
        AsyncImage(
            modifier = Modifier
                .fillMaxSize(),
            model = imageRequest,
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
    }

    @Composable
    override fun Image(mediaResource: MediaResource) {
        val context = LocalContext.current
        val imageRequest = remember(key1 = mediaResource.uri) {
            val uri = mediaResource.uri
            val memoryCacheKey = uri.toString()
            ImageRequest.Builder(context = context)
                .data(data = uri)
                .memoryCacheKey(key = memoryCacheKey)
                .placeholderMemoryCacheKey(key = memoryCacheKey)
                .build()
        }
        if (mediaResource.isVideo) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth(),
                model = imageRequest,
                contentScale = ContentScale.FillWidth,
                contentDescription = null
            )
        } else {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                model = imageRequest,
                contentScale = ContentScale.FillWidth,
                contentDescription = null
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