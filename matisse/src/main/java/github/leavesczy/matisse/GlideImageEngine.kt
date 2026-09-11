package github.leavesczy.matisse

import android.net.Uri
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import kotlinx.parcelize.Parcelize

private const val MAX_IMAGE_DECODE_DIMENSION = 4096

/**
 * 基于 Glide Compose 的 [ImageEngine] 实现。
 *
 * 宿主应用必须通过 `implementation` 添加 `com.github.bumptech.glide:compose`。
 *
 * 缩略图会裁切并填满容器；视频封面会完整显示在预览区域内。非视频大图按容器宽度
 * 等比展示且支持纵向滚动，其解码目标的宽高最大限制为 4096 像素。超过限制的图片
 * 会保持宽高比进行降采样，因此放大后清晰度可能降低。
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
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GlideComposeImage(
                    modifier = Modifier
                        .fillMaxSize(),
                    model = mediaResource.uri,
                    contentScale = ContentScale.Fit,
                    backgroundColor = null
                )
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val maxWidth = constraints.maxWidth
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(state = rememberScrollState())
                        .heightIn(min = maxHeight),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlideComposeImage(
                        modifier = Modifier
                            .fillMaxWidth(),
                        model = mediaResource.uri,
                        contentScale = ContentScale.FillWidth,
                        backgroundColor = null,
                        overrideWidth = maxWidth
                    )
                }
            }
        }
    }

}

@Composable
private fun GlideComposeImage(
    modifier: Modifier,
    model: Uri,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    backgroundColor: Color?,
    overrideWidth: Int? = null
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
        contentDescription = null,
        requestBuilderTransform = { requestBuilder ->
            if (overrideWidth == null) {
                requestBuilder
            } else {
                // 保持宽高比采样，并将解码目标的最大宽高限制在 4096 像素
                requestBuilder
                    .override(
                        overrideWidth.coerceAtMost(maximumValue = MAX_IMAGE_DECODE_DIMENSION),
                        MAX_IMAGE_DECODE_DIMENSION
                    )
                    .downsample(DownsampleStrategy.CENTER_INSIDE)
            }
        }
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