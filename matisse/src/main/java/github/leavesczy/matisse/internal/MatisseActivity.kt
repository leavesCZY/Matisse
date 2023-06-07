package github.leavesczy.matisse.internal

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import github.leavesczy.matisse.*
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.theme.MatisseTheme
import github.leavesczy.matisse.internal.ui.MatissePage
import github.leavesczy.matisse.internal.ui.MatissePreviewPage
import github.leavesczy.matisse.internal.ui.rememberSystemUiController
import github.leavesczy.matisse.internal.utils.PermissionUtils

/**
 * @Author: leavesCZY
 * @Date: 2022/5/28 22:28
 * @Desc:
 */
class MatisseActivity : AppCompatActivity() {

    private val matisse by lazy {
        MatisseContract.getRequest(intent = intent)
    }

    private val matisseViewModel by viewModels<MatisseViewModel>(factoryProducer = {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MatisseViewModel(
                    application = application, matisse = matisse
                ) as T
            }
        }
    })

    private val requestReadMediaPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            matisseViewModel.onRequestReadMediaPermissionResult(granted = result.all { it.value })
        }

    private val takePictureLauncher = registerForActivityResult(MatisseCaptureContract()) {
        if (it != null) {
            onSure(selectedMediaResources = listOf(it))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MatisseTheme {
                SetSystemUi(previewPageVisible = matisseViewModel.matissePreviewViewState.visible)
                MatissePage(
                    viewModel = matisseViewModel,
                    onRequestTakePicture = ::onRequestTakePicture,
                    onSure = ::onSure
                )
                MatissePreviewPage(
                    viewModel = matisseViewModel,
                    onSure = ::onSure
                )
            }
        }
        requestReadMediaPermission()
    }

    private fun requestReadMediaPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
        ) {
            val mimeTypes = matisse.mimeTypes
            val onlyImage = mimeTypes.all { it.isImage }
            val onlyVideo = mimeTypes.all { it.isVideo }
            if (onlyImage) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else if (onlyVideo) {
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (PermissionUtils.checkSelfPermission(context = this, permissions = permissions)) {
            matisseViewModel.onRequestReadMediaPermissionResult(granted = true)
        } else {
            matisseViewModel.onRequestReadMediaPermission()
            requestReadMediaPermissionLauncher.launch(permissions)
        }
    }

    private fun onRequestTakePicture() {
        takePictureLauncher.launch(MatisseCapture(captureStrategy = matisse.captureStrategy))
    }

    private fun onSure() {
        val selectedResources = matisseViewModel.matisseViewState.selectedResources
        onSure(selectedMediaResources = selectedResources)
    }

    private fun onSure(selectedMediaResources: List<MediaResource>) {
        if (selectedMediaResources.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
        } else {
            val data = MatisseContract.buildResult(selectedMediaResources = selectedMediaResources)
            setResult(Activity.RESULT_OK, data)
        }
        finish()
    }

}

@Composable
private fun SetSystemUi(previewPageVisible: Boolean) {
    val resources = LocalContext.current.resources
    val statusBarColor = Color.Transparent
    val navigationBarColor = if (previewPageVisible) {
        Color.Transparent
    } else {
        colorResource(id = R.color.matisse_navigation_bar_color)
    }
    val statusBarDarkIcons = if (previewPageVisible) {
        false
    } else {
        resources.getBoolean(R.bool.matisse_status_bar_dark_icons)
    }
    val navigationBarDarkIcons = if (previewPageVisible) {
        false
    } else {
        resources.getBoolean(R.bool.matisse_navigation_bar_dark_icons)
    }
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = statusBarColor,
        darkIcons = statusBarDarkIcons,
    )
    systemUiController.setNavigationBarColor(
        color = navigationBarColor,
        darkIcons = navigationBarDarkIcons,
    )
}