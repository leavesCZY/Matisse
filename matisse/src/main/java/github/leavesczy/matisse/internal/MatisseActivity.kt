package github.leavesczy.matisse.internal

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.ui.MatisseLoadingDialog
import github.leavesczy.matisse.internal.ui.MatissePage
import github.leavesczy.matisse.internal.ui.MatissePreviewImagePage
import github.leavesczy.matisse.internal.ui.MatisseTheme
import github.leavesczy.matisse.internal.ui.MatisseVideoPlayerPage
import kotlinx.coroutines.flow.collectLatest

internal class MatisseActivity : BaseCaptureActivity() {

    private val matisse by lazy(mode = LazyThreadSafetyMode.NONE) {
        IntentCompat.getParcelableExtra(
            intent,
            Matisse::class.java.name,
            Matisse::class.java
        )
    }

    private val matisseViewModel by viewModels<MatisseViewModel>(factoryProducer = {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MatisseViewModel(
                    application = application,
                    matisse = matisse!!
                ) as T
            }
        }
    })

    private val requestReadMediaPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            matisseViewModel.onReadMediaPermissionResult(
                granted = result.all {
                    it.value
                }
            )
        }

    override val captureStrategy: CaptureStrategy
        get() = requireNotNull(value = matisseViewModel.captureStrategy)

    override fun onCreate(savedInstanceState: Bundle?) {
        setSystemBarUi(previewPageVisible = false)
        super.onCreate(savedInstanceState)
        if (matisse == null) {
            finishWithCanceledResult()
            return
        }
        setContent {
            LaunchedEffect(key1 = Unit) {
                snapshotFlow {
                    matisseViewModel.previewImagePageViewState.visible
                }.collectLatest {
                    setSystemBarUi(previewPageVisible = it)
                }
            }
            MatisseTheme {
                MatissePage(
                    pageViewState = matisseViewModel.pageViewState,
                    bottomBarViewState = matisseViewModel.bottomBarViewState,
                    selectionLimitReached = matisseViewModel.selectionLimitReached,
                    onTakePictureRequest = ::requestTakePicture,
                    onConfirmClick = ::onConfirmClick,
                    onFastSelectMedia = ::onFastSelectMedia
                )
                MatissePreviewImagePage(
                    pageViewState = matisseViewModel.previewImagePageViewState,
                    imageEngine = matisseViewModel.pageViewState.matisse.imageEngine,
                    selectionLimitReached = matisseViewModel.selectionLimitReached,
                    onConfirmClick = ::onConfirmClick
                )
                MatisseVideoPlayerPage(
                    pageViewState = matisseViewModel.videoPlayerPageViewState
                )
                MatisseLoadingDialog(
                    modifier = Modifier,
                    visible = matisseViewModel.loadingDialogVisible
                )
            }
        }
        requestReadMediaPermission()
    }

    private fun requestReadMediaPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
        ) {
            buildList {
                val mediaType = matisseViewModel.mediaType
                if (mediaType.includeImage) {
                    add(element = Manifest.permission.READ_MEDIA_IMAGES)
                }
                if (mediaType.includeVideo) {
                    add(element = Manifest.permission.READ_MEDIA_VIDEO)
                }
            }.toTypedArray()
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissionGranted(context = this, permissions = permissions)) {
            matisseViewModel.onReadMediaPermissionResult(granted = true)
        } else {
            requestReadMediaPermissionLauncher.launch(permissions)
        }
    }

    override fun onCapturedMedia(mediaResource: MediaResource) {
        val maxSelectable = matisseViewModel.maxSelectable
        val selectedMedia = matisseViewModel.getSelectedMedia()
        val wouldMixMediaTypes = matisseViewModel.singleMediaType && selectedMedia.any {
            it.isVideo
        }
        val result =
            if (maxSelectable > 1 && (selectedMedia.size in 1..<maxSelectable) && !wouldMixMediaTypes) {
                val selectedMediaMutable = selectedMedia.toMutableList()
                selectedMediaMutable.add(element = mediaResource)
                selectedMediaMutable
            } else {
                listOf(element = mediaResource)
            }
        finishWithSelectedMedia(result = result)
    }

    private fun onConfirmClick() {
        val selectedMedia = matisseViewModel.getSelectedMedia()
        if (matisseViewModel.singleMediaType) {
            val includeImage = selectedMedia.any { it.isImage }
            val includeVideo = selectedMedia.any { it.isVideo }
            if (includeImage && includeVideo) {
                showToast(id = R.string.matisse_error_mixed_media)
                return
            }
        }
        finishWithSelectedMedia(result = selectedMedia)
    }

    private fun onFastSelectMedia(mediaResource: MediaResource) {
        finishWithSelectedMedia(result = listOf(element = mediaResource))
    }

    private fun finishWithSelectedMedia(result: List<MediaResource>) {
        val data = Intent()
        val selectedMediaList = arrayListOf<Parcelable>().apply {
            addAll(result)
        }
        data.putParcelableArrayListExtra(MediaResource::class.java.name, selectedMediaList)
        setResult(RESULT_OK, data)
        finish()
    }

    private fun finishWithCanceledResult() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onTakePictureCancelled() {

    }

    private fun setSystemBarUi(previewPageVisible: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            val types = WindowInsetsCompat.Type.statusBars()
            if (previewPageVisible) {
                hide(types)
            } else {
                show(types)
            }
            val statusBarDarkIcons: Boolean
            val navigationBarDarkIcons: Boolean
            if (previewPageVisible) {
                statusBarDarkIcons = false
                navigationBarDarkIcons = false
            } else {
                statusBarDarkIcons = resources.getBoolean(R.bool.matisse_status_bar_icons_dark)
                navigationBarDarkIcons =
                    resources.getBoolean(R.bool.matisse_navigation_bar_icons_dark)
            }
            isAppearanceLightStatusBars = statusBarDarkIcons
            isAppearanceLightNavigationBars = navigationBarDarkIcons
        }
    }

}