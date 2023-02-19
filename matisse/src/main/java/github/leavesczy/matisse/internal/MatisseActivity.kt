package github.leavesczy.matisse.internal

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseAction
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.theme.MatisseTheme
import github.leavesczy.matisse.internal.ui.MatissePage
import github.leavesczy.matisse.internal.ui.MatissePreviewPage
import github.leavesczy.matisse.internal.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    private val requestReadImagesPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            matisseViewModel.onRequestReadImagesPermissionResult(granted = granted)
        }

    private val requestWriteExternalStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requestCameraPermissionIfNeed()
            } else {
                showToast(getString(R.string.matisse_on_write_external_storage_permission_denied))
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePicture()
            } else {
                showToast(getString(R.string.matisse_on_camera_permission_denied))
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { successful ->
            matisseViewModel.takePictureResult(successful = successful)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MatisseTheme {
                SetSystemUi(previewPageVisible = matisseViewModel.matissePreviewViewState.visible)
                MatissePage(
                    viewModel = matisseViewModel,
                    onRequestTakePicture = ::onRequestTakePicture
                )
                MatissePreviewPage(viewModel = matisseViewModel)
            }
        }
        initEvent()
        requestReadImagesPermission()
    }

    private fun initEvent() {
        lifecycleScope.launch(context = Dispatchers.Main.immediate) {
            matisseViewModel.matisseAction.collect {
                when (it) {
                    is MatisseAction.OnSure -> {
                        onSure(selectedMediaResources = it.resources)
                    }
                }
            }
        }
    }

    @Composable
    private fun SetSystemUi(previewPageVisible: Boolean) {
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

    private fun requestReadImagesPermission() {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU) {
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
        if (matisse.captureStrategy.shouldRequestWriteExternalStoragePermission(context = this)) {
            requestWriteExternalStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            requestCameraPermissionIfNeed()
        }
    }

    private fun requestCameraPermissionIfNeed() {
        val cameraPermission = Manifest.permission.CAMERA
        if (PermissionUtils.containsPermission(
                context = this,
                permission = cameraPermission
            ) && !PermissionUtils.checkSelfPermission(context = this, permission = cameraPermission)
        ) {
            requestCameraPermissionLauncher.launch(cameraPermission)
        } else {
            takePicture()
        }
    }

    private fun takePicture() {
        lifecycleScope.launch(context = Dispatchers.Main.immediate) {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (captureIntent.resolveActivity(packageManager) != null) {
                val imageUri = matisseViewModel.createImageUriForTakePicture()
                if (imageUri != null) {
                    takePictureLauncher.launch(imageUri)
                }
            }
        }
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

    private fun showToast(message: String) {
        if (message.isNotBlank()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

}