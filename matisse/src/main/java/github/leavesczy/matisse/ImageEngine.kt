package github.leavesczy.matisse

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Matisse 用于展示图片和视频封面的加载引擎。
 *
 * 实现会随 [Matisse] 一同通过 Intent 传递，因此实现类及其成员必须满足 [Parcelable] 要求。
 * Matisse 不传递 Coil 或 Glide 依赖，宿主应用需要根据所选实现自行添加运行时依赖。
 * 需实现 [Thumbnail]（网格与相册列表缩略图）与 [Preview]（预览页完整图片或视频封面）两个
 * 主线程 Composable；实现应保持可重入且不得执行阻塞操作。
 *
 * @see CoilImageEngine
 * @see GlideImageEngine
 */
@Stable
interface ImageEngine : Parcelable {

    /**
     * 展示媒体网格和相册列表中的图片缩略图或视频封面缩略图。
     *
     * 调用方会提供有界容器，实现应填充容器；允许通过裁切保持统一的缩略图尺寸。
     *
     * @param mediaResource 需要展示的图片或视频资源
     */
    @Composable
    fun Thumbnail(mediaResource: MediaResource)

    /**
     * 展示预览页面中的完整图片或视频封面。
     *
     * 实现需要自行处理内容尺寸。图片应保持宽高比且避免裁切，超出预览区域时应提供可查看完整内容的方式；
     * 视频资源只需展示静态封面，视频播放由 Matisse 单独处理。
     *
     * @param mediaResource 需要预览的图片或视频资源
     */
    @Composable
    fun Preview(mediaResource: MediaResource)

}