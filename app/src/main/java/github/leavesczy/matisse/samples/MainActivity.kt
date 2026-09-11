package github.leavesczy.matisse.samples

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import github.leavesczy.matisse.MatisseCaptureContract
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.samples.logic.MainViewModel
import github.leavesczy.matisse.samples.theme.MatisseSampleTheme

class MainActivity : AppCompatActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setSystemBarUi()
        super.onCreate(savedInstanceState)
        setContent {
            val pagingWriteStoragePermissionLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) {
                        mainViewModel.insertPagingTestImages()
                    } else {
                        showStoragePermissionDeniedToast()
                    }
                }
            val imageEngineWriteStoragePermissionLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) {
                        mainViewModel.insertImageEngineTestImages()
                    } else {
                        showStoragePermissionDeniedToast()
                    }
                }
            val takePictureLauncher =
                rememberLauncherForActivityResult(contract = MatisseCaptureContract()) {
                    mainViewModel.onTakePictureResult(mediaResource = it)
                }
            val mediaPickerLauncher =
                rememberLauncherForActivityResult(contract = MatisseContract()) {
                    mainViewModel.onMediaPickerResult(result = it)
                }
            MatisseSampleTheme(darkTheme = mainViewModel.pageViewState.darkTheme) {
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
                    },
                    onInsertPagingTestImages = {
                        if (needsLegacyWriteStoragePermission()) {
                            pagingWriteStoragePermissionLauncher.launch(
                                input = Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        } else {
                            mainViewModel.insertPagingTestImages()
                        }
                    },
                    onInsertImageEngineTestImages = {
                        if (needsLegacyWriteStoragePermission()) {
                            imageEngineWriteStoragePermissionLauncher.launch(
                                input = Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        } else {
                            mainViewModel.insertImageEngineTestImages()
                        }
                    }
                )
            }
        }
    }

    private fun needsLegacyWriteStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
    }

    private fun showStoragePermissionDeniedToast() {
        Toast.makeText(
            this,
            "需要存储写入权限才能插入测试图片",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun setSystemBarUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

}