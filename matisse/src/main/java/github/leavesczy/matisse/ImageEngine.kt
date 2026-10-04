package github.leavesczy.matisse

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Matisse 用于展示图片和视频封面的加载引擎。
 *
 * 实现会随 [Matisse] 一同通过 Intent 传递，因此实现类及其成员必须满足 [Parcelable] 要求。
 * Matisse 不传递 Coil 或 Glide 依赖，宿主应用需要根据所选实现自行添加运行时依赖。
 *
 * @see CoilImageEngine
 * @see GlideImageEngine
 */
@Stable
interface ImageEngine : Parcelable {

    /**
     * 展示媒体网格和相册列表中的图片缩略图或视频封面。
     *
     * 调用方提供有界容器，实现应填满容器，允许裁切以统一格子比例。
     */
    @Composable
    fun Thumbnail(mediaResource: MediaResource)

    /**
     * 展示预览页中的完整图片或视频封面。
     *
     * 实现自行处理内容尺寸：图片保持宽高比且不裁切，超出预览区域时提供查看完整内容的方式；
     * 视频只展示静态封面，播放由 Matisse 处理。
     */
    @Composable
    fun Preview(mediaResource: MediaResource)

}