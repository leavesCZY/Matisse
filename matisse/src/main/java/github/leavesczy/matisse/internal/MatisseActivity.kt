package github.leavesczy.matisse.internal

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.collectAsLazyPagingItems
import github.leavesczy.matisse.CaptureStrategy
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.ui.MatissePage
import github.leavesczy.matisse.internal.ui.MatissePreviewPage
import github.leavesczy.matisse.internal.ui.MatisseTheme
import github.leavesczy.matisse.internal.ui.rememberMatisseGalleryItems
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
        registerForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            matisseViewModel.onReadMediaPermissionResult(
                granted = hasFullReadMediaPermission() || hasPartialReadMediaPermission()
            )
        }

    override val captureStrategy: CaptureStrategy?
        get() {
            if (matisse == null) {
                return null
            }
            return matisseViewModel.captureStrategy
        }

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
                    matisseViewModel.previewPageViewState.isVisible
                }.collectLatest {
                    setSystemBarUi(previewPageVisible = it)
                }
            }
            MatisseTheme {
                val pageViewState = matisseViewModel.pageViewState
                // 列表页与预览页共用同一个 LazyPagingItems，任一侧触发的分页加载两边同时可见
                val pagingItems = matisseViewModel.mediaPagingDataFlow.collectAsLazyPagingItems()
                val galleryItems = rememberMatisseGalleryItems(
                    capturedMediaItems = pageViewState.visibleCapturedMediaItems,
                    pagingItems = pagingItems
                )
                MatissePage(
                    pageViewState = pageViewState,
                    galleryItems = galleryItems,
                    bottomBarViewState = matisseViewModel.bottomBarViewState,
                    onCaptureClick = ::requestCapture,
                    onConfirmClick = ::onConfirmClick,
                    onReturnOnTapMediaClick = ::onReturnOnTapMediaClick
                )
                MatissePreviewPage(
                    pageViewState = matisseViewModel.previewPageViewState,
                    galleryItems = galleryItems,
                    imageEngine = pageViewState.matisse.imageEngine,
                    onConfirmClick = ::onConfirmClick
                )
            }
        }
        requestReadMediaPermission()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // uiMode 由本 Activity 自行处理，深浅色切换后需按新资源重设系统栏图标颜色
        if (matisse != null) {
            setSystemBarUi(previewPageVisible = matisseViewModel.previewPageViewState.isVisible)
        }
    }

    private fun requestReadMediaPermission() {
        if (matisseViewModel.isReadMediaPermissionInitialized) {
            return
        }
        val permissions = buildReadMediaPermissions()
        if (hasFullReadMediaPermission()) {
            matisseViewModel.onReadMediaPermissionResult(granted = true)
        } else {
            requestReadMediaPermissionLauncher.launch(input = permissions)
        }
    }

    private fun buildReadMediaPermissions(): Array<String> {
        val fullAccessPermissions = buildFullReadMediaPermissions()
        return if (supportsPartialMediaPermission()) {
            fullAccessPermissions + Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        } else {
            fullAccessPermissions
        }
    }

    private fun buildFullReadMediaPermissions(): Array<String> {
        return if (usesGranularMediaPermissions()) {
            buildList {
                val mediaType = matisseViewModel.mediaType
                if (mediaType.includesImage) {
                    add(element = Manifest.permission.READ_MEDIA_IMAGES)
                }
                if (mediaType.includesVideo) {
                    add(element = Manifest.permission.READ_MEDIA_VIDEO)
                }
            }.toTypedArray()
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun hasFullReadMediaPermission(): Boolean {
        return permissionGranted(permissions = buildFullReadMediaPermissions())
    }

    private fun hasPartialReadMediaPermission(): Boolean {
        return supportsPartialMediaPermission() && permissionGranted(
            permissions = arrayOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        )
    }

    private fun usesGranularMediaPermissions(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
    }

    private fun supportsPartialMediaPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                applicationInfo.targetSdkVersion >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                containsPermissionInManifest(
                    permission = Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
    }

    override fun onCapturedMedia(mediaResource: MediaResource) {
        matisseViewModel.onMediaCaptured(mediaResource = mediaResource)
    }

    private fun onConfirmClick() {
        finishWithSelectedMedia(result = matisseViewModel.getSelectedMedia())
    }

    private fun onReturnOnTapMediaClick(mediaResource: MediaResource) {
        finishWithSelectedMedia(result = listOf(element = mediaResource))
    }

    private fun finishWithSelectedMedia(result: List<MediaResource>) {
        val data = Intent()
        val selectedMediaList = arrayListOf<Parcelable>().apply {
            addAll(elements = result)
        }
        data.putParcelableArrayListExtra(MediaResource::class.java.name, selectedMediaList)
        setResult(RESULT_OK, data)
        finish()
    }

    private fun finishWithCanceledResult() {
        setResult(RESULT_CANCELED)
        finish()
    }

    // 图库选择器内取消拍照只需结束会话；列表页继续展示，无需额外 UI
    override fun onCaptureCancelled() = Unit

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
