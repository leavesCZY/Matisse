package github.leavesczy.matisse.internal

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.theme.MatisseTheme
import github.leavesczy.matisse.internal.ui.MatisseLoadingDialog
import github.leavesczy.matisse.internal.ui.MatissePage
import github.leavesczy.matisse.internal.ui.MatissePreviewPage
import github.leavesczy.matisse.internal.utils.PermissionUtils

/**
 * @Author: leavesCZY
 * @Date: 2022/5/28 22:28
 * @Desc:
 */
internal class MatisseActivity : BaseCaptureActivity() {

    private val matisse by lazy(mode = LazyThreadSafetyMode.NONE) {
        MatisseContract.getRequest(intent = intent)
    }

    override val captureStrategy: CaptureStrategy
        get() = requireCaptureStrategy()

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

    private fun requestOpenVideo(mediaResource: MediaResource) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(mediaResource.uri, "video/*")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showToast(id = R.string.matisse_no_apps_support_video_preview)
        }
    }

    override fun dispatchTakePictureResult(mediaResource: MediaResource) {
        val maxSelectable = matisse.maxSelectable
        val selectedResources = matisseViewModel.selectedResources
        val selectedResourcesSize = selectedResources.size
        if (maxSelectable > 1 && (selectedResourcesSize in 1..<maxSelectable)) {
            val selectedResourcesMutable = selectedResources.toMutableList()
            selectedResourcesMutable.add(element = mediaResource)
            onSure(resources = selectedResourcesMutable)
        } else {
            onSure(resources = listOf(mediaResource))
        }
    }

    override fun takePictureCancelled() {

    }

    private fun requireCaptureStrategy(): CaptureStrategy {
        val captureStrategy = matisse.captureStrategy
        checkNotNull(captureStrategy)
        return captureStrategy
    }

    private fun onSure() {
        onSure(resources = matisseViewModel.selectedResources)
    }

    private fun onSure(resources: List<MediaResource>) {
        if (resources.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
        } else {
            val data = MatisseContract.buildResult(selectedMediaResources = resources)
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

}