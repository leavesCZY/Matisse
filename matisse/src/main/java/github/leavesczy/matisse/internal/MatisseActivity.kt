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
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import github.leavesczy.matisse.MatisseCaptureContract
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.theme.MatisseTheme
import github.leavesczy.matisse.internal.ui.MatissePage
import github.leavesczy.matisse.internal.ui.MatissePreviewPage
import github.leavesczy.matisse.internal.utils.PermissionUtils

/**
 * @Author: leavesCZY
 * @Date: 2022/5/28 22:28
 * @Desc:
 */
class MatisseActivity : AppCompatActivity() {

    private val matisse by lazy(mode = LazyThreadSafetyMode.NONE) {
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

    private val requestReadImagesPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            matisseViewModel.onRequestReadImagesPermissionResult(granted = granted)
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
        requestReadImagesPermission()
    }

    private fun requestReadImagesPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
        ) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (PermissionUtils.checkSelfPermission(context = this, permission = permission)) {
            matisseViewModel.onRequestReadImagesPermissionResult(granted = true)
        } else {
            matisseViewModel.onRequestReadImagesPermission()
            requestReadImagesPermissionLauncher.launch(permission)
        }
    }

    private fun onRequestTakePicture() {
        takePictureLauncher.launch(matisse.captureStrategy)
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