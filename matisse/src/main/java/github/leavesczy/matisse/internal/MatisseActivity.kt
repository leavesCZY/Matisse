package github.leavesczy.matisse.internal

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
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
import github.leavesczy.matisse.ImageMimeTypePrefix
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.R
import github.leavesczy.matisse.VideoMimeTypePrefix
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.ui.MatisseLoadingDialog
import github.leavesczy.matisse.internal.ui.MatissePage
import github.leavesczy.matisse.internal.ui.MatissePreviewPage
import github.leavesczy.matisse.internal.ui.MatisseTheme

/**
 * @Author: leavesCZY
 * @Date: 2022/5/28 22:28
 * @Desc:
 */
internal class MatisseActivity : BaseCaptureActivity() {

    override val captureStrategy: CaptureStrategy
        get() = requireCaptureStrategy()

    private val matisseViewModel by viewModels<MatisseViewModel>(factoryProducer = {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MatisseViewModel(
                    application = application,
                    matisse = intent.getParcelableExtra(Matisse::class.java.name)!!
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
                    matisseViewModel = matisseViewModel,
                    onRequestTakePicture = ::requestTakePicture,
                    onClickSure = ::onClickSure
                )
                MatissePreviewPage(
                    pageViewState = matisseViewModel.matissePreviewPageViewState,
                    onClickSure = ::onClickSure,
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
            val mimeTypes = matisseViewModel.mediaFilter.mediaType()
            if (mimeTypes.imageOnly) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else if (mimeTypes.videoOnly) {
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
        if (permissionGranted(context = this, permissions = permissions)) {
            matisseViewModel.requestReadMediaPermissionResult(granted = true)
        } else {
            requestReadMediaPermissionLauncher.launch(permissions)
        }
    }

    private val MediaType.imageOnly: Boolean
        get() {
            return when (this) {
                is MediaType.ImageAndVideo, MediaType.VideoOnly -> {
                    false
                }

                is MediaType.ImageOnly -> {
                    true
                }

                is MediaType.SingleMimeType -> {
                    mimeType.startsWith(prefix = ImageMimeTypePrefix)
                }
            }
        }

    private val MediaType.videoOnly: Boolean
        get() {
            return when (this) {
                is MediaType.ImageAndVideo, is MediaType.ImageOnly -> {
                    false
                }

                MediaType.VideoOnly -> {
                    true
                }

                is MediaType.SingleMimeType -> {
                    mimeType.startsWith(prefix = VideoMimeTypePrefix)
                }
            }
        }

    private fun requestOpenVideo(mediaResource: MediaResource) {
        val intent = Intent(this, MatisseVideoViewActivity::class.java)
        intent.putExtra(MediaResource::class.java.simpleName, mediaResource)
        startActivity(intent)
    }

    override fun dispatchTakePictureResult(mediaResource: MediaResource) {
        val maxSelectable = matisseViewModel.maxSelectable
        val selectedResources = matisseViewModel.selectedResources
        val illegalMediaType = matisseViewModel.singleMediaType && selectedResources.any {
            it.isVideo
        }
        if (maxSelectable > 1 && (selectedResources.size in 1..<maxSelectable) && !illegalMediaType) {
            val selectedResourcesMutable = selectedResources.toMutableList()
            selectedResourcesMutable.add(element = mediaResource)
            onSure(selected = selectedResourcesMutable)
        } else {
            onSure(selected = listOf(element = mediaResource))
        }
    }

    override fun takePictureCancelled() {

    }

    private fun requireCaptureStrategy(): CaptureStrategy {
        val captureStrategy = matisseViewModel.captureStrategy
        checkNotNull(captureStrategy)
        return captureStrategy
    }

    private fun onClickSure() {
        val maxSelectable = matisseViewModel.maxSelectable
        val selectedResourcesSize = matisseViewModel.selectedResources.size
        if (selectedResourcesSize > maxSelectable) {
            showToast(
                message = String.format(
                    getString(R.string.matisse_limit_the_number_of_pictures),
                    maxSelectable
                )
            )
            return
        }
        onSure(selected = matisseViewModel.selectedResources)
    }

    private fun onSure(selected: List<MediaResource>) {
        if (selected.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
        } else {
            val data = Intent()
            val resources = arrayListOf<Parcelable>().apply {
                addAll(selected)
            }
            data.putParcelableArrayListExtra(MediaResource::class.java.name, resources)
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