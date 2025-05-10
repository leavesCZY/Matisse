package github.leavesczy.matisse.samples

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import github.leavesczy.matisse.MatisseCaptureContract
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.samples.logic.MainPageViewState
import github.leavesczy.matisse.samples.logic.MainViewModel
import github.leavesczy.matisse.samples.logic.MediaCaptureStrategy
import github.leavesczy.matisse.samples.logic.MediaFilterStrategy
import github.leavesczy.matisse.samples.logic.MediaImageEngine
import github.leavesczy.matisse.samples.theme.MatisseTheme

/**
 * @Author: leavesCZY
 * @Date: 2024/2/21 12:01
 * @Desc:
 */
class MainActivity : AppCompatActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setSystemBarUi()
        super.onCreate(savedInstanceState)
        setContent {
            val takePictureLauncher =
                rememberLauncherForActivityResult(contract = MatisseCaptureContract()) {
                    mainViewModel.takePictureResult(result = it)
                }
            val mediaPickerLauncher =
                rememberLauncherForActivityResult(contract = MatisseContract()) {
                    mainViewModel.mediaPickerResult(result = it)
                }
            MatisseTheme {
                MainPage(
                    pageViewState = mainViewModel.pageViewState,
                    imageAndVideo = {
                        mediaPickerLauncher.launch(
                            mainViewModel.buildMatisse(mediaType = MediaType.ImageAndVideo)
                        )
                    },
                    imageOnly = {
                        mediaPickerLauncher.launch(
                            mainViewModel.buildMatisse(mediaType = MediaType.ImageOnly)
                        )
                    },
                    videoOnly = {
                        mediaPickerLauncher.launch(mainViewModel.buildMatisse(mediaType = MediaType.VideoOnly))
                    },
                    gifAndMp4 = {
                        mediaPickerLauncher.launch(
                            mainViewModel.buildMatisse(
                                mediaType = MediaType.MultipleMimeType(
                                    mimeTypes = setOf("image/gif", "video/mp4")
                                )
                            )
                        )
                    },
                    takePicture = {
                        val matisseCapture = mainViewModel.buildMediaCaptureStrategy()
                        if (matisseCapture != null) {
                            takePictureLauncher.launch(matisseCapture)
                        }
                    }
                )
            }
        }
    }

    private fun setSystemBarUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

}

@Composable
private fun MainPage(
    pageViewState: MainPageViewState,
    imageAndVideo: () -> Unit,
    imageOnly: () -> Unit,
    videoOnly: () -> Unit,
    gifAndMp4: () -> Unit,
    takePicture: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
        contentWindowInsets = WindowInsets(left = 0.dp, right = 0.dp, top = 0.dp, bottom = 0.dp),
        topBar = {
            Box(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.primary)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(height = 55.dp)
            ) {
                Text(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterStart)
                        .padding(horizontal = 10.dp),
                    text = stringResource(id = R.string.app_name),
                    fontSize = 22.sp,
                    color = Color.White
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .navigationBarsPadding()
                .verticalScroll(state = rememberScrollState())
                .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 50.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Title(text = "gridColumns")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                for (gridColumns in 2..5) {
                    RadioButton(
                        tips = gridColumns.toString(),
                        selected = pageViewState.gridColumns == gridColumns,
                        onClick = {
                            pageViewState.onGridColumnsChanged(gridColumns)
                        }
                    )
                }
            }
            Title(text = "maxSelectable")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                for (maxSelectable in 1..4) {
                    RadioButton(
                        tips = maxSelectable.toString(),
                        selected = pageViewState.maxSelectable == maxSelectable,
                        onClick = {
                            pageViewState.onMaxSelectableChanged(maxSelectable)
                        }
                    )
                }
            }
            OptionDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Title(text = "fastSelect")
                Checkbox(
                    checked = pageViewState.fastSelect,
                    onCheckedChange = pageViewState.onFastSelectChanged
                )
            }
            OptionDivider()
            Title(text = "ImageEngine")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                for (engine in MediaImageEngine.entries) {
                    RadioButton(
                        tips = engine.name,
                        selected = pageViewState.imageEngine == engine,
                        onClick = {
                            pageViewState.onImageEngineChanged(engine)
                        }
                    )
                }
            }
            OptionDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Title(text = "singleMediaType")
                Checkbox(
                    checked = pageViewState.singleMediaType,
                    onCheckedChange = pageViewState.onSingleMediaTypeChanged
                )
            }
            OptionDivider()
            Title(text = "mediaFilter")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                for (strategy in MediaFilterStrategy.entries) {
                    RadioButton(
                        tips = strategy.name,
                        selected = pageViewState.filterStrategy == strategy,
                        onClick = {
                            pageViewState.onFilterStrategyChanged(strategy)
                        }
                    )
                }
            }
            OptionDivider()
            Title(text = "CaptureStrategy")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                for (strategy in MediaCaptureStrategy.entries) {
                    RadioButton(
                        tips = strategy.name,
                        selected = pageViewState.captureStrategy == strategy,
                        onClick = {
                            pageViewState.onCaptureStrategyChanged(strategy)
                        }
                    )
                }
            }
            OptionDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Title(text = "CapturePreferencesCustom")
                Checkbox(
                    checked = pageViewState.capturePreferencesCustom,
                    enabled = pageViewState.captureStrategy != MediaCaptureStrategy.Close,
                    onCheckedChange = pageViewState.onCapturePreferencesCustomChanged
                )
            }
            Button(
                text = "图片 + 视频",
                onClick = imageAndVideo
            )
            Button(
                text = "图片",
                onClick = imageOnly
            )
            Button(
                text = "视频",
                onClick = videoOnly
            )
            Button(
                text = "gif + mp4",
                onClick = gifAndMp4
            )
            Button(
                text = "直接拍照",
                enabled = pageViewState.captureStrategy != MediaCaptureStrategy.Close,
                onClick = takePicture
            )
            Button(
                text = "切换主题",
                onClick = pageViewState.switchTheme
            )
            for (mediaResource in pageViewState.mediaList) {
                MediaResourceItem(mediaResource = mediaResource)
            }
        }
    }
}

@Composable
private fun OptionDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        thickness = 0.5.dp
    )
}

@Composable
private fun Button(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .fillMaxWidth(),
        enabled = enabled,
        onClick = onClick
    ) {
        Text(
            modifier = Modifier,
            text = text,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun Title(text: String) {
    Text(
        modifier = Modifier,
        text = text,
        fontSize = 17.sp
    )
}

@Composable
private fun RadioButton(
    tips: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier,
            text = tips,
            fontSize = 16.sp
        )
        RadioButton(
            modifier = Modifier,
            selected = selected,
            onClick = onClick
        )
    }
}

@Composable
private fun MediaResourceItem(mediaResource: MediaResource) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(size = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(all = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(size = 80.dp),
                model = mediaResource.uri,
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .padding(start = 10.dp),
                text = mediaResource.uri.toString() + "\n\n" +
                        mediaResource.path + "\n\n" +
                        mediaResource.mimeType,
                fontSize = 14.sp,
                lineHeight = 16.sp
            )
        }
    }
}