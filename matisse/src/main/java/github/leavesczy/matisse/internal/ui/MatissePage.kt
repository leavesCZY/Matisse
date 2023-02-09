package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatissePageAction
import github.leavesczy.matisse.internal.logic.MatisseViewModel

/**
 * @Author: CZY
 * @Date: 2022/5/31 16:36
 * @Desc:
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MatissePage(viewModel: MatisseViewModel, pageAction: MatissePageAction) {
    val matisseViewState = viewModel.matisseViewState
    val maxSelectable = viewModel.matisseViewState.matisse.maxSelectable
    val selectedMediaResources = viewModel.matisseViewState.selectedResources
    val allBucket = viewModel.matisseViewState.allBucket
    val selectedBucket = viewModel.matisseViewState.selectedBucket
    val selectedBucketResources = selectedBucket.resources
    val supportCapture = selectedBucket.supportCapture
    val lazyGridState by remember(key1 = selectedBucket.bucketId) {
        mutableStateOf(
            value = LazyGridState(
                firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0
            )
        )
    }
    val context = LocalContext.current
    val localConfiguration = LocalConfiguration.current
    val localDensity = LocalDensity.current
    val spanCount = remember {
        context.resources.getInteger(R.integer.matisse_image_span_count)
    }
    val imageItemWidthPx = remember {
        with(localDensity) {
            (localConfiguration.screenWidthDp.dp.toPx() / spanCount).toInt()
        }
    }
    Scaffold(modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(id = R.color.matisse_main_page_background_color),
        topBar = {
            MatisseTopBar(
                allBucket = allBucket, selectedBucket = selectedBucket, onSelectBucket = {
                    viewModel.onSelectBucket(bucket = it)
                }, onClickBackMenu = pageAction.onClickBackMenu
            )
        },
        bottomBar = {
            MatisseBottomBar(
                viewModel = viewModel, onSureButtonClick = pageAction.onSureButtonClick
            )
        }) { innerPadding ->
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            state = lazyGridState,
            columns = GridCells.Fixed(count = spanCount),
            contentPadding = PaddingValues(
                bottom = 60.dp
            )
        ) {
            if (supportCapture) {
                item(key = "MatisseCapture", contentType = "MatisseCapture", content = {
                    CaptureItem(onClick = pageAction.onRequestCapture)
                })
            }
            items(items = selectedBucketResources, key = {
                it.key
            }, contentType = {
                "MatisseAlbum"
            }, itemContent = { media ->
                val index = selectedMediaResources.indexOf(element = media)
                val isSelected = index > -1
                val enabled = isSelected || selectedMediaResources.size < maxSelectable
                AlbumItem(media = media,
                    isSelected = isSelected,
                    enabled = enabled,
                    position = if (isSelected) {
                        (index + 1).toString()
                    } else {
                        ""
                    },
                    itemWidthPx = imageItemWidthPx,
                    onClickMedia = {
                        viewModel.onClickMedia(mediaResource = media)
                    },
                    onMediaCheckChanged = {
                        viewModel.onMediaCheckChanged(mediaResource = media)
                    })
            })
        }
    }
}

@Composable
private fun AlbumItem(
    media: MediaResource,
    isSelected: Boolean,
    enabled: Boolean,
    position: String,
    itemWidthPx: Int,
    onClickMedia: () -> Unit,
    onMediaCheckChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .padding(all = 1.dp)
            .aspectRatio(ratio = 1f)
            .clip(shape = RoundedCornerShape(size = 2.dp))
            .background(color = colorResource(id = R.color.matisse_image_item_background_color))
            .then(
                other = if (isSelected) {
                    Modifier.drawBorder(color = colorResource(id = R.color.matisse_image_item_border_color_when_selected))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClickMedia)
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = ImageRequest.Builder(context = context).data(data = media.uri)
                .size(size = itemWidthPx).crossfade(enable = false).build(),
            contentScale = ContentScale.Crop,
            contentDescription = media.displayName
        )
        MatisseCheckbox(
            modifier = Modifier
                .align(alignment = Alignment.TopEnd)
                .padding(all = 3.dp),
            text = position,
            checked = isSelected,
            enabled = enabled,
            onCheckedChange = onMediaCheckChanged
        )
    }
}

@Composable
private fun CaptureItem(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(all = 1.dp)
            .aspectRatio(ratio = 1f)
            .clip(shape = RoundedCornerShape(size = 2.dp))
            .background(color = colorResource(id = R.color.matisse_image_item_background_color))
            .clickable(onClick = onClick)
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(fraction = 0.5f)
                .align(alignment = Alignment.Center),
            imageVector = Icons.Filled.PhotoCamera,
            tint = colorResource(id = R.color.matisse_capture_icon_color),
            contentDescription = "Capture"
        )
    }
}

private fun Modifier.drawBorder(color: Color): Modifier {
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