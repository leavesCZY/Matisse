package github.leavesczy.matisse.internal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.SystemCameraContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

/**
 * 拍照流程基类：存储写入权限 → 相机权限 → 启动系统相机 → 收尾（读取结果或清理 Uri）。
 *
 * 流程进度以 [CaptureSession] 表示并保存到 Activity 状态中，Activity 重建或进程被回收后据此续跑；
 * 同一时间只允许一个拍照会话。
 */
internal abstract class BaseCaptureActivity : AppCompatActivity() {

    /**
     * 当前可用的拍照策略。Intent 配置缺失、或选择器未启用拍照时可能为 null。
     * [onCreate] 恢复会话时不得假定其非空。
     */
    protected abstract val captureStrategy: CaptureStrategy?

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
        registerForActivityResult(contract = SystemCameraContract()) { isSuccessful ->
            onCameraResult(isSuccessful = isSuccessful)
        }

    private var captureSession: CaptureSession = CaptureSession.Idle

    private var finalizeJob: Job? = null

    private var requestedPermissionsCache: Array<out String>? = null

    protected val isCaptureSessionIdle: Boolean
        get() = captureSession is CaptureSession.Idle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            captureSession = savedInstanceState.captureSession
            when (val session = captureSession) {
                is CaptureSession.Idle, is CaptureSession.AwaitingCamera -> {
                    // AwaitingCamera：Activity Result 会再次回调 onCameraResult
                }

                is CaptureSession.RequestingPermission -> {
                    // 等 onStart 后再续跑，让 Activity Result 先重放权限回调
                    resumeCapturePermissionFlowAfterStart()
                }

                is CaptureSession.Finalizing -> {
                    // 与进程内已有收尾合并，或在进程重建后重新执行收尾
                    finalizeCapture(
                        outputUri = session.outputUri,
                        isSuccessful = session.isSuccessful
                    )
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
        val strategy = captureStrategy
        if (strategy == null) {
            completeCaptureCancelled()
            return
        }
        CaptureFinalizeCoordinator.onNewCaptureSession()
        captureSession = CaptureSession.RequestingPermission
        proceedCapturePermissionFlow()
    }

    private fun resumeCapturePermissionFlowAfterStart() {
        lifecycle.addObserver(
            observer = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    owner.lifecycle.removeObserver(observer = this)
                    // 下一帧再判断：权限回调若已推进会话则跳过，否则续跑权限链
                    window.decorView.post {
                        if (captureSession is CaptureSession.RequestingPermission) {
                            proceedCapturePermissionFlow()
                        }
                    }
                }
            }
        )
    }

    private fun proceedCapturePermissionFlow() {
        val strategy = captureStrategy
        if (strategy == null) {
            completeCaptureCancelled()
            return
        }
        if (strategy.shouldRequestWriteExternalStoragePermission(context = applicationContext)) {
            requestWriteExternalStoragePermissionLauncher.launch(input = Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            requestCameraPermissionIfNeeded()
        }
    }

    private fun requestCameraPermissionIfNeeded() {
        if (captureSession !is CaptureSession.RequestingPermission) {
            return
        }
        val cameraPermission = Manifest.permission.CAMERA
        val shouldRequestCameraPermission = containsPermissionInManifest(
            permission = cameraPermission
        ) && !permissionGranted(permission = cameraPermission)
        if (shouldRequestCameraPermission) {
            requestCameraPermissionLauncher.launch(input = cameraPermission)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        lifecycleScope.launch {
            if (captureSession !is CaptureSession.RequestingPermission) {
                return@launch
            }
            val strategy = captureStrategy
            if (strategy == null) {
                completeCaptureCancelled()
                return@launch
            }
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (captureIntent.resolveActivity(packageManager) == null) {
                showToast(id = R.string.matisse_error_no_camera_app)
                completeCaptureCancelled()
                return@launch
            }
            val captureUri = withContext(context = NonCancellable) {
                strategy.createCaptureUri(context = applicationContext)
            }
            if (captureUri == null) {
                completeCaptureCancelled()
                return@launch
            }
            try {
                ensureActive()
                if (captureSession !is CaptureSession.RequestingPermission) {
                    discardCaptureOutput(captureUri = captureUri)
                    return@launch
                }
                captureSession = CaptureSession.AwaitingCamera(outputUri = captureUri)
                captureLauncher.launch(input = captureUri)
            } catch (cancellationException: CancellationException) {
                discardCaptureOutput(captureUri = captureUri)
                captureSession = CaptureSession.Idle
                throw cancellationException
            } catch (throwable: Throwable) {
                // resolveActivity 通过后相机仍可能被禁用或拒绝访问（ActivityNotFoundException / SecurityException）
                MatisseLog.e(throwable = throwable)
                discardCaptureOutput(captureUri = captureUri)
                showToast(id = R.string.matisse_error_no_camera_app)
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
        val strategy = captureStrategy
        if (strategy == null) {
            captureSession = CaptureSession.Idle
            onCaptureCancelled()
            return
        }
        val currentSession = captureSession
        if (currentSession is CaptureSession.Finalizing &&
            currentSession.outputUri == outputUri &&
            currentSession.isSuccessful == isSuccessful &&
            finalizeJob?.isActive == true
        ) {
            return
        }
        captureSession = CaptureSession.Finalizing(
            outputUri = outputUri,
            isSuccessful = isSuccessful
        )
        finalizeJob?.cancel()
        finalizeJob = lifecycleScope.launch {
            val mediaResource = try {
                CaptureFinalizeCoordinator.awaitOrRun(
                    outputUri = outputUri,
                    isSuccessful = isSuccessful
                ) {
                    withContext(context = NonCancellable) {
                        if (isSuccessful) {
                            val mediaResource = strategy.loadCapturedMedia(
                                context = applicationContext,
                                captureUri = outputUri
                            )
                            if (mediaResource == null) {
                                // loadCapturedMedia 返回 null 时删除 Uri，避免留下空文件或空相册记录
                                strategy.deleteCaptureUri(
                                    context = applicationContext,
                                    captureUri = outputUri
                                )
                            }
                            mediaResource
                        } else {
                            strategy.deleteCaptureUri(
                                context = applicationContext,
                                captureUri = outputUri
                            )
                            null
                        }
                    }
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                MatisseLog.e(throwable = throwable)
                try {
                    withContext(context = NonCancellable) {
                        strategy.deleteCaptureUri(
                            context = applicationContext,
                            captureUri = outputUri
                        )
                    }
                } catch (deleteThrowable: Throwable) {
                    MatisseLog.e(throwable = deleteThrowable)
                }
                null
            }
            // 已销毁的旧实例在此终止，回调资格只留给仍存活的实例
            ensureActive()
            val session = captureSession
            if (session !is CaptureSession.Finalizing ||
                session.outputUri != outputUri ||
                session.isSuccessful != isSuccessful
            ) {
                return@launch
            }
            val shouldDeliver = CaptureFinalizeCoordinator.markDelivered(
                outputUri = outputUri,
                isSuccessful = isSuccessful
            )
            if (shouldDeliver) {
                completeCapture(mediaResource = mediaResource)
            } else {
                // 同一次收尾已由其它 Activity 实例回调过，仅复位会话，避免卡在 Finalizing
                captureSession = CaptureSession.Idle
            }
        }
    }

    private suspend fun discardCaptureOutput(captureUri: Uri) {
        val strategy = captureStrategy ?: return
        withContext(context = NonCancellable) {
            strategy.deleteCaptureUri(
                context = applicationContext,
                captureUri = captureUri
            )
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

    protected abstract fun onCapturedMedia(mediaResource: MediaResource)

    /**
     * 拍照流程取消或未能得到有效结果时的 Activity 侧处理（例如结束独立拍照 Activity）。
     * 不等于 [CaptureStrategy.deleteCaptureUri]：后者负责清理拍照输出资源。
     */
    protected abstract fun onCaptureCancelled()

    protected fun permissionGranted(permissions: Array<String>): Boolean {
        return permissions.all {
            permissionGranted(permission = it)
        }
    }

    private fun permissionGranted(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 宿主 Manifest 是否声明了 [permission]。PackageInfo 读取成功后在当前 Activity 实例内缓存。
     */
    protected fun containsPermissionInManifest(permission: String): Boolean {
        return loadRequestedPermissions().contains(element = permission)
    }

    private fun loadRequestedPermissions(): Array<out String> {
        requestedPermissionsCache?.let { return it }
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            val permissions = packageInfo.requestedPermissions ?: emptyArray()
            requestedPermissionsCache = permissions
            permissions
        } catch (exception: PackageManager.NameNotFoundException) {
            MatisseLog.e(throwable = exception)
            emptyArray()
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

private sealed class CaptureSession : Parcelable {

    @Parcelize
    data object Idle : CaptureSession()

    @Parcelize
    data object RequestingPermission : CaptureSession()

    @Parcelize
    data class AwaitingCamera(val outputUri: Uri) : CaptureSession()

    @Parcelize
    data class Finalizing(
        val outputUri: Uri,
        val isSuccessful: Boolean
    ) : CaptureSession()

}

private var Bundle.captureSession: CaptureSession
    get() {
        return BundleCompat.getParcelable(
            this,
            CaptureSession::class.java.name,
            CaptureSession::class.java
        ) ?: CaptureSession.Idle
    }
    set(value) {
        putParcelable(CaptureSession::class.java.name, value)
    }

/**
 * 进程内拍照收尾单飞：Activity 重建导致新旧实例都进入 [CaptureSession.Finalizing] 时，
 * 共用同一次 load/delete；[markDelivered] 仅对第一个调用方返回 true，避免重复回调。
 * 状态以 Uri 与相机结果为 key，每次发起新的拍照会话时清空。
 */
private object CaptureFinalizeCoordinator {

    private val lock = Any()

    private var runningKey: String? = null

    private var runningDeferred: CompletableDeferred<MediaResource?>? = null

    private var deliveredKey: String? = null

    fun onNewCaptureSession() {
        synchronized(lock = lock) {
            runningKey = null
            runningDeferred = null
            deliveredKey = null
        }
    }

    /** 已有同 key 的收尾在执行时等待其结果，否则执行 [block]；已回调过的 key 直接返回 null。 */
    suspend fun awaitOrRun(
        outputUri: Uri,
        isSuccessful: Boolean,
        block: suspend () -> MediaResource?
    ): MediaResource? {
        val key = finalizeKey(outputUri = outputUri, isSuccessful = isSuccessful)
        val deferredToAwait: CompletableDeferred<MediaResource?>
        val shouldRun: Boolean
        synchronized(lock = lock) {
            if (deliveredKey == key) {
                return null
            }
            val existingDeferred = runningDeferred
            if (runningKey == key && existingDeferred != null) {
                deferredToAwait = existingDeferred
                shouldRun = false
            } else {
                val deferred = CompletableDeferred<MediaResource?>()
                runningKey = key
                runningDeferred = deferred
                deferredToAwait = deferred
                shouldRun = true
            }
        }
        if (!shouldRun) {
            return deferredToAwait.await()
        }
        try {
            val result = block()
            deferredToAwait.complete(value = result)
            return result
        } catch (throwable: Throwable) {
            deferredToAwait.completeExceptionally(exception = throwable)
            throw throwable
        } finally {
            synchronized(lock = lock) {
                if (runningKey == key) {
                    runningKey = null
                    runningDeferred = null
                }
            }
        }
    }

    fun markDelivered(outputUri: Uri, isSuccessful: Boolean): Boolean {
        val key = finalizeKey(outputUri = outputUri, isSuccessful = isSuccessful)
        synchronized(lock = lock) {
            if (deliveredKey == key) {
                return false
            }
            deliveredKey = key
            return true
        }
    }

    private fun finalizeKey(outputUri: Uri, isSuccessful: Boolean): String {
        return outputUri.toString() + '\u0000' + isSuccessful
    }

}

