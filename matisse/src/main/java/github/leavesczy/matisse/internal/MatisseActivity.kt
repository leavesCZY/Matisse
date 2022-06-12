package github.leavesczy.matisse.internal

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaResources
import github.leavesczy.matisse.internal.theme.MatisseTheme
import github.leavesczy.matisse.internal.utils.PermissionUtils
import github.leavesczy.matisse.internal.vm.MatisseViewModel
import github.leavesczy.matisse.internal.widget.MatissePage
import github.leavesczy.matisse.internal.widget.MatissePreviewPage
import kotlinx.coroutines.launch

/**
 * @Author: leavesCZY
 * @Date: 2022/5/28 22:28
 * @Desc:
 */
class MatisseActivity : ComponentActivity() {

    companion object {

        private const val routeMatisse = "routeMatisse"

        private const val routePreviewSuffix = "routePreviewSuffix"

        private const val keyPreviewOnlySelected = "keyPreviewOnlySelected"

        private const val keyPreviewPageStartIndex = "keyPreviewPageStartIndex"

        private const val routePreview =
            routePreviewSuffix + "/{${keyPreviewOnlySelected}}" + "/{${keyPreviewPageStartIndex}}"

    }

    private val matisse by lazy {
        SelectionSpec.getMatisse()
    }

    private val captureStrategy by lazy {
        matisse.captureStrategy
    }

    private val matisseViewModel by viewModels<MatisseViewModel>(factoryProducer = {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MatisseViewModel(
                    application = application,
                    matisse = matisse
                ) as T
            }
        }
    })

    private val requestReadExternalStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            matisseViewModel.onRequestReadExternalStoragePermissionResult(granted = granted)
        }

    private val requestWriteExternalStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requestCameraPermissionIfNeed()
            } else {
                val tips = matisse.tips.onWriteExternalStorageDenied
                if (tips.isNotBlank()) {
                    Toast.makeText(this, tips, Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePicture()
            } else {
                val tips = matisse.tips.onCameraDenied
                if (tips.isNotBlank()) {
                    Toast.makeText(this, tips, Toast.LENGTH_SHORT).show()
                }
            }
        }

    private var tempImageUri: Uri? = null

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            val mTempImageUri = tempImageUri
            tempImageUri = null
            if (mTempImageUri != null) {
                lifecycleScope.launch {
                    if (result) {
                        val resources = captureStrategy.loadResources(
                            context = this@MatisseActivity,
                            imageUri = mTempImageUri
                        )
                        if (resources != null) {
                            onSure(listOf(resources))
                        }
                    } else {
                        captureStrategy.onTakePictureCanceled(
                            context = this@MatisseActivity,
                            imageUri = mTempImageUri
                        )
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val navController = rememberAnimatedNavController()
            MatisseTheme(matisseTheme = matisse.theme) {
                NavigationView(navController = navController)
            }
        }
        if (PermissionUtils.checkSelfPermission(
                context = this,
                permission = Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            matisseViewModel.onRequestReadExternalStoragePermissionResult(granted = true)
        } else {
            matisseViewModel.onRequestReadExternalStoragePermission()
            requestReadExternalStoragePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    @Composable
    private fun NavigationView(navController: NavHostController) {
        AnimatedNavHost(
            modifier = Modifier,
            navController = navController,
            startDestination = routeMatisse,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = {
                        -it
                    },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = {
                        it
                    },
                    animationSpec = tween(300)
                )
            },
        ) {
            composable(
                route = routeMatisse
            ) {
                MatissePage(
                    matisse = matisse,
                    matisseViewModel = matisseViewModel,
                    onClickBackMenu = {
                        finish()
                    },
                    onCapture = {
                        dispatchTakePicture()
                    },
                    onPreviewSelectedResources = {
                        val selectedMediaResources = matisseViewModel.selectedMediaResources
                        if (selectedMediaResources.isNotEmpty()) {
                            val previewOnlySelected = 1
                            val previewPageStartIndex = 0
                            navController.navigate(
                                route = "$routePreviewSuffix/$previewOnlySelected/$previewPageStartIndex"
                            )
                        }
                    },
                    onClickMedia = { mediaResource ->
                        val previewOnlySelected = 0
                        val previewPageStartIndex =
                            matisseViewModel.matisseViewState.value.selectedBucket.resources.indexOf(
                                mediaResource
                            )
                        navController.navigate(
                            route = "$routePreviewSuffix/$previewOnlySelected/$previewPageStartIndex"
                        )
                    }, onSure = {
                        onSure(selectedMediaResources = matisseViewModel.selectedMediaResources)
                    })
            }
            composable(
                route = routePreview
            ) { backStackEntry ->
                val matisseViewState by matisseViewModel.matisseViewState.collectAsState()
                val previewOnlySelected by remember {
                    mutableStateOf(
                        backStackEntry.arguments?.getString(
                            keyPreviewOnlySelected
                        )?.toIntOrNull() == 1
                    )
                }
                val initialPage by remember {
                    mutableStateOf(
                        backStackEntry.arguments?.getString(
                            keyPreviewPageStartIndex
                        )?.toIntOrNull() ?: 0
                    )
                }
                val previewResource by remember {
                    mutableStateOf(
                        if (previewOnlySelected) {
                            matisseViewState.selectedMediaResources.toList()
                        } else {
                            matisseViewModel.filterSelectedBucketResources()
                        }
                    )
                }
                MatissePreviewPage(
                    matisse = matisse,
                    matisseViewModel = matisseViewModel,
                    previewResource = previewResource,
                    selectedMediaResources = matisseViewState.selectedMediaResources,
                    initialPage = initialPage
                )
            }
        }
    }

    private fun dispatchTakePicture() {
        if (captureStrategy.isEnabled()) {
            requestWritePermissionIfNeed()
        }
    }

    private fun requestWritePermissionIfNeed() {
        if (captureStrategy.shouldRequestWriteExternalStoragePermission(context = this)) {
            requestWriteExternalStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            requestCameraPermissionIfNeed()
        }
    }

    private fun requestCameraPermissionIfNeed() {
        if (PermissionUtils.containsPermission(
                context = this,
                permission = Manifest.permission.CAMERA
            )
            &&
            !PermissionUtils.checkSelfPermission(
                context = this,
                permission = Manifest.permission.CAMERA
            )
        ) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        } else {
            takePicture()
        }
    }

    private fun takePicture() {
        lifecycleScope.launch {
            val imageUri = captureStrategy.createImageUri(context = this@MatisseActivity)
            tempImageUri = imageUri
            if (imageUri != null) {
                val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (captureIntent.resolveActivity(packageManager) != null) {
                    takePictureLauncher.launch(imageUri)
                }
            }
        }
    }

    private fun onSure(selectedMediaResources: List<MediaResources>) {
        if (selectedMediaResources.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
        } else {
            val data =
                MatisseContract.buildResult(selectedMediaResources = selectedMediaResources)
            setResult(Activity.RESULT_OK, data)
        }
        finish()
    }

}