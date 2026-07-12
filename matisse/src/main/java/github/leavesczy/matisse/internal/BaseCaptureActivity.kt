package github.leavesczy.matisse.internal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseTakePictureContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal abstract class BaseCaptureActivity : AppCompatActivity() {

    protected abstract val captureStrategy: CaptureStrategy

    private val requestWriteExternalStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requestCameraPermissionIfNeeded()
            } else {
                showToast(id = R.string.matisse_error_write_storage_permission)
                onTakePictureCancelled()
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePicture()
            } else {
                showToast(id = R.string.matisse_error_camera_permission)
                onTakePictureCancelled()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(MatisseTakePictureContract()) { isSuccessful ->
            handleTakePictureResult(isSuccessful = isSuccessful)
        }

    private var pendingCaptureUri: Uri? = null

    protected fun requestTakePicture() {
        if (captureStrategy.shouldRequestWriteExternalStoragePermission(context = applicationContext)) {
            requestWriteExternalStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            requestCameraPermissionIfNeeded()
        }
    }

    private fun requestCameraPermissionIfNeeded() {
        lifecycleScope.launch(context = Dispatchers.Main.immediate) {
            val cameraPermission = Manifest.permission.CAMERA
            val shouldRequestCameraPermission = containsPermission(
                context = applicationContext,
                permission = cameraPermission
            ) && !permissionGranted(
                context = applicationContext,
                permission = cameraPermission
            )
            if (shouldRequestCameraPermission) {
                requestCameraPermissionLauncher.launch(cameraPermission)
            } else {
                takePicture()
            }
        }
    }

    private fun takePicture() {
        lifecycleScope.launch(context = Dispatchers.Main.immediate) {
            pendingCaptureUri = null
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (captureIntent.resolveActivity(packageManager) != null) {
                val imageUri = captureStrategy.createImageUri(context = applicationContext)
                if (imageUri != null) {
                    pendingCaptureUri = imageUri
                    takePictureLauncher.launch(
                        MatisseTakePictureContract.Params(
                            uri = imageUri,
                            extra = captureStrategy.getCaptureExtra()
                        )
                    )
                    return@launch
                }
            } else {
                showToast(id = R.string.matisse_error_no_camera_app)
            }
            onTakePictureCancelled()
        }
    }

    private fun handleTakePictureResult(isSuccessful: Boolean) {
        lifecycleScope.launch(context = Dispatchers.Main.immediate) {
            val imageUri = pendingCaptureUri
            pendingCaptureUri = null
            if (imageUri != null) {
                if (isSuccessful) {
                    val capturedMedia = captureStrategy.loadCapturedMedia(
                        context = applicationContext,
                        imageUri = imageUri
                    )
                    if (capturedMedia != null) {
                        onCapturedMedia(mediaResource = capturedMedia)
                        return@launch
                    }
                }
                captureStrategy.onTakePictureCancelled(
                    context = applicationContext,
                    imageUri = imageUri
                )
            }
            onTakePictureCancelled()
        }
    }

    protected abstract fun onCapturedMedia(mediaResource: MediaResource)

    protected abstract fun onTakePictureCancelled()

    protected fun permissionGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            permissionGranted(context = context, permission = it)
        }
    }

    private fun permissionGranted(context: Context, permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun containsPermission(context: Context, permission: String): Boolean {
        return withContext(context = Dispatchers.Default) {
            try {
                val packageManager: PackageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_PERMISSIONS
                )
                val permissions = packageInfo.requestedPermissions
                if (!permissions.isNullOrEmpty()) {
                    return@withContext permissions.contains(permission)
                }
            } catch (exception: PackageManager.NameNotFoundException) {
                exception.printStackTrace()
            }
            return@withContext false
        }
    }

    protected fun showToast(@StringRes id: Int) {
        showToast(text = getString(id))
    }

    protected fun showToast(text: String) {
        if (text.isNotBlank()) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

}