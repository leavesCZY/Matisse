package github.leavesczy.matisse.internal

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.internal.ui.MatisseTheme
import github.leavesczy.matisse.internal.ui.MatisseVideoViewPage

/**
 * @Author: leavesCZY
 * @Date: 2024/2/18 15:37
 * @Desc:
 */
internal class MatisseVideoViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        val mediaResource = IntentCompat.getParcelableExtra(
            intent,
            MediaResource::class.java.name,
            MediaResource::class.java
        )
        if (mediaResource == null) {
            finish()
            return
        }
        setContent {
            MatisseTheme {
                MatisseVideoViewPage(
                    videoUri = mediaResource.uri,
                    onBack = ::finish
                )
            }
        }
    }

}