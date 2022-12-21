package github.leavesczy.matisse.internal.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.internal.logic.MatissePageAction
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.logic.SelectionSpec
import github.leavesczy.matisse.internal.theme.MatisseTheme
import github.leavesczy.matisse.internal.utils.PermissionUtils
import kotlinx.coroutines.launch

/**
 * @Author: leavesCZY
 * @Date: 2022/5/28 22:28
 * @Desc:
 */
class MatisseActivity : ComponentActivity() {

    private val matisse = SelectionSpec.getMatisse()

    private val captureStrategy: CaptureStrategy
        get() = matisse.captureStrategy

    private val matisseViewModel by viewModels<MatisseViewModel>(factoryProducer = {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MatisseViewModel(
                    application = application,
                    matisse = matisse
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
                val tips = matisse.tips.onWriteExternalStorageDenied
                if (tips.isNotBlank()) {
                    Toast.makeText(this, tips, Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePicture()
            } else {
                val tips = matisse.tips.onCameraDenied
                if (tips.isNotBlank()) {
                    Toast.makeText(this, tips, Toast.LENGTH_SHORT).show()
                }
            }
        }

    private var tempImageUri: Uri? = null

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            val mTempImageUri = tempImageUri
            tempImageUri = null
            if (mTempImageUri != null) {
                lifecycleScope.launch {
                    if (result) {
                        val resource = captureStrategy.loadResource(
                            context = this@MatisseActivity,
                            imageUri = mTempImageUri
                        )
                        if (resource != null) {
                            onSure(listOf(resource))
                        }
                    } else {
                        captureStrategy.onTakePictureCanceled(
                            context = this@MatisseActivity,
                            imageUri = mTempImageUri
                        )
                    }
                }
            }
        }

    private val matissePageAction = MatissePageAction(
        onClickBackMenu = {
            finish()
        }, onRequestCapture = {
            onRequestCapture()
        }, onSureButtonClick = {
            onSure(selectedMediaResources = matisseViewModel.matisseViewState.selectedResources)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MatisseTheme(matisseTheme = matisse.theme) {
                SetSystemUi(previewVisible = matisseViewModel.matissePreviewViewState.visible)
                MatissePage(viewModel = matisseViewModel, pageAction = matissePageAction)
                MatissePreviewPage(viewModel = matisseViewModel)
            }
        }
        requestReadImagesPermission()
    }

    private fun requestReadImagesPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
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

    private fun onRequestCapture() {
        if (captureStrategy.shouldRequestWriteExternalStoragePermission(context = this)) {
            requestWriteExternalStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            requestCameraPermissionIfNeed()
        }
    }

    private fun requestCameraPermissionIfNeed() {
        if (PermissionUtils.containsPermission(
                context = this,
                permission = Manifest.permission.CAMERA
            ) && !PermissionUtils.checkSelfPermission(
                context = this,
                permission = Manifest.permission.CAMERA
            )
        ) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            takePicture()
        }
    }

    private fun takePicture() {
        lifecycleScope.launch {
            tempImageUri = null
            val imageUri = captureStrategy.createImageUri(context = this@MatisseActivity)
            if (imageUri != null) {
                val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (captureIntent.resolveActivity(packageManager) != null) {
                    takePictureLauncher.launch(imageUri)
                    tempImageUri = imageUri
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

}