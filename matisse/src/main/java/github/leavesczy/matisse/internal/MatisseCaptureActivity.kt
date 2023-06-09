package github.leavesczy.matisse.internal

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.MatisseCaptureContract
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @Author: CZY
 * @Date: 2023/4/11 16:31
 * @Desc:
 */
class MatisseCaptureActivity : AppCompatActivity() {

    private val matisseCapture by lazy {
        MatisseCaptureContract.getRequest(intent = intent)
    }

    private val captureStrategy: CaptureStrategy
        get() = matisseCapture.captureStrategy

    private val requestWriteExternalStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requestCameraPermissionIfNeed()
            } else {
                showToast(getString(R.string.matisse_write_external_storage_permission_denied))
                canceled()
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePicture()
            } else {
                showToast(getString(R.string.matisse_camera_permission_denied))
                canceled()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { successful ->
            takePictureResult(successful = successful)
        }

    private var tempImageUriForTakePicture: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestTakePicture()
    }

    private fun requestTakePicture() {
        if (captureStrategy.shouldRequestWriteExternalStoragePermission(context = this)) {
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
            tempImageUriForTakePicture = null
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (captureIntent.resolveActivity(packageManager) != null) {
                val imageUri = captureStrategy.createImageUri(context = applicationContext)
                if (imageUri != null) {
                    tempImageUriForTakePicture = imageUri
                    takePictureLauncher.launch(imageUri)
                    return@launch
                }
            } else {
                showToast(message = getString(R.string.matisse_no_apps_support_take_picture))
            }
            canceled()
        }
    }

    private fun takePictureResult(successful: Boolean) {
        lifecycleScope.launch(context = Dispatchers.Main.immediate) {
            val imageUri = tempImageUriForTakePicture
            if (imageUri != null) {
                tempImageUriForTakePicture = null
                if (successful) {
                    val resource = captureStrategy.loadResource(
                        context = applicationContext,
                        imageUri = imageUri
                    )
                    if (resource != null) {
                        dispatchResults(mediaResource = resource)
                        return@launch
                    }
                } else {
                    captureStrategy.onTakePictureCanceled(
                        context = applicationContext,
                        imageUri = imageUri
                    )
                }
            }
            canceled()
        }
    }

    private fun dispatchResults(mediaResource: MediaResource) {
        val data = MatisseCaptureContract.buildResult(mediaResource = mediaResource)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun canceled() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun showToast(message: String) {
        if (message.isNotBlank()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

}