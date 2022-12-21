package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.internal.logic.MatissePageAction
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.theme.LocalMatisseTheme

/**
 * @Author: CZY
 * @Date: 2022/5/31 16:36
 * @Desc:
 */
@Composable
internal fun MatissePage(viewModel: MatisseViewModel, pageAction: MatissePageAction) {
    val matisseViewState = viewModel.matisseViewState
    val matisse = matisseViewState.matisse
    val selectedMediaResources = matisseViewState.selectedResources
    val allBucket = matisseViewState.allBucket
    val selectedBucket = matisseViewState.selectedBucket
    val selectedBucketResources = selectedBucket.resources
    val supportCapture = selectedBucket.supportCapture
    val lazyGridState by remember(key1 = selectedBucket.bucketId) {
        mutableStateOf(
            value = LazyGridState(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0
            )
        )
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        backgroundColor = LocalMatisseTheme.current.surfaceColor,
        topBar = {
            MatisseTopBar(
                allBucket = allBucket,
                selectedBucket = selectedBucket,
                onSelectBucket = {
                    viewModel.onSelectBucket(bucket = it)
                },
                onClickBackMenu = pageAction.onClickBackMenu,
            )
        },
        bottomBar = {
            MatisseBottomBar(
                viewModel = viewModel,
                onSureButtonClick = pageAction.onSureButtonClick,
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            state = lazyGridState,
            columns = GridCells.Fixed(count = matisse.spanCount),
            contentPadding = PaddingValues(
                bottom = 60.dp
            )
        ) {
            if (supportCapture) {
                item(
                    key = "Capture",
                    contentType = "Capture",
                    content = {
                        CaptureItem(onClick = pageAction.onRequestCapture)
                    }
                )
            }
            items(
                count = selectedBucketResources.size,
                key = {
                    selectedBucketResources[it].key
                },
                contentType = {
                    "Album"
                },
                itemContent = { itemIndex ->
                    val resource = selectedBucketResources[itemIndex]
                    val index = selectedMediaResources.indexOf(resource)
                    val isSelected = index > -1
                    val enabled = isSelected || selectedMediaResources.size < matisse.maxSelectable
                    AlbumItem(
                        mediaResource = resource,
                        isSelected = isSelected,
                        enabled = enabled,
                        position = if (isSelected) {
                            (index + 1).toString()
                        } else {
                            ""
                        },
                        onClickMedia = {
                            viewModel.onClickMedia(mediaResource = it)
                        },
                        onMediaCheckChanged = {
                            viewModel.onMediaCheckChanged(mediaResource = it)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun AlbumItem(
    mediaResource: MediaResource,
    isSelected: Boolean,
    enabled: Boolean,
    position: String,
    onClickMedia: (MediaResource) -> Unit,
    onMediaCheckChanged: (MediaResource) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(all = 1.dp)
            .aspectRatio(ratio = 1f)
            .clip(shape = RoundedCornerShape(size = 2.dp))
            .background(color = LocalMatisseTheme.current.imageBackgroundColor)
            .then(
                other = if (isSelected) {
                    Modifier.drawFrame(color = LocalMatisseTheme.current.checkBoxTheme.frameColor)
                } else {
                    Modifier
                }
            )
            .clickable {
                onClickMedia(mediaResource)
            }
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = mediaResource.uri,
            contentScale = ContentScale.Crop,
            contentDescription = mediaResource.displayName
        )
        MatisseCheckbox(
            modifier = Modifier
                .align(alignment = Alignment.TopEnd)
                .padding(all = 3.dp),
            theme = LocalMatisseTheme.current.checkBoxTheme,
            text = position,
            checked = isSelected,
            enabled = enabled,
            onCheckedChange = {
                onMediaCheckChanged(mediaResource)
            }
        )
    }
}

@Composable
private fun CaptureItem(onClick: () -> Unit) {
    val captureIconTheme = LocalMatisseTheme.current.captureIconTheme
    Box(
        modifier = Modifier
            .padding(all = 1.dp)
            .aspectRatio(ratio = 1f)
            .clip(shape = RoundedCornerShape(size = 2.dp))
            .background(color = captureIconTheme.backgroundColor)
            .clickable(onClick = onClick)
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(fraction = 0.5f)
                .align(alignment = Alignment.Center),
            imageVector = captureIconTheme.icon,
            tint = captureIconTheme.tint,
            contentDescription = "Capture",
        )
    }
}

private fun Modifier.drawFrame(color: Color): Modifier {
    return drawWithCache {
        val lineWidth = 3.dp.toPx()
        val topLeftPoint = lineWidth / 2f
        val rectSize = size.width - lineWidth
        onDrawWithContent {
            drawContent()
            drawRect(
                color = color,
                topLeft = Offset(topLeftPoint, topLeftPoint),
                size = Size(width = rectSize, height = rectSize),
                style = Stroke(width = lineWidth)
            )
        }
    }
}