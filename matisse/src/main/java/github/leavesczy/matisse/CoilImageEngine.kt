package github.leavesczy.matisse

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import coil3.ImageLoader
import coil3.asImage
import coil3.compose.AsyncImage
import coil3.decode.ContentMetadata
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.toBitmap
import coil3.toCoilUri
import coil3.video.VideoFrameDecoder
import github.leavesczy.matisse.internal.logic.ImageEngineDecode
import github.leavesczy.matisse.internal.logic.MediaStoreThumbnail
import kotlinx.coroutines.CancellationException
import kotlinx.parcelize.Parcelize
import okio.buffer
import okio.source

/**
 * 基于 Coil 3 的 [ImageEngine] 实现。
 *
 * 宿主需要做的事：
 * - 通过 `implementation` 添加 `io.coil-kt.coil3:coil-compose` 和 `io.coil-kt.coil3:coil-video`；
 * - 如需在预览页播放 GIF 等动图，还需添加 `io.coil-kt.coil3:coil-gif`，并在 Application 中通过
 *   [coil3.SingletonImageLoader.setSafe] 向宿主 [coil3.ImageLoader] 注册对应 Decoder。
 *   Matisse 使用独立 Activity，不会读到宿主 Composable 树上的 `LocalImageLoader`；
 * - 除此之外无需额外配置。缩略图读取逻辑通过请求级 Fetcher 接入，不需要在 ImageLoader 中
 *   注册组件，也不会影响宿主自身的图片请求。
 *
 * [Thumbnail]：始终按 [MediaStoreThumbnail.MAX_DIMENSION] 解码，并以媒体 Uri 为内存缓存 key，
 * 网格、相册封面与预览占位共用同一张 Bitmap，由 Compose 裁切到格子尺寸。
 * Android 10 及以上的 MediaStore 媒体优先打开系统缩略图编码流，交给宿主 ImageLoader 的
 * Decoder 解码，并按 extras 中的方向旋转；读取失败、低版本或 FileProvider 等非 MediaStore
 * Uri 时，按同一目标尺寸解码原图或抽取视频帧。系统缩略图为静态图；回退解码时是否展示动图
 * 取决于宿主 ImageLoader 注册的 Decoder。
 *
 * [Preview]：非视频大图按容器宽度等比展示且支持纵向滚动，加载完成前先展示 [Thumbnail] 的内存缓存。
 * 解码位图的宽高最大限制为 [ImageEngineDecode.MAX_BITMAP_DIMENSION] 像素，超过限制会保持
 * 宽高比降采样。
 */
@Parcelize
class CoilImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        CoilComposeImage(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = R.color.matisse_media_item_background_color)),
            model = rememberCoilThumbnailRequest(mediaResource = mediaResource),
            contentScale = ContentScale.Crop
        )
    }

    @Composable
    override fun Preview(mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val (decodeWidth, decodeHeight) = ImageEngineDecode.targetSize(constraints = constraints)
                CoilComposeImage(
                    modifier = Modifier
                        .fillMaxSize(),
                    model = rememberCoilVideoPreviewRequest(
                        mediaResource = mediaResource,
                        decodeWidth = decodeWidth,
                        decodeHeight = decodeHeight
                    ),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val decodeWidth = ImageEngineDecode.previewWidth(constraints = constraints)
                val request = rememberCoilPreviewRequest(
                    mediaResource = mediaResource,
                    decodeWidth = decodeWidth
                )
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
private fun rememberCoilThumbnailRequest(mediaResource: MediaResource): ImageRequest {
    val context = LocalContext.current
    return remember(key1 = mediaResource) {
        val thumbnailSize = Size(
            width = MediaStoreThumbnail.MAX_DIMENSION,
            height = MediaStoreThumbnail.MAX_DIMENSION
        )
        val thumbnailUri = MediaStoreThumbnail.thumbnailUri(mediaResource = mediaResource)
        ImageRequest.Builder(context = context)
            .memoryCacheKey(key = MediaStoreThumbnail.memoryCacheKey(mediaResource = mediaResource))
            .size(size = thumbnailSize)
            .scale(scale = Scale.FIT)
            .precision(precision = Precision.INEXACT)
            .maxBitmapSize(
                size = Size(
                    width = ImageEngineDecode.MAX_BITMAP_DIMENSION,
                    height = ImageEngineDecode.MAX_BITMAP_DIMENSION
                )
            )
            .apply {
                if (thumbnailUri == null) {
                    data(data = mediaResource.uri)
                    if (mediaResource.isVideo) {
                        decoderFactory { result, options, _ ->
                            VideoFrameDecoder(source = result.source, options = options)
                        }
                    }
                } else {
                    data(
                        data = CoilMediaStoreThumbnail(
                            mediaResource = mediaResource,
                            thumbnailUri = thumbnailUri
                        )
                    )
                    fetcherFactory(factory = CoilMediaStoreThumbnailFetcher.Factory)
                }
            }
            .build()
    }
}

/** 与 [rememberCoilThumbnailRequest] 相同的内存缓存 key，供预览加载完成前占位。 */
private fun thumbnailPlaceholderKey(mediaResource: MediaResource): MemoryCache.Key {
    return MemoryCache.Key(key = MediaStoreThumbnail.memoryCacheKey(mediaResource = mediaResource))
}

@Composable
private fun rememberCoilVideoPreviewRequest(
    mediaResource: MediaResource,
    decodeWidth: Int,
    decodeHeight: Int
): ImageRequest {
    val context = LocalContext.current
    return remember(key1 = mediaResource, key2 = decodeWidth, key3 = decodeHeight) {
        ImageRequest.Builder(context = context)
            .data(data = mediaResource.uri)
            .size(size = Size(width = decodeWidth, height = decodeHeight))
            .maxBitmapSize(
                size = Size(
                    width = ImageEngineDecode.MAX_BITMAP_DIMENSION,
                    height = ImageEngineDecode.MAX_BITMAP_DIMENSION
                )
            )
            .placeholderMemoryCacheKey(key = thumbnailPlaceholderKey(mediaResource = mediaResource))
            .apply {
                if (mediaResource.isVideo) {
                    decoderFactory { result, options, _ ->
                        VideoFrameDecoder(source = result.source, options = options)
                    }
                }
            }
            .build()
    }
}

@Composable
private fun rememberCoilPreviewRequest(
    mediaResource: MediaResource,
    decodeWidth: Int
): ImageRequest {
    val context = LocalContext.current
    return remember(key1 = mediaResource, key2 = decodeWidth) {
        ImageRequest.Builder(context = context)
            .data(data = mediaResource.uri)
            // 高度无约束时显式提供目标宽度，避免按原始尺寸解码
            .size(size = Size(width = decodeWidth, height = Dimension.Undefined))
            .maxBitmapSize(
                size = Size(
                    width = ImageEngineDecode.MAX_BITMAP_DIMENSION,
                    height = ImageEngineDecode.MAX_BITMAP_DIMENSION
                )
            )
            .placeholderMemoryCacheKey(key = thumbnailPlaceholderKey(mediaResource = mediaResource))
            .build()
    }
}

@Composable
private fun CoilComposeImage(
    modifier: Modifier,
    model: Any,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        modifier = modifier,
        model = model,
        alignment = Alignment.Center,
        contentScale = contentScale,
        contentDescription = null
    )
}

/**
 * 缩略图请求的数据模型，仅通过请求级 [Fetcher.Factory] 处理，不需要宿主在 [ImageLoader] 中注册任何组件。
 *
 * @param mediaResource 原始媒体，回退解码时使用其 Uri
 * @param thumbnailUri 读取系统缩略图使用的 Images / Video 集合 Uri
 */
private class CoilMediaStoreThumbnail(
    val mediaResource: MediaResource,
    val thumbnailUri: Uri
)

/**
 * 目标尺寸不超过 [MediaStoreThumbnail.MAX_DIMENSION] 时打开系统缩略图编码流，交给
 * [ImageLoader] 的 Decoder 解码并按 extras 旋转；否则或读取失败时解码原图（视频则抽帧）。
 * 结果写入请求指定的内存缓存 key。
 */
private class CoilMediaStoreThumbnailFetcher(
    private val data: CoilMediaStoreThumbnail,
    private val options: Options,
    private val imageLoader: ImageLoader
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val targetWidth = options.size.width.pxOrElse { 0 }
        val targetHeight = options.size.height.pxOrElse { 0 }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            MediaStoreThumbnail.isSizeSupported(width = targetWidth, height = targetHeight)
        ) {
            try {
                return fetchSystemThumbnail()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
            }
        }
        return fetchOriginal()
    }

    private suspend fun fetchSystemThumbnail(): ImageFetchResult {
        val thumbnail = MediaStoreThumbnail.openStream(
            context = options.context,
            thumbnailUri = data.thumbnailUri
        )
        val fileDescriptor = thumbnail.fileDescriptor
        val imageSource = ImageSource(
            source = fileDescriptor.createInputStream().source().buffer(),
            fileSystem = options.fileSystem,
            metadata = ContentMetadata(
                uri = data.thumbnailUri.toCoilUri(),
                assetFileDescriptor = fileDescriptor
            )
        )
        try {
            val decoded = decodeImage(
                fetchResult = SourceFetchResult(
                    source = imageSource,
                    mimeType = "image/jpeg",
                    dataSource = DataSource.DISK
                ),
                decodeOptions = options
            )
            return rotateImageFetchResult(
                result = decoded,
                rotationDegrees = thumbnail.rotationDegrees
            )
        } finally {
            imageSource.close()
            try {
                fileDescriptor.close()
            } catch (_: Throwable) {
            }
        }
    }

    private suspend fun fetchOriginal(): FetchResult {
        val mediaResource = data.mediaResource
        val decodeOptions = options.copy(
            scale = Scale.FILL,
            memoryCachePolicy = CachePolicy.DISABLED,
            diskCachePolicy = CachePolicy.DISABLED
        )
        val mapped = imageLoader.components.map(data = mediaResource.uri, options = decodeOptions)
        val fetcher = imageLoader.components.newFetcher(
            data = mapped,
            options = decodeOptions,
            imageLoader = imageLoader
        )?.first ?: throw IllegalStateException("No fetcher for ${mediaResource.uri}")
        return when (val fetchResult = fetcher.fetch()) {
            is ImageFetchResult -> fetchResult
            is SourceFetchResult -> decodeSource(
                fetchResult = fetchResult,
                decodeOptions = decodeOptions,
                mediaResource = mediaResource
            )

            null -> throw IllegalStateException("Fetch failed for ${mediaResource.uri}")
        }
    }

    private suspend fun decodeSource(
        fetchResult: SourceFetchResult,
        decodeOptions: Options,
        mediaResource: MediaResource
    ): ImageFetchResult {
        if (mediaResource.isVideo) {
            val decodeResult = VideoFrameDecoder(
                source = fetchResult.source,
                options = decodeOptions
            ).decode()
            return ImageFetchResult(
                image = decodeResult.image,
                isSampled = decodeResult.isSampled,
                dataSource = fetchResult.dataSource
            )
        }
        return decodeImage(fetchResult = fetchResult, decodeOptions = decodeOptions)
    }

    private suspend fun decodeImage(
        fetchResult: SourceFetchResult,
        decodeOptions: Options
    ): ImageFetchResult {
        var factoryIndex = 0
        while (true) {
            val decoderPair = imageLoader.components.newDecoder(
                result = fetchResult,
                options = decodeOptions,
                imageLoader = imageLoader,
                startIndex = factoryIndex
            ) ?: throw IllegalStateException("No decoder for ${data.mediaResource.uri}")
            val decodeResult = decoderPair.first.decode()
            if (decodeResult != null) {
                return ImageFetchResult(
                    image = decodeResult.image,
                    isSampled = decodeResult.isSampled,
                    dataSource = fetchResult.dataSource
                )
            }
            factoryIndex = decoderPair.second + 1
        }
    }

    private fun rotateImageFetchResult(
        result: ImageFetchResult,
        rotationDegrees: Int
    ): ImageFetchResult {
        val degrees = Math.floorMod(rotationDegrees, 360)
        if (degrees == 0) {
            return result
        }
        var bitmap = result.image.toBitmap()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            bitmap.config == Bitmap.Config.HARDWARE
        ) {
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return result
        }
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
        return ImageFetchResult(
            image = rotated.asImage(),
            isSampled = result.isSampled,
            dataSource = result.dataSource
        )
    }

    object Factory : Fetcher.Factory<CoilMediaStoreThumbnail> {

        override fun create(
            data: CoilMediaStoreThumbnail,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return CoilMediaStoreThumbnailFetcher(
                data = data,
                options = options,
                imageLoader = imageLoader
            )
        }

    }

}
