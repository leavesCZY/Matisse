package github.leavesczy.matisse

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

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
    fun Thumbnail(
        modifier: Modifier,
        mediaResource: MediaResource,
        contentScale: ContentScale
    ) {
        Image(
            modifier = modifier,
            mediaResource = mediaResource,
            contentScale = contentScale
        )
    }

    /**
     * 在预览页面加载大图时调用
     */
    @Composable
    fun Image(
        modifier: Modifier,
        mediaResource: MediaResource,
        contentScale: ContentScale
    )

}