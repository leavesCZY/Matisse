package github.leavesczy.matisse.internal

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseTakePictureContract
import github.leavesczy.matisse.internal.logic.MatisseTakePictureContractParams
import github.leavesczy.matisse.internal.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @Author: leavesCZY
 * @Date: 2023/10/21 16:49
 * @Desc:
 */
internal abstract class BaseMatisseCaptureActivity : AppCompatActivity() {

    protected abstract val captureStrategy: CaptureStrategy

    private val requestWriteExternalStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requestCameraPermissionIfNeed()
            } else {
                showToast(id = R.string.matisse_write_external_storage_permission_denied)
                onCancelTakePicture()
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePicture()
            } else {
                showToast(id = R.string.matisse_camera_permission_denied)
                onCancelTakePicture()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(MatisseTakePictureContract()) { successful ->
            takePictureResult(successful = successful)
        }

    private var tempImageUriForTakePicture: Uri? = null

    protected fun requestTakePicture() {
        if (captureStrategy.shouldRequestWriteExternalStoragePermission(context = this)) {
            requestWriteExternalStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            requestCameraPermissionIfNeed()
        }
    }

    private fun requestCameraPermissionIfNeed() {
        lifecycleScope.launch(context = Dispatchers.Main.immediate) {
            val cameraPermission = Manifest.permission.CAMERA
            val context = this@BaseMatisseCaptureActivity
            val requirePermissionToTakePhotos = PermissionUtils.containsPermission(
                context = context,
                permission = cameraPermission
            ) && !PermissionUtils.permissionGranted(
                context = context,
                permission = cameraPermission
            )
            if (requirePermissionToTakePhotos) {
                requestCameraPermissionLauncher.launch(cameraPermission)
            } else {
                takePicture()
            }
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
                    takePictureLauncher.launch(
                        MatisseTakePictureContractParams(
                            uri = imageUri,
                            extra = captureStrategy.getCaptureExtra()
                        )
                    )
                    return@launch
                }
            } else {
                showToast(id = R.string.matisse_no_apps_support_take_picture)
            }
            onCancelTakePicture()
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
                        dispatchTakePictureResult(mediaResource = resource)
                        return@launch
                    }
                } else {
                    captureStrategy.onTakePictureCanceled(
                        context = applicationContext,
                        imageUri = imageUri
                    )
                }
            }
            onCancelTakePicture()
        }
    }

    protected abstract fun dispatchTakePictureResult(mediaResource: MediaResource)

    protected abstract fun onCancelTakePicture()

    protected fun showToast(@StringRes id: Int) {
        val msg = getString(id)
        if (msg.isNotBlank()) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

}