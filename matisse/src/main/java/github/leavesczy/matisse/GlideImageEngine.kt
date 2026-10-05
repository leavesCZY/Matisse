package github.leavesczy.matisse

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
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
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.bitmap.Downsampler
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import github.leavesczy.matisse.internal.logic.ImageEngineDecode
import github.leavesczy.matisse.internal.logic.MediaStoreThumbnail
import kotlinx.parcelize.Parcelize
import java.io.InputStream
import java.lang.ref.WeakReference

/**
 * 基于 Glide Compose 的 [ImageEngine] 实现。
 *
 * 宿主需要做的事：
 * - 通过 `implementation` 添加 `com.github.bumptech.glide:compose`（会传递引入 Glide 本体）；
 * - 除此之外无需额外配置。首次为 MediaStore 媒体加载缩略图或预览时，Matisse 会向当前 Glide
 *   实例的 Registry 追加自有模型类型的加载组件，不需要宿主在 AppGlideModule 中注册，也不会改变宿主其它模型
 *   （Uri、String、File 等）的加载行为。宿主通过 `Glide.tearDown` / `Glide.init` 重新初始化后，
 *   Matisse 会在下次使用时自动重新注册。
 *
 * [Thumbnail]：始终按 [MediaStoreThumbnail.MAX_DIMENSION] 解码，网格、相册封面与预览占位共用
 * 同一内存缓存条目，由 Compose 裁切到格子尺寸。Android 10 及以上的 MediaStore 媒体优先读取
 * 系统缩略图，并经由 Glide 的 Downsampler 解码以复用 BitmapPool；读取失败、低版本或 FileProvider
 * 等非 MediaStore Uri 时，回退为 Glide 内置的原图解码 / 视频抽帧。缩略图保留原始宽高比。
 *
 * [Preview]：非视频大图按容器宽度等比展示且支持纵向滚动，加载完成前先展示 [Thumbnail] 的内存缓存。
 * 解码目标的宽高最大限制为 [ImageEngineDecode.MAX_BITMAP_DIMENSION] 像素，超过限制会保持
 * 宽高比降采样。
 */
@Parcelize
class GlideImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        val context = LocalContext.current
        val model = remember(key1 = mediaResource) {
            GlideMediaStoreThumbnail.from(context = context, mediaResource = mediaResource)
        }
        val requestBuilderTransform = remember(key1 = model) {
            { requestBuilder: RequestBuilder<Drawable> ->
                requestBuilder.applyThumbnailOptions(model = model)
            }
        }
        GlideImage(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = R.color.matisse_media_item_background_color)),
            model = model,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            contentDescription = null,
            requestBuilderTransform = requestBuilderTransform
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
                GlidePreviewImage(
                    modifier = Modifier
                        .fillMaxSize(),
                    mediaResource = mediaResource,
                    contentScale = ContentScale.Fit,
                    overrideWidth = decodeWidth,
                    overrideHeight = decodeHeight
                )
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val decodeWidth = ImageEngineDecode.previewWidth(constraints = constraints)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(state = rememberScrollState())
                        .heightIn(min = maxHeight),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlidePreviewImage(
                        modifier = Modifier
                            .fillMaxWidth(),
                        mediaResource = mediaResource,
                        contentScale = ContentScale.FillWidth,
                        overrideWidth = decodeWidth,
                        overrideHeight = ImageEngineDecode.MAX_BITMAP_DIMENSION
                    )
                }
            }
        }
    }

}

/**
 * 缩略图按 [MediaStoreThumbnail.MAX_DIMENSION] 解码以命中同一内存缓存；系统缩略图不写入磁盘缓存。
 */
private fun RequestBuilder<Drawable>.applyThumbnailOptions(model: Any): RequestBuilder<Drawable> {
    val builder = if (model is GlideMediaStoreThumbnail) {
        downsample(DownsampleStrategy.AT_LEAST)
            .dontTransform()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
    } else {
        downsample(DownsampleStrategy.CENTER_OUTSIDE)
    }
    return builder.override(
        MediaStoreThumbnail.MAX_DIMENSION,
        MediaStoreThumbnail.MAX_DIMENSION
    )
}

@Composable
private fun GlidePreviewImage(
    modifier: Modifier,
    mediaResource: MediaResource,
    contentScale: ContentScale,
    overrideWidth: Int,
    overrideHeight: Int
) {
    val context = LocalContext.current
    val requestBuilderTransform =
        remember(key1 = mediaResource, key2 = overrideWidth, key3 = overrideHeight) {
            val thumbnailModel =
                GlideMediaStoreThumbnail.from(context = context, mediaResource = mediaResource)
            val transform: (RequestBuilder<Drawable>) -> RequestBuilder<Drawable> =
                { requestBuilder ->
                    requestBuilder
                        .downsample(DownsampleStrategy.CENTER_INSIDE)
                        .override(overrideWidth, overrideHeight)
                        .thumbnail(
                            Glide.with(context)
                                .load(thumbnailModel)
                                .applyThumbnailOptions(model = thumbnailModel)
                        )
                }
            transform
        }
    GlideImage(
        modifier = modifier,
        model = mediaResource.uri,
        contentScale = contentScale,
        alignment = Alignment.Center,
        contentDescription = null,
        requestBuilderTransform = requestBuilderTransform
    )
}

/**
 * 缩略图请求的模型。Glide 以模型的 equals / hashCode 加上目标尺寸作为内存缓存
 * key，因此只用原始 Uri 参与比较。
 *
 * @param mediaResource 原始媒体，回退解码时使用其 Uri
 * @param thumbnailUri 读取系统缩略图使用的 Images / Video 集合 Uri
 */
private class GlideMediaStoreThumbnail(
    val mediaResource: MediaResource,
    val thumbnailUri: Uri
) {

    override fun equals(other: Any?): Boolean {
        return other is GlideMediaStoreThumbnail && other.mediaResource.uri == mediaResource.uri
    }

    override fun hashCode(): Int {
        return mediaResource.uri.hashCode()
    }

    override fun toString(): String {
        return "GlideMediaStoreThumbnail(uri=${mediaResource.uri})"
    }

    companion object {

        private val lock = Any()

        @Volatile
        private var registeredGlide: WeakReference<Glide>? = null

        /**
         * 系统缩略图可用时返回 [GlideMediaStoreThumbnail]，否则返回原始 Uri。
         * 首次使用前会向当前 Glide 实例注册 Matisse 的加载组件。
         */
        fun from(context: Context, mediaResource: MediaResource): Any {
            val thumbnailUri = MediaStoreThumbnail.thumbnailUri(mediaResource = mediaResource)
                ?: return mediaResource.uri
            ensureRegistered(context = context)
            return GlideMediaStoreThumbnail(
                mediaResource = mediaResource,
                thumbnailUri = thumbnailUri
            )
        }

        private fun ensureRegistered(context: Context) {
            val glide = Glide.get(context)
            if (registeredGlide?.get() === glide) {
                return
            }
            synchronized(lock = lock) {
                if (registeredGlide?.get() === glide) {
                    return
                }
                val applicationContext = context.applicationContext
                val registry = glide.registry
                registry.prepend(
                    GlideMediaStoreThumbnail::class.java,
                    MediaStoreThumbnail.ThumbnailStream::class.java,
                    ThumbnailLoaderFactory(context = applicationContext)
                )
                registry.prepend(
                    Registry.BUCKET_BITMAP,
                    MediaStoreThumbnail.ThumbnailStream::class.java,
                    Bitmap::class.java,
                    ThumbnailDecoder(
                        downsampler = Downsampler(
                            registry.imageHeaderParsers,
                            applicationContext.resources.displayMetrics,
                            glide.bitmapPool,
                            glide.arrayPool
                        ),
                        bitmapPool = glide.bitmapPool
                    )
                )
                // 系统缩略图不可用或读取失败时，Glide 会依次尝试同一模型的其余 LoadData，
                // 回退为 Glide 内置的 Uri 加载链路（图片解码原图、视频抽帧），结果仍写入同一个缓存 key
                registry.append(
                    GlideMediaStoreThumbnail::class.java,
                    InputStream::class.java,
                    OriginalLoaderFactory(dataClass = InputStream::class.java)
                )
                registry.append(
                    GlideMediaStoreThumbnail::class.java,
                    ParcelFileDescriptor::class.java,
                    OriginalLoaderFactory(dataClass = ParcelFileDescriptor::class.java)
                )
                registry.append(
                    GlideMediaStoreThumbnail::class.java,
                    AssetFileDescriptor::class.java,
                    OriginalLoaderFactory(dataClass = AssetFileDescriptor::class.java)
                )
                registeredGlide = WeakReference(glide)
            }
        }

    }

}

/** 仅在目标尺寸不超过系统缩略图尺寸时提供 LoadData，否则交给后续的原图加载器。 */
private class ThumbnailLoader(
    private val context: Context
) : ModelLoader<GlideMediaStoreThumbnail, MediaStoreThumbnail.ThumbnailStream> {

    override fun buildLoadData(
        model: GlideMediaStoreThumbnail,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<MediaStoreThumbnail.ThumbnailStream>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }
        if (width == Target.SIZE_ORIGINAL || height == Target.SIZE_ORIGINAL ||
            !MediaStoreThumbnail.isSizeSupported(width = width, height = height)
        ) {
            return null
        }
        return ModelLoader.LoadData(
            ObjectKey(model),
            ThumbnailFetcher(context = context, model = model)
        )
    }

    override fun handles(model: GlideMediaStoreThumbnail): Boolean {
        return true
    }

}

private class ThumbnailLoaderFactory(
    private val context: Context
) : ModelLoaderFactory<GlideMediaStoreThumbnail, MediaStoreThumbnail.ThumbnailStream> {

    override fun build(
        multiFactory: MultiModelLoaderFactory
    ): ModelLoader<GlideMediaStoreThumbnail, MediaStoreThumbnail.ThumbnailStream> {
        return ThumbnailLoader(context = context)
    }

    override fun teardown() = Unit

}

private class ThumbnailFetcher(
    private val context: Context,
    private val model: GlideMediaStoreThumbnail
) : DataFetcher<MediaStoreThumbnail.ThumbnailStream> {

    private val cancellationSignal = CancellationSignal()

    private var thumbnailStream: MediaStoreThumbnail.ThumbnailStream? = null

    override fun loadData(
        priority: Priority,
        callback: DataFetcher.DataCallback<in MediaStoreThumbnail.ThumbnailStream>
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            callback.onLoadFailed(UnsupportedOperationException("MediaStore thumbnail requires Android 10"))
            return
        }
        val stream = try {
            MediaStoreThumbnail.openStream(
                context = context,
                thumbnailUri = model.thumbnailUri,
                cancellationSignal = cancellationSignal
            )
        } catch (exception: Exception) {
            callback.onLoadFailed(exception)
            return
        }
        thumbnailStream = stream
        callback.onDataReady(stream)
    }

    override fun cleanup() {
        try {
            thumbnailStream?.fileDescriptor?.close()
        } catch (_: Throwable) {
        }
        thumbnailStream = null
    }

    override fun cancel() {
        cancellationSignal.cancel()
    }

    override fun getDataClass(): Class<MediaStoreThumbnail.ThumbnailStream> {
        return MediaStoreThumbnail.ThumbnailStream::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }

}

/**
 * 通过 Glide 的 [Downsampler] 解码系统缩略图，从而复用 [BitmapPool] 中的位图内存，并遵循请求的
 * DownsampleStrategy；随后按 extras 中的角度旋转。
 */
private class ThumbnailDecoder(
    private val downsampler: Downsampler,
    private val bitmapPool: BitmapPool
) : ResourceDecoder<MediaStoreThumbnail.ThumbnailStream, Bitmap> {

    override fun handles(source: MediaStoreThumbnail.ThumbnailStream, options: Options): Boolean {
        return true
    }

    override fun decode(
        source: MediaStoreThumbnail.ThumbnailStream,
        width: Int,
        height: Int,
        options: Options
    ): Resource<Bitmap>? {
        val resource = source.fileDescriptor.createInputStream().use { inputStream ->
            downsampler.decode(inputStream, width, height, options)
        } ?: return null
        val decoded = resource.get()
        val rotated = TransformationUtils.rotateImage(
            decoded,
            Math.floorMod(source.rotationDegrees, 360)
        )
        if (rotated === decoded) {
            return resource
        }
        resource.recycle()
        return BitmapResource.obtain(rotated, bitmapPool)
    }

}

/** 把模型转交给 Glide 内置的 Uri 加载器，使回退解码与系统缩略图共用同一个请求与缓存 key。 */
private class OriginalLoader<Data : Any>(
    private val uriLoader: ModelLoader<Uri, Data>
) : ModelLoader<GlideMediaStoreThumbnail, Data> {

    override fun buildLoadData(
        model: GlideMediaStoreThumbnail,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<Data>? {
        return uriLoader.buildLoadData(model.mediaResource.uri, width, height, options)
    }

    override fun handles(model: GlideMediaStoreThumbnail): Boolean {
        return uriLoader.handles(model.mediaResource.uri)
    }

}

private class OriginalLoaderFactory<Data : Any>(
    private val dataClass: Class<Data>
) : ModelLoaderFactory<GlideMediaStoreThumbnail, Data> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideMediaStoreThumbnail, Data> {
        return OriginalLoader(uriLoader = multiFactory.build(Uri::class.java, dataClass))
    }

    override fun teardown() = Unit

}
