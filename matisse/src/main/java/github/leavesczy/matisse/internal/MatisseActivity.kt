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

/**
 * @Author: leavesCZY
 * @Date: 2022/5/28 22:28
 * @Desc:
 */
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
            matisseViewModel.requestReadMediaPermissionResult(
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
            setResultCanceled()
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
                    onRequestTakePicture = ::requestTakePicture,
                    onClickConfirm = ::onClickConfirm,
                    selectMediaInFastSelectMode = ::selectMediaInFastSelectMode
                )
                MatissePreviewImagePage(
                    pageViewState = matisseViewModel.previewImagePageViewState,
                    imageEngine = matisseViewModel.pageViewState.matisse.imageEngine,
                    onClickConfirm = ::onClickConfirm
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
            matisseViewModel.requestReadMediaPermissionResult(granted = true)
        } else {
            requestReadMediaPermissionLauncher.launch(permissions)
        }
    }

    override fun dispatchTakePictureResult(mediaResource: MediaResource) {
        val maxSelectable = matisseViewModel.maxSelectable
        val selectedResources = matisseViewModel.filterSelectedMedia()
        val illegalMediaType = matisseViewModel.singleMediaType && selectedResources.any {
            it.isVideo
        }
        val result =
            if (maxSelectable > 1 && (selectedResources.size in 1..<maxSelectable) && !illegalMediaType) {
                val selectedResourcesMutable = selectedResources.toMutableList()
                selectedResourcesMutable.add(element = mediaResource)
                selectedResourcesMutable
            } else {
                listOf(element = mediaResource)
            }
        setResult(result = result)
    }

    private fun onClickConfirm() {
        val selectedResources = matisseViewModel.filterSelectedMedia()
        if (matisseViewModel.singleMediaType) {
            val includeImage = selectedResources.any { it.isImage }
            val includeVideo = selectedResources.any { it.isVideo }
            if (includeImage && includeVideo) {
                showToast(id = R.string.matisse_error_mixed_media)
                return
            }
        }
        setResult(result = selectedResources)
    }

    private fun selectMediaInFastSelectMode(mediaResource: MediaResource) {
        setResult(result = listOf(element = mediaResource))
    }

    private fun setResult(result: List<MediaResource>) {
        val data = Intent()
        val resources = arrayListOf<Parcelable>().apply {
            addAll(result)
        }
        data.putParcelableArrayListExtra(MediaResource::class.java.name, resources)
        setResult(RESULT_OK, data)
        finish()
    }

    private fun setResultCanceled() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun takePictureCancelled() {

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