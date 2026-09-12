package github.leavesczy.matisse.internal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseTakePictureContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal abstract class BaseCaptureActivity : AppCompatActivity() {

    companion object {

        private const val KEY_PENDING_CAPTURE_URI = "pendingCaptureUri"

        private const val KEY_CAPTURE_IN_PROGRESS = "captureInProgress"

        private const val KEY_AWAITING_CAMERA_RESULT = "awaitingCameraResult"

    }

    protected abstract val captureStrategy: CaptureStrategy

    private val requestWriteExternalStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requestCameraPermissionIfNeeded()
            } else {
                finishCaptureFlowCancelled()
                showToast(id = R.string.matisse_error_write_storage_permission)
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePicture()
            } else {
                finishCaptureFlowCancelled()
                showToast(id = R.string.matisse_error_camera_permission)
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(MatisseTakePictureContract()) { isSuccessful ->
            handleTakePictureResult(isSuccessful = isSuccessful)
        }

    private var pendingCaptureUri: Uri? = null

    private var captureInProgress = false

    private var awaitingCameraResult = false

    private var isFinalizingCapture = false

    protected val hasPendingCapture: Boolean
        get() = pendingCaptureUri != null

    protected val isCaptureInProgress: Boolean
        get() = captureInProgress

    protected val isAwaitingCameraResult: Boolean
        get() = awaitingCameraResult

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            pendingCaptureUri = BundleCompat.getParcelable(
                savedInstanceState,
                KEY_PENDING_CAPTURE_URI,
                Uri::class.java
            )
            captureInProgress = savedInstanceState.getBoolean(KEY_CAPTURE_IN_PROGRESS, false) ||
                    pendingCaptureUri != null
            awaitingCameraResult =
                savedInstanceState.getBoolean(KEY_AWAITING_CAMERA_RESULT, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingCaptureUri?.let {
            outState.putParcelable(KEY_PENDING_CAPTURE_URI, it)
        }
        outState.putBoolean(KEY_CAPTURE_IN_PROGRESS, captureInProgress)
        outState.putBoolean(KEY_AWAITING_CAMERA_RESULT, awaitingCameraResult)
        super.onSaveInstanceState(outState)
    }

    protected fun requestTakePicture() {
        if (captureInProgress) {
            return
        }
        captureInProgress = true
        if (captureStrategy.shouldRequestWriteExternalStoragePermission(context = applicationContext)) {
            requestWriteExternalStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            requestCameraPermissionIfNeeded()
        }
    }

    /**
     * 选择器在配置变更后若仍持有未完成的拍照 Uri，且并非正在等待系统相机结果时，
     * 尝试完成 load 或清理，避免留下 IS_PENDING 记录。
     */
    protected fun resumeInterruptedCaptureFinalize() {
        if (pendingCaptureUri == null || awaitingCameraResult || isFinalizingCapture) {
            return
        }
        captureInProgress = true
        handleTakePictureResult(isSuccessful = true)
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
            val previousCaptureUri = pendingCaptureUri
            if (previousCaptureUri != null) {
                pendingCaptureUri = null
                awaitingCameraResult = false
                withContext(context = NonCancellable) {
                    captureStrategy.onTakePictureCancelled(
                        context = applicationContext,
                        imageUri = previousCaptureUri
                    )
                }
            }
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (captureIntent.resolveActivity(packageManager) != null) {
                val imageUri = captureStrategy.createImageUri(context = applicationContext)
                if (imageUri != null) {
                    pendingCaptureUri = imageUri
                    awaitingCameraResult = true
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
            finishCaptureFlowCancelled()
        }
    }

    private fun handleTakePictureResult(isSuccessful: Boolean) {
        if (isFinalizingCapture) {
            return
        }
        val imageUri = pendingCaptureUri
        if (imageUri == null) {
            finishCaptureFlowCancelled()
            return
        }
        awaitingCameraResult = false
        isFinalizingCapture = true
        lifecycleScope.launch(context = Dispatchers.Main.immediate) {
            try {
                val capturedMedia = withContext(context = NonCancellable) {
                    if (isSuccessful) {
                        val media = captureStrategy.loadCapturedMedia(
                            context = applicationContext,
                            imageUri = imageUri
                        )
                        if (media != null) {
                            return@withContext media
                        }
                    }
                    captureStrategy.onTakePictureCancelled(
                        context = applicationContext,
                        imageUri = imageUri
                    )
                    null
                }
                pendingCaptureUri = null
                captureInProgress = false
                if (capturedMedia != null) {
                    onCapturedMedia(mediaResource = capturedMedia)
                } else {
                    onTakePictureCancelled()
                }
            } finally {
                isFinalizingCapture = false
            }
        }
    }

    private fun finishCaptureFlowCancelled() {
        captureInProgress = false
        awaitingCameraResult = false
        onTakePictureCancelled()
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