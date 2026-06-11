package github.leavesczy.matisse.samples

import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.samples.logic.MainPageViewState
import github.leavesczy.matisse.samples.logic.MediaCaptureStrategy
import github.leavesczy.matisse.samples.logic.MediaFilterStrategy
import github.leavesczy.matisse.samples.logic.MediaImageEngine

@Composable
fun MainPage(
    pageViewState: MainPageViewState,
    onPickImageAndVideo: () -> Unit,
    onPickImageOnly: () -> Unit,
    onPickVideoOnly: () -> Unit,
    onPickGifAndMp4: () -> Unit,
    onTakePictureClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
        topBar = {
            Box(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.primary)
                    .fillMaxWidth()
                    .windowInsetsPadding(insets = WindowInsets.statusBarsIgnoringVisibility)
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
        val scrollState = rememberSaveable(saver = ScrollState.Saver) {
            ScrollState(initial = 0)
        }
        Column(
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .verticalScroll(state = scrollState)
                .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Title(text = "gridColumns")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                for (gridColumns in 2..5) {
                    RadioButton(
                        label = gridColumns.toString(),
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
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                for (maxSelectable in 1..4) {
                    RadioButton(
                        label = maxSelectable.toString(),
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
                        label = engine.name,
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
                        label = strategy.name,
                        selected = pageViewState.mediaFilterStrategy == strategy,
                        onClick = {
                            pageViewState.onMediaFilterStrategyChanged(strategy)
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
                        label = strategy.name,
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
                Title(text = "useFrontCamera")
                Checkbox(
                    checked = pageViewState.useFrontCamera,
                    enabled = pageViewState.captureStrategy != MediaCaptureStrategy.Disabled,
                    onCheckedChange = pageViewState.onUseFrontCameraChanged
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = 2.dp,
                    alignment = Alignment.Top
                )
            ) {
                Button(
                    text = "图片 + 视频",
                    onClick = onPickImageAndVideo
                )
                Button(
                    text = "图片",
                    onClick = onPickImageOnly
                )
                Button(
                    text = "视频",
                    onClick = onPickVideoOnly
                )
                Button(
                    text = "gif + mp4",
                    onClick = onPickGifAndMp4
                )
                Button(
                    text = "直接拍照",
                    enabled = pageViewState.captureStrategy != MediaCaptureStrategy.Disabled,
                    onClick = onTakePictureClick
                )
                Button(
                    text = "切换主题",
                    onClick = pageViewState.onThemeToggled
                )
            }
            for (media in pageViewState.pickedMediaList) {
                MediaResourceItem(mediaResource = media)
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
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier,
            text = label,
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
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(size = 80.dp)
                    .clip(shape = RoundedCornerShape(size = 12.dp)),
                model = mediaResource.uri,
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .weight(weight = 1f)
                    .padding(start = 10.dp),
                text = mediaResource.toString(),
                fontSize = 15.sp,
                lineHeight = 17.sp
            )
        }
    }
}