package github.leavesczy.matisse

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * 图片加载引擎。Matisse 库本身不传递 Coil / Glide 依赖，集成方需根据所选实现自行添加对应依赖
 */
@Stable
interface ImageEngine : Parcelable {

    /**
     * 加载图片缩略图或视频封面缩略图时调用
     */
    @Composable
    fun Thumbnail(mediaResource: MediaResource)

    /**
     * 加载大图或预览视频封面时调用
     */
    @Composable
    fun Image(mediaResource: MediaResource)

}