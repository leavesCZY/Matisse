package github.leavesczy.matisse.internal

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import github.leavesczy.matisse.*
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.theme.MatisseTheme
import github.leavesczy.matisse.internal.ui.MatisseLoadingDialog
import github.leavesczy.matisse.internal.ui.MatissePage
import github.leavesczy.matisse.internal.ui.MatissePreviewPage
import github.leavesczy.matisse.internal.utils.PermissionUtils
import github.leavesczy.matisse.internal.utils.isImage
import github.leavesczy.matisse.internal.utils.isVideo

/**
 * @Author: leavesCZY
 * @Date: 2022/5/28 22:28
 * @Desc:
 */
internal class MatisseActivity : AppCompatActivity() {

    private val matisse by lazy {
        MatisseContract.getRequest(intent = intent)
    }

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

    private val requestReadMediaPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            matisseViewModel.requestReadMediaPermissionResult(granted = result.all { it.value })
        }

    private val takePictureLauncher = registerForActivityResult(MatisseCaptureContract()) {
        if (it != null) {
            onSure(selectedResources = listOf(it))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DisposableEffect(key1 = matisseViewModel.matissePreviewPageViewState.visible) {
                setSystemBarUi(previewPageVisible = matisseViewModel.matissePreviewPageViewState.visible)
                onDispose {

                }
            }
            MatisseTheme {
                MatissePage(
                    matisse = matisse,
                    matissePageViewState = matisseViewModel.matissePageViewState,
                    matisseTopBarViewState = matisseViewModel.matisseTopBarViewState,
                    matisseBottomBarViewState = matisseViewModel.matisseBottomBarViewState,
                    selectedResources = matisseViewModel.selectedResources,
                    onRequestTakePicture = ::requestTakePicture,
                    onSure = ::onSure
                )
                MatissePreviewPage(
                    matisse = matisse,
                    pageViewState = matisseViewModel.matissePreviewPageViewState,
                    onSure = ::onSure,
                    requestOpenVideo = ::requestOpenVideo
                )
                MatisseLoadingDialog(visible = matisseViewModel.loadingDialogVisible)
            }
        }
        requestReadMediaPermission()
    }

    private fun requestReadMediaPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
        ) {
            val mimeTypes = matisse.mediaFilter.supportedMimeTypes()
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
        if (PermissionUtils.permissionGranted(context = this, permissions = permissions)) {
            matisseViewModel.requestReadMediaPermissionResult(granted = true)
        } else {
            requestReadMediaPermissionLauncher.launch(permissions)
        }
    }

    private fun requestTakePicture() {
        takePictureLauncher.launch(MatisseCapture(captureStrategy = matisse.captureStrategy))
    }

    private fun requestOpenVideo(mediaResource: MediaResource) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(mediaResource.uri, "video/*")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showToast(message = getString(R.string.matisse_no_apps_support_video_preview))
        }
    }

    private fun onSure() {
        onSure(selectedResources = matisseViewModel.selectedResources)
    }

    private fun onSure(selectedResources: List<MediaResource>) {
        if (selectedResources.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
        } else {
            val data = MatisseContract.buildResult(selectedMediaResources = selectedResources)
            setResult(Activity.RESULT_OK, data)
        }
        finish()
    }

    private fun setSystemBarUi(previewPageVisible: Boolean) {
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (previewPageVisible) {
                hide(WindowInsetsCompat.Type.statusBars())
            } else {
                show(WindowInsetsCompat.Type.statusBars())
            }
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            isAppearanceLightStatusBars = statusBarDarkIcons
            isAppearanceLightNavigationBars = navigationBarDarkIcons
        }
    }

    private fun showToast(message: String) {
        if (message.isNotBlank()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

}