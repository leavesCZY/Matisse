@file:OptIn(ExperimentalLayoutApi::class)

package github.leavesczy.matisse.samples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import github.leavesczy.matisse.MatisseCapture
import github.leavesczy.matisse.MatisseCaptureContract
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.samples.ui.theme.MatisseTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val darkTheme = mainViewModel.darkTheme
            MatisseTheme(darkTheme = darkTheme) {
                SetSystemUi(darkTheme = darkTheme)
                MainPage(mainViewModel = mainViewModel)
            }
        }
    }

    @Composable
    private fun SetSystemUi(darkTheme: Boolean) {
        val systemUiController = rememberSystemUiController()
        val statusBarColor = MaterialTheme.colorScheme.primary
        val navigationBarColor = MaterialTheme.colorScheme.background
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = false,
            transformColorForLightContent = {
                statusBarColor
            }
        )
        systemUiController.setNavigationBarColor(
            color = navigationBarColor,
            darkIcons = !darkTheme,
            transformColorForLightContent = {
                navigationBarColor
            }
        )
    }

}

@Composable
private fun MainPage(mainViewModel: MainViewModel) {
    val mainPageViewState = mainViewModel.mainPageViewState
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(height = 60.dp)
                    .background(color = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterStart)
                        .padding(horizontal = 20.dp),
                    text = stringResource(id = R.string.app_name),
                    fontSize = 22.sp,
                    color = Color.White
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(all = 20.dp),
            contentPadding = innerPadding
        ) {
            item(
                key = "Options",
                contentType = "Options"
            ) {
                Options(mainViewModel = mainViewModel)
            }
            items(
                items = mainPageViewState.mediaList,
                key = {
                    it.uri
                },
                contentType = {
                    "mediaResource"
                }
            ) {
                MediaResourceItem(mediaResource = it)
            }
        }
    }
}

@Composable
private fun Options(mainViewModel: MainViewModel) {
    val takePictureLauncher =
        rememberLauncherForActivityResult(contract = MatisseCaptureContract()) { result ->
            mainViewModel.takePictureResult(result = result)
        }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(contract = MatisseContract()) { result ->
            mainViewModel.imagePickerResult(result = result)
        }
    val mainPageViewState = mainViewModel.mainPageViewState
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier,
            text = "媒体类型"
        )
        Row(modifier = Modifier) {
            for (value in MediaType.values()) {
                MyRadioButton(
                    tips = value.name,
                    selected = mainPageViewState.mediaType == value,
                    onClick = {
                        mainViewModel.onMediaTypeChanged(mediaType = value)
                    }
                )
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier,
            text = "Gif"
        )
        Row(modifier = Modifier) {
            MyRadioButton(
                tips = "包含",
                selected = mainPageViewState.supportGif,
                onClick = {
                    mainViewModel.onSupportGifChanged(supportGif = true)
                }
            )
            MyRadioButton(
                tips = "不包含",
                selected = !mainPageViewState.supportGif,
                onClick = {
                    mainViewModel.onSupportGifChanged(supportGif = false)
                }
            )
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier,
            text = "选取数量"
        )
        Row(modifier = Modifier) {
            for (i in 1..3) {
                MyRadioButton(
                    tips = i.toString(),
                    selected = mainPageViewState.maxSelectable == i,
                    onClick = {
                        mainViewModel.onMaxSelectableChanged(maxSelectable = i)
                    }
                )
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier,
            text = "拍照策略"
        )
        FlowRow(modifier = Modifier) {
            for (value in MediaCaptureStrategy.values()) {
                MyRadioButton(
                    tips = value.name,
                    selected = mainPageViewState.captureStrategy == value,
                    onClick = {
                        mainViewModel.onCaptureStrategyChanged(captureStrategy = value)
                    }
                )
            }
        }
    }
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        onClick = {
            mainViewModel.switchTheme()
        }
    ) {
        Text(
            modifier = Modifier,
            text = "切换主题"
        )
    }
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        enabled = mainPageViewState.captureStrategy != MediaCaptureStrategy.Nothing,
        onClick = {
            val matisseCapture =
                MatisseCapture(captureStrategy = mainViewModel.getMediaCaptureStrategy())
            takePictureLauncher.launch(matisseCapture)
        }
    ) {
        Text(
            modifier = Modifier,
            text = "直接拍照"
        )
    }
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        onClick = {
            imagePickerLauncher.launch(mainViewModel.buildMatisse())
        }
    ) {
        Text(
            modifier = Modifier,
            text = "选择图片或视频"
        )
    }
}

@Composable
private fun MediaResourceItem(mediaResource: MediaResource) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(size = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(size = 100.dp),
                model = mediaResource.uri,
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(id = R.string.app_name)
            )
            Text(
                modifier = Modifier.padding(start = 10.dp),
                text = mediaResource.uri.toString() + "\n\n" +
                        mediaResource.mimeType,
                fontSize = 14.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun MyRadioButton(
    tips: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier,
            text = tips
        )
        RadioButton(
            modifier = Modifier,
            selected = selected,
            onClick = onClick
        )
    }
}