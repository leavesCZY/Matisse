package github.leavesczy.matisse.samples

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

/**
 * @Author: leavesCZY
 * @Date: 2022/5/29 21:10
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
class MatisseApp : Application() {

    override fun onCreate() {
        super.onCreate()
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        init(context = this)
    }

    private fun init(context: Context) {
        val imageLoader = ImageLoader.Builder(context)
            .crossfade(false)
            .allowHardware(false)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
        Coil.setImageLoader(imageLoader)
    }

}