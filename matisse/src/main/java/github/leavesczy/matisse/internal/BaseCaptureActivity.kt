package github.leavesczy.matisse.internal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
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
import github.leavesczy.matisse.internal.logic.MatisseCaptureIntentContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

internal abstract class BaseCaptureActivity : AppCompatActivity() {

    protected abstract val captureStrategy: CaptureStrategy

    private val requestWriteExternalStoragePermissionLauncher =
        registerForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requestCameraPermissionIfNeeded()
            } else {
                completeCaptureCancelled()
                showToast(id = R.string.matisse_error_write_storage_permission)
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                completeCaptureCancelled()
                showToast(id = R.string.matisse_error_camera_permission)
            }
        }

    private val captureLauncher =
        registerForActivityResult(contract = MatisseCaptureIntentContract()) { isSuccessful ->
            onCameraResult(isSuccessful = isSuccessful)
        }

    private var captureSession: CaptureSession = CaptureSession.Idle

    protected val isCaptureSessionIdle: Boolean
        get() = captureSession is CaptureSession.Idle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            captureSession = savedInstanceState.captureSession
            when (val session = captureSession) {
                is CaptureSession.Idle,
                is CaptureSession.AwaitingCamera -> {

                }

                is CaptureSession.RequestingPermission -> {
                    captureSession = CaptureSession.Idle
                }

                is CaptureSession.Finalizing -> {
                    finalizeCapture(outputUri = session.outputUri, isSuccessful = true)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.captureSession = captureSession
        super.onSaveInstanceState(outState)
    }

    protected fun requestCapture() {
        if (captureSession !is CaptureSession.Idle) {
            return
        }
        captureSession = CaptureSession.RequestingPermission
        if (captureStrategy.shouldRequestWriteExternalStoragePermission(context = applicationContext)) {
            requestWriteExternalStoragePermissionLauncher.launch(input = Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            requestCameraPermissionIfNeeded()
        }
    }

    private fun requestCameraPermissionIfNeeded() {
        lifecycleScope.launch(context = Dispatchers.Main.immediate) {
            if (captureSession !is CaptureSession.RequestingPermission) {
                return@launch
            }
            val cameraPermission = Manifest.permission.CAMERA
            val shouldRequestCameraPermission = containsPermission(
                context = applicationContext,
                permission = cameraPermission
            ) && !permissionGranted(
                context = applicationContext,
                permission = cameraPermission
            )
            if (shouldRequestCameraPermission) {
                requestCameraPermissionLauncher.launch(input = cameraPermission)
            } else {
                launchCamera()
            }
        }
    }

    private fun launchCamera() {
        lifecycleScope.launch(context = Dispatchers.Main.immediate) {
            if (captureSession !is CaptureSession.RequestingPermission) {
                return@launch
            }
            val previousOutputUri = captureSession.outputUriOrNull()
            if (previousOutputUri != null) {
                withContext(context = NonCancellable) {
                    captureStrategy.deleteImageUri(
                        context = applicationContext,
                        imageUri = previousOutputUri
                    )
                }
            }
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (captureIntent.resolveActivity(packageManager) == null) {
                showToast(id = R.string.matisse_error_no_camera_app)
                completeCaptureCancelled()
                return@launch
            }
            val imageUri = withContext(context = NonCancellable) {
                val uri = captureStrategy.createImageUri(context = applicationContext)
                if (uri != null) {
                    captureSession = CaptureSession.AwaitingCamera(outputUri = uri)
                }
                uri
            }
            if (imageUri != null) {
                captureLauncher.launch(input = imageUri)
            } else {
                completeCaptureCancelled()
            }
        }
    }

    private fun onCameraResult(isSuccessful: Boolean) {
        val session = captureSession
        if (session !is CaptureSession.AwaitingCamera) {
            return
        }
        finalizeCapture(outputUri = session.outputUri, isSuccessful = isSuccessful)
    }

    private fun finalizeCapture(outputUri: Uri, isSuccessful: Boolean) {
        captureSession = CaptureSession.Finalizing(outputUri = outputUri)
        lifecycleScope.launch(context = Dispatchers.Main.immediate) {
            val capturedMedia = withContext(context = NonCancellable) {
                if (isSuccessful) {
                    val media = captureStrategy.loadCapturedMedia(
                        context = applicationContext,
                        imageUri = outputUri
                    )
                    if (media != null) {
                        return@withContext media
                    }
                }
                captureStrategy.deleteImageUri(
                    context = applicationContext,
                    imageUri = outputUri
                )
                null
            }
            completeCapture(mediaResource = capturedMedia)
        }
    }

    private fun completeCapture(mediaResource: MediaResource?) {
        captureSession = CaptureSession.Idle
        if (mediaResource != null) {
            onCapturedMedia(mediaResource = mediaResource)
        } else {
            onCaptureCancelled()
        }
    }

    private fun completeCaptureCancelled() {
        captureSession = CaptureSession.Idle
        onCaptureCancelled()
    }

    /** 拍照成功并得到有效 [MediaResource] 后的 Activity 侧处理（回传结果或结束选择器）。 */
    protected abstract fun onCapturedMedia(mediaResource: MediaResource)

    /**
     * 拍照流程取消或结果无效时的 Activity 侧处理（例如结束 Activity）。
     * 不等于 [CaptureStrategy.deleteImageUri]：后者负责清理拍照输出资源。
     */
    protected abstract fun onCaptureCancelled()

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
                    return@withContext permissions.contains(element = permission)
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
        if (text.isBlank()) {
            return
        }
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

}

private enum class CapturePhase {
    Idle,
    RequestingPermission,
    AwaitingCamera,
    Finalizing
}

private sealed class CaptureSession {
    data object Idle : CaptureSession()
    data object RequestingPermission : CaptureSession()
    data class AwaitingCamera(val outputUri: Uri) : CaptureSession()
    data class Finalizing(val outputUri: Uri) : CaptureSession()

    fun outputUriOrNull(): Uri? {
        return when (this) {
            is AwaitingCamera -> outputUri
            is Finalizing -> outputUri
            Idle, RequestingPermission -> null
        }
    }

    fun toSavedState(): SavedCaptureSession {
        return SavedCaptureSession(
            phase = when (this) {
                Idle -> CapturePhase.Idle
                RequestingPermission -> CapturePhase.RequestingPermission
                is AwaitingCamera -> CapturePhase.AwaitingCamera
                is Finalizing -> CapturePhase.Finalizing
            },
            outputUri = outputUriOrNull()
        )
    }
}

@Parcelize
private data class SavedCaptureSession(
    val phase: CapturePhase,
    val outputUri: Uri? = null
) : Parcelable {

    fun toCaptureSession(): CaptureSession {
        return when (phase) {
            CapturePhase.AwaitingCamera -> {
                if (outputUri != null) {
                    CaptureSession.AwaitingCamera(outputUri = outputUri)
                } else {
                    CaptureSession.Idle
                }
            }

            CapturePhase.Finalizing -> {
                if (outputUri != null) {
                    CaptureSession.Finalizing(outputUri = outputUri)
                } else {
                    CaptureSession.Idle
                }
            }

            CapturePhase.RequestingPermission -> {
                CaptureSession.RequestingPermission
            }

            CapturePhase.Idle -> {
                CaptureSession.Idle
            }
        }
    }

}

private var Bundle.captureSession: CaptureSession
    get() {
        val saved = BundleCompat.getParcelable(
            this,
            SavedCaptureSession::class.java.name,
            SavedCaptureSession::class.java
        )
        return saved?.toCaptureSession() ?: CaptureSession.Idle
    }
    set(value) {
        putParcelable(SavedCaptureSession::class.java.name, value.toSavedState())
    }
