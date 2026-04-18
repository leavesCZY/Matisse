package github.leavesczy.matisse.samples

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import github.leavesczy.matisse.MatisseCaptureContract
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.samples.logic.MainViewModel
import github.leavesczy.matisse.samples.theme.MatisseTheme

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 12:01
 * @Desc:
 */
class MainActivity : AppCompatActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setSystemBarUi()
        super.onCreate(savedInstanceState)
        setContent {
            val takePictureLauncher =
                rememberLauncherForActivityResult(contract = MatisseCaptureContract()) {
                    mainViewModel.takePictureResult(result = it)
                }
            val mediaPickerLauncher =
                rememberLauncherForActivityResult(contract = MatisseContract()) {
                    mainViewModel.mediaPickerResult(result = it)
                }
            MatisseTheme {
                MainPage(
                    pageViewState = mainViewModel.pageViewState,
                    onClickImageAndVideo = {
                        mediaPickerLauncher.launch(
                            mainViewModel.buildMatisse(mediaType = MediaType.ImageAndVideo)
                        )
                    },
                    onClickImageOnly = {
                        mediaPickerLauncher.launch(
                            mainViewModel.buildMatisse(mediaType = MediaType.ImageOnly)
                        )
                    },
                    onClickVideoOnly = {
                        mediaPickerLauncher.launch(input = mainViewModel.buildMatisse(mediaType = MediaType.VideoOnly))
                    },
                    onClickGifAndMp4 = {
                        mediaPickerLauncher.launch(
                            input = mainViewModel.buildMatisse(
                                mediaType = MediaType.MultipleMimeType(
                                    mimeTypes = setOf("image/gif", "video/mp4")
                                )
                            )
                        )
                    },
                    onClickTakePicture = {
                        val matisseCapture = mainViewModel.buildMediaCaptureStrategy()
                        if (matisseCapture != null) {
                            takePictureLauncher.launch(input = matisseCapture)
                        }
                    }
                )
            }
        }
    }

    private fun setSystemBarUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

}