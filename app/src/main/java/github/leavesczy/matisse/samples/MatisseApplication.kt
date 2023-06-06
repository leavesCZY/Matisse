package github.leavesczy.matisse.samples

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder

/**
 * @Author: leavesCZY
 * @Date: 2022/5/29 21:10
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
class MatisseApplication : Application() {

    private val Context.isSystemInDarkTheme: Boolean
        get() = resources.configuration.isSystemInDarkTheme

    private val Configuration.isSystemInDarkTheme: Boolean
        get() = uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(
            if (isSystemInDarkTheme) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
        initCoil(context = this)
    }

    private fun initCoil(context: Context) {
        val imageLoader = ImageLoader.Builder(context = context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
            }
            .build()
        Coil.setImageLoader(imageLoader)
    }

}