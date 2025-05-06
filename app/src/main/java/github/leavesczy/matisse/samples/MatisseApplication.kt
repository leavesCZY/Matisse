package github.leavesczy.matisse.samples

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder

/**
 * @Author: leavesCZY
 * @Date: 2022/5/29 21:10
 * @Desc:
 */
class MatisseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        initCoil()
    }

    private fun initCoil() {
        SingletonImageLoader.setSafe(factory = { context ->
            ImageLoader
                .Builder(context = context)
                .crossfade(enable = false)
                .allowHardware(enable = true)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(AnimatedImageDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                    add(VideoFrameDecoder.Factory())
                }
                .build()
        })
    }

}