package github.leavesczy.matisse.samples

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import coil.compose.AsyncImage
import github.leavesczy.matisse.MatisseCaptureContract
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.samples.logic.MainPageViewState
import github.leavesczy.matisse.samples.logic.MainViewModel
import github.leavesczy.matisse.samples.logic.MediaCapturePreferences
import github.leavesczy.matisse.samples.logic.MediaCaptureStrategy
import github.leavesczy.matisse.samples.logic.MediaFilterStrategy
import github.leavesczy.matisse.samples.logic.MediaImageEngine
import github.leavesczy.matisse.samples.logic.MediaType
import github.leavesczy.matisse.samples.ui.theme.MatisseTheme

class MainActivity : AppCompatActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setSystemBarUi()
        super.onCreate(savedInstanceState)
        setContent {
            val takePictureLauncher =
                rememberLauncherForActivityResult(contract = MatisseCaptureContract()) { result ->
                    mainViewModel.takePictureResult(result = result)
                }
            val mediaPickerLauncher =
                rememberLauncherForActivityResult(contract = MatisseContract()) { result ->
                    mainViewModel.mediaPickerResult(result = result)
                }
            MatisseTheme {
                MainPage(
                    mainPageViewState = mainViewModel.mainPageViewState,
                    takePicture = {
                        takePictureLauncher.launch(mainViewModel.buildMatisseCapture())
                    },
                    mediaPicker = {
                        mediaPickerLauncher.launch(mainViewModel.buildMatisse())
                    }
                )
            }
        }
    }

    private fun setSystemBarUi() {
        val statusBarColor = android.graphics.Color.TRANSPARENT
        val navigationBarColor = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = statusBarColor
            ),
            navigationBarStyle = SystemBarStyle.dark(
                scrim = navigationBarColor
            )
        )
    }

}

@Composable
private fun MainPage(
    mainPageViewState: MainPageViewState,
    takePicture: () -> Unit,
    mediaPicker: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            Box(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.primary)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(height = 60.dp)
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
        Column(
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .verticalScroll(state = rememberScrollState())
                .padding(
                    start = 16.dp, top = 10.dp,
                    end = 16.dp, bottom = 60.dp
                ),
            horizontalAlignment = Alignment.Start,
        ) {
            Title(text = "类型")
            Row {
                for (value in MediaType.values()) {
                    RadioButton(
                        tips = value.name,
                        selected = mainPageViewState.mediaType == value,
                        onClick = {
                            mainPageViewState.onMediaTypeChanged(value)
                        }
                    )
                }
            }
            Title(text = "过滤策略")
            FlowRow {
                for (strategy in MediaFilterStrategy.values()) {
                    RadioButton(
                        tips = strategy.name,
                        selected = mainPageViewState.filterStrategy == strategy,
                        onClick = {
                            mainPageViewState.onFilterStrategyChanged(strategy)
                        }
                    )
                }
            }
            Title(text = "数量")
            Row {
                for (i in 1..3) {
                    RadioButton(
                        tips = i.toString(),
                        selected = mainPageViewState.maxSelectable == i,
                        onClick = {
                            mainPageViewState.onMaxSelectableChanged(i)
                        }
                    )
                }
            }
            Title(text = "ImageEngine")
            FlowRow(
                maxItemsInEachRow = 3
            ) {
                for (engine in MediaImageEngine.values()) {
                    RadioButton(
                        tips = engine.name,
                        selected = mainPageViewState.imageEngine == engine,
                        onClick = {
                            mainPageViewState.onImageEngineChanged(engine)
                        }
                    )
                }
            }
            Title(text = "拍照策略")
            FlowRow(
                maxItemsInEachRow = 2
            ) {
                for (strategy in MediaCaptureStrategy.values()) {
                    RadioButton(
                        tips = strategy.name,
                        selected = mainPageViewState.captureStrategy == strategy,
                        onClick = {
                            mainPageViewState.onCaptureStrategyChanged(strategy)
                        }
                    )
                }
            }
            Title(text = "拍照配置项")
            FlowRow {
                for (preferences in MediaCapturePreferences.values()) {
                    RadioButton(
                        tips = preferences.name,
                        selected = mainPageViewState.capturePreferences == preferences,
                        onClick = {
                            mainPageViewState.onCapturePreferencesChanged(preferences)
                        }
                    )
                }
            }
            Button(
                text = "切换主题",
                enabled = true,
                onClick = mainPageViewState.switchTheme
            )
            Button(
                text = "直接拍照",
                enabled = mainPageViewState.captureStrategy != MediaCaptureStrategy.Nothing,
                onClick = takePicture
            )
            Button(
                text = "选择图片或视频",
                enabled = true,
                onClick = mediaPicker
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 16.dp)
            )
            for (mediaResource in mainPageViewState.mediaList) {
                MediaResourceItem(mediaResource = mediaResource)
            }
        }
    }
}

@Composable
private fun Button(text: String, enabled: Boolean, onClick: () -> Unit) {
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
            .padding(vertical = 4.dp),
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
                    .size(size = 100.dp),
                model = mediaResource.uri,
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(id = R.string.app_name)
            )
            Text(
                modifier = Modifier.padding(start = 10.dp),
                text = mediaResource.uri.toString() + "\n\n" +
                        mediaResource.path + "\n\n" +
                        mediaResource.name + "\n\n" +
                        mediaResource.mimeType,
                fontSize = 14.sp,
                lineHeight = 16.sp
            )
        }
    }
}