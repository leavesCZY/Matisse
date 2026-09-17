package github.leavesczy.matisse.internal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
import github.leavesczy.matisse.internal.ui.MatissePage
import github.leavesczy.matisse.internal.ui.MatissePreviewPage
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
        registerForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            matisseViewModel.onReadMediaPermissionResult(
                granted = hasFullReadMediaPermission() || hasPartialReadMediaPermission()
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
                    matisseViewModel.previewPageViewState.isVisible
                }.collectLatest {
                    setSystemBarUi(previewPageVisible = it)
                }
            }
            MatisseTheme {
                MatissePage(
                    pageViewState = matisseViewModel.pageViewState,
                    bottomBarViewState = matisseViewModel.bottomBarViewState,
                    isSelectionLimitReached = matisseViewModel.isSelectionLimitReached,
                    onCaptureClick = ::requestCapture,
                    onConfirmClick = ::onConfirmClick,
                    onFastSelectMediaClick = ::onFastSelectMediaClick
                )
                MatissePreviewPage(
                    pageViewState = matisseViewModel.previewPageViewState,
                    imageEngine = matisseViewModel.pageViewState.matisse.imageEngine,
                    isSelectionLimitReached = matisseViewModel.isSelectionLimitReached,
                    onConfirmClick = ::onConfirmClick
                )
                MatisseVideoPlayerPage(
                    pageViewState = matisseViewModel.videoPlayerPageViewState
                )
            }
        }
        requestReadMediaPermission()
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
        return if (usesGranularMediaPermissions()) {
            buildList {
                val mediaType = matisseViewModel.mediaType
                if (mediaType.includesImage) {
                    add(element = Manifest.permission.READ_MEDIA_IMAGES)
                }
                if (mediaType.includesVideo) {
                    add(element = Manifest.permission.READ_MEDIA_VIDEO)
                }
                if (supportsPartialMediaPermission()) {
                    add(element = Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                }
            }.toTypedArray()
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun hasFullReadMediaPermission(): Boolean {
        val fullAccessPermissions = if (usesGranularMediaPermissions()) {
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
        return permissionGranted(context = this, permissions = fullAccessPermissions)
    }

    private fun hasPartialReadMediaPermission(): Boolean {
        return supportsPartialMediaPermission() && permissionGranted(
            context = this,
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
                containsPartialMediaPermissionInManifest()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun containsPartialMediaPermissionInManifest(): Boolean {
        val packageInfo = packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        )
        return packageInfo.requestedPermissions?.contains(
            element = Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == true
    }

    override fun onCapturedMedia(mediaResource: MediaResource) {
        matisseViewModel.onMediaCaptured(mediaResource = mediaResource)
    }

    private fun onConfirmClick() {
        val selectedMedia = matisseViewModel.getSelectedMedia()
        if (matisseViewModel.singleMediaType) {
            val includesImage = selectedMedia.any { it.isImage }
            val includesVideo = selectedMedia.any { it.isVideo }
            if (includesImage && includesVideo) {
                showToast(id = R.string.matisse_error_mixed_media)
                return
            }
        }
        finishWithSelectedMedia(result = selectedMedia)
    }

    private fun onFastSelectMediaClick(mediaResource: MediaResource) {
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

    override fun onCaptureCancelled() {

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
