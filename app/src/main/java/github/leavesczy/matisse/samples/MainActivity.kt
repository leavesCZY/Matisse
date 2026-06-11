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

class MainActivity : AppCompatActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setSystemBarUi()
        super.onCreate(savedInstanceState)
        setContent {
            val takePictureLauncher =
                rememberLauncherForActivityResult(contract = MatisseCaptureContract()) {
                    mainViewModel.onTakePictureResult(mediaResource = it)
                }
            val mediaPickerLauncher =
                rememberLauncherForActivityResult(contract = MatisseContract()) {
                    mainViewModel.onMediaPickerResult(result = it)
                }
            MatisseTheme(darkTheme = mainViewModel.pageViewState.darkTheme) {
                MainPage(
                    pageViewState = mainViewModel.pageViewState,
                    onPickImageAndVideo = {
                        mediaPickerLauncher.launch(
                            mainViewModel.buildMatisse(mediaType = MediaType.ImageAndVideo)
                        )
                    },
                    onPickImageOnly = {
                        mediaPickerLauncher.launch(
                            mainViewModel.buildMatisse(mediaType = MediaType.ImageOnly)
                        )
                    },
                    onPickVideoOnly = {
                        mediaPickerLauncher.launch(input = mainViewModel.buildMatisse(mediaType = MediaType.VideoOnly))
                    },
                    onPickGifAndMp4 = {
                        mediaPickerLauncher.launch(
                            input = mainViewModel.buildMatisse(
                                mediaType = MediaType.MultipleMimeType(
                                    mimeTypes = setOf("image/gif", "video/mp4")
                                )
                            )
                        )
                    },
                    onTakePictureClick = {
                        val matisseCapture = mainViewModel.buildMatisseCapture()
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