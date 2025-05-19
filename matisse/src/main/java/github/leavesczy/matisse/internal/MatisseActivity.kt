package github.leavesczy.matisse.internal

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import github.leavesczy.matisse.internal.custom.PermissionAbout
import github.leavesczy.matisse.internal.custom.SettingsActivityResultContract
import github.leavesczy.matisse.internal.custom.checkPermissionCustom
import github.leavesczy.matisse.internal.custom.checkPermissionResultCustom
import github.leavesczy.matisse.internal.custom.requestReadMediaPermissionCustom
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.ui.MatisseLoadingDialog
import github.leavesczy.matisse.internal.ui.MatissePage
import github.leavesczy.matisse.internal.ui.MatissePreviewPage
import github.leavesczy.matisse.internal.ui.MatisseTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive

/**
 * @Author: leavesCZY
 * @Date: 2022/5/28 22:28
 * @Desc:
 */
internal class MatisseActivity : BaseCaptureActivity() {

    private val showPermissionDialog: MutableState<Boolean> = mutableStateOf(false)
    private val permissionState: MutableState<String> = mutableStateOf("")
    private var scope = CoroutineScope(Dispatchers.Default)
    private val requestReadMediaPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (scope.isActive) {
                scope.cancel()
            }
            showPermissionDialog.value = false
            matisseViewModel.requestReadMediaPermissionResult(granted = result.any { it.value })
            permissionState.value = checkPermissionCustom(this)
        }

    private val matisseViewModel by viewModels<MatisseViewModel>(factoryProducer = {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MatisseViewModel(
                    application = application,
                    matisse = IntentCompat.getParcelableExtra(
                        intent,
                        Matisse::class.java.name,
                        Matisse::class.java
                    )!!
                ) as T
            }
        }
    })


    override val captureStrategy: CaptureStrategy
        get() = requireNotNull(value = matisseViewModel.captureStrategy)


    override fun onCreate(savedInstanceState: Bundle?) {
        setSystemBarUi(previewPageVisible = false)
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            // 13,14,12 denied
            // 使用契约启动设置界面并处理结果
            val launcher = rememberLauncherForActivityResult(
                contract = SettingsActivityResultContract(),
                onResult = { result ->
                    // 用户已从设置界面返回
                    // 在这里处理返回事件
                    permissionState.value = checkPermissionCustom(context)
                    requestReadMediaPermissionCustom()
                }
            )

            LaunchedEffect(key1 = Unit) {
                snapshotFlow {
                    matisseViewModel.previewPageViewState.visible
                }.collectLatest {
                    setSystemBarUi(previewPageVisible = it)
                }
            }

            MatisseTheme {
                MatissePage(
                    pageViewState = matisseViewModel.pageViewState,
                    bottomBarViewState = matisseViewModel.bottomBarViewState,
                    onRequestTakePicture = ::requestTakePicture,
                    onClickSure = ::onClickSure,
                    selectMediaInFastSelectMode = ::selectMediaInFastSelectMode,
                    customContent = { innerPadding ->
                        PermissionAbout(
                            innerPadding = innerPadding,
                            requestPermission = ::requestReadMediaPermissionCustom,
                            showPermissionDialog = showPermissionDialog.value,
                            permissionState = permissionState.value,
                            onClick = {
                                launcher.launch(null)
                            },
                            onDismissPermissionDialog = {
                                showPermissionDialog.value = false
                            }
                        )
                    }
                )
                MatissePreviewPage(
                    pageViewState = matisseViewModel.previewPageViewState,
                    imageEngine = matisseViewModel.pageViewState.imageEngine,
                    requestOpenVideo = ::requestOpenVideo,
                    onClickSure = ::onClickSure
                )
                MatisseLoadingDialog(
                    modifier = Modifier,
                    visible = matisseViewModel.loadingDialogVisible
                )
            }
        }
    }

    private fun requestReadMediaPermissionCustom() {
        requestReadMediaPermissionCustom(
            matisseViewModel.mediaType,
            applicationInfo = applicationInfo,
            checkPermissionResult = { permissions: Array<String> ->
                permissionState.value = checkPermissionResultCustom(
                    context = this,
                    requestReadMediaPermissionLauncher = requestReadMediaPermissionLauncher,
                    onPermissionAllow = {
                        matisseViewModel.requestReadMediaPermissionResult(
                            true
                        )
                    },
                    scope = scope,
                    onScopeIsNotActive = { scope = CoroutineScope(Dispatchers.Default) },
                    permissions = permissions,
                    onRequestDenied = {
                        showPermissionDialog.value = true
                    }
                )
            }
        )
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

    private fun requestOpenVideo(mediaResource: MediaResource) {
        val intent = Intent(this, MatisseVideoViewActivity::class.java)
        intent.putExtra(MediaResource::class.java.name, mediaResource)
        startActivity(intent)
    }


    private fun onClickSure() {
        val selectedResources = matisseViewModel.filterSelectedMedia()
        if (matisseViewModel.singleMediaType) {
            val includeImage = selectedResources.any { it.isImage }
            val includeVideo = selectedResources.any { it.isVideo }
            if (includeImage && includeVideo) {
                showToast(id = R.string.matisse_cannot_select_both_picture_and_video_at_the_same_time)
                return
            }
        }
        onSure(selected = selectedResources)
    }

    private fun selectMediaInFastSelectMode(mediaResource: MediaResource) {
        onSure(selected = listOf(element = mediaResource))
    }

    private fun onSure(selected: List<MediaResource>) {
        if (selected.isEmpty()) {
            setResult(RESULT_CANCELED)
        } else {
            val data = Intent()
            val resources = arrayListOf<Parcelable>().apply {
                addAll(selected)
            }
            data.putParcelableArrayListExtra(MediaResource::class.java.name, resources)
            setResult(RESULT_OK, data)
        }
        finish()
    }

    private fun setSystemBarUi(previewPageVisible: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (previewPageVisible) {
                hide(WindowInsetsCompat.Type.statusBars())
            } else {
                show(WindowInsetsCompat.Type.statusBars())
            }
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
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
            isAppearanceLightStatusBars = statusBarDarkIcons
            isAppearanceLightNavigationBars = navigationBarDarkIcons
        }
    }

}