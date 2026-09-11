package github.leavesczy.matisse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.maxBitmapSize
import coil3.size.Dimension
import coil3.size.Size
import coil3.video.VideoFrameDecoder
import kotlinx.parcelize.Parcelize

private const val MAX_IMAGE_DECODE_DIMENSION = 4096

/**
 * 基于 Coil 3 的 [ImageEngine] 实现。
 *
 * 宿主应用必须添加 `io.coil-kt.coil3:coil-compose`；需要展示视频封面时还必须添加
 * `io.coil-kt.coil3:coil-video`。如需加载 GIF，还需添加 `io.coil-kt.coil3:coil-gif`
 * 并在宿主的 [coil3.ImageLoader] 中注册对应的 GIF Decoder。
 *
 * 缩略图会裁切并填满容器；视频封面会完整显示在预览区域内。非视频大图按容器宽度
 * 等比展示且支持纵向滚动，其解码位图的宽高最大限制为 4096 像素。超过限制的图片
 * 会保持宽高比进行降采样，因此放大后清晰度可能降低。
 */
@Parcelize
class CoilImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        CoilComposeImage(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = R.color.matisse_media_item_background_color)),
            model = rememberCoilModel(mediaResource = mediaResource),
            contentScale = ContentScale.Crop
        )
    }

    @Composable
    override fun Image(mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CoilComposeImage(
                    modifier = Modifier
                        .fillMaxSize(),
                    model = rememberCoilModel(mediaResource = mediaResource),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val context = LocalContext.current
                val maxWidth = constraints.maxWidth
                val request = remember(key1 = mediaResource.uri, key2 = maxWidth) {
                    ImageRequest.Builder(context = context)
                        .data(data = mediaResource.uri)
                        // 高度无约束时显式提供目标宽度，避免按原始尺寸解码
                        .size(size = Size(width = maxWidth, height = Dimension.Undefined))
                        // 保持宽高比降采样，将单张位图的最大宽高限制在 4096 像素
                        .maxBitmapSize(
                            size = Size(
                                width = MAX_IMAGE_DECODE_DIMENSION,
                                height = MAX_IMAGE_DECODE_DIMENSION
                            )
                        )
                        .build()
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(state = rememberScrollState())
                        .heightIn(min = maxHeight),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CoilComposeImage(
                        modifier = Modifier
                            .fillMaxWidth(),
                        model = request,
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }

}

@Composable
private fun rememberCoilModel(mediaResource: MediaResource): Any {
    if (!mediaResource.isVideo) {
        return mediaResource.uri
    }
    val context = LocalContext.current
    return remember(key1 = mediaResource.uri) {
        ImageRequest.Builder(context = context)
            .data(data = mediaResource.uri)
            // MediaStore Uri 通常没有文件扩展名，因此需要明确指定视频帧解码器
            .decoderFactory { result, options, _ ->
                VideoFrameDecoder(source = result.source, options = options)
            }
            .build()
    }
}

@Composable
private fun CoilComposeImage(
    modifier: Modifier,
    model: Any,
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