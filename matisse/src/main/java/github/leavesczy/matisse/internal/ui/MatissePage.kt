package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseBottomBarViewState
import github.leavesczy.matisse.internal.logic.MatissePageViewState
import github.leavesczy.matisse.internal.logic.MatisseTopBarViewState
import github.leavesczy.matisse.internal.utils.isVideo

/**
 * @Author: CZY
 * @Date: 2022/5/31 16:36
 * @Desc:
 */
@Composable
internal fun MatissePage(
    matisse: Matisse,
    matissePageViewState: MatissePageViewState,
    matisseTopBarViewState: MatisseTopBarViewState,
    matisseBottomBarViewState: MatisseBottomBarViewState,
    selectedResources: List<MediaResource>,
    onRequestTakePicture: () -> Unit,
    onSure: () -> Unit
) {
    val context = LocalContext.current
    val spanCount = remember {
        context.resources.getInteger(R.integer.matisse_image_span_count)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(id = R.color.matisse_main_page_background_color),
        topBar = {
            MatisseTopBar(
                matisse = matisse,
                topBarViewState = matisseTopBarViewState
            )
        },
        bottomBar = {
            MatisseBottomBar(
                bottomBarViewState = matisseBottomBarViewState,
                onSure = onSure
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            state = matissePageViewState.lazyGridState,
            columns = GridCells.Fixed(count = spanCount),
            contentPadding = PaddingValues(
                bottom = 60.dp
            )
        ) {
            val selectedBucket = matissePageViewState.selectedBucket
            if (selectedBucket.supportCapture) {
                item(
                    key = "Capture",
                    contentType = "Capture",
                    content = {
                        CaptureItem(onClick = onRequestTakePicture)
                    }
                )
            }
            items(
                items = selectedBucket.resources,
                key = {
                    it.id
                },
                contentType = {
                    "Media"
                },
                itemContent = { media ->
                    AlbumItem(
                        matisse = matisse,
                        mediaResource = media,
                        selectedResources = selectedResources,
                        onClickMedia = matissePageViewState.onClickMedia,
                        onClickCheckBox = matissePageViewState.onMediaCheckChanged
                    )
                }
            )
        }
    }
}

@Composable
private fun AlbumItem(
    matisse: Matisse,
    mediaResource: MediaResource,
    selectedResources: List<MediaResource>,
    onClickMedia: (MediaResource) -> Unit,
    onClickCheckBox: (MediaResource) -> Unit
) {
    val index = selectedResources.indexOf(element = mediaResource)
    val isSelected = index > -1
    val enabled = isSelected || selectedResources.size < matisse.maxSelectable
    val position = if (isSelected) {
        (index + 1).toString()
    } else {
        ""
    }
//    val index by remember {
//        derivedStateOf {
//            selectedResources.indexOf(element = mediaResource)
//        }
//    }
//    val isSelected by remember {
//        derivedStateOf {
//            index > -1
//        }
//    }
//    val enabled by remember {
//        derivedStateOf {
//            isSelected || selectedResources.size < maxSelectable
//        }
//    }
//    val position by remember {
//        derivedStateOf {
//            if (isSelected) {
//                (index + 1).toString()
//            } else {
//                ""
//            }
//        }
//    }
    Box(
        modifier = Modifier
            .padding(all = 1.dp)
            .aspectRatio(ratio = 1f)
            .clip(shape = RoundedCornerShape(size = 4.dp))
            .background(color = colorResource(id = R.color.matisse_image_item_background_color))
            .then(
                other = if (isSelected) {
                    Modifier.drawBorder(color = colorResource(id = R.color.matisse_image_item_border_color_when_selected))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = {
                onClickMedia(mediaResource)
            })
    ) {
        matisse.imageEngine.Image(
            modifier = Modifier.fillMaxSize(),
            model = mediaResource.uri,
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            contentDescription = mediaResource.displayName
        )
        MatisseCheckbox(
            modifier = Modifier
                .align(alignment = Alignment.TopEnd)
                .padding(all = 3.dp),
            text = position,
            checked = isSelected,
            enabled = enabled,
            onClick = {
                onClickCheckBox(mediaResource)
            }
        )
        if (mediaResource.isVideo) {
            Icon(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .size(size = 32.dp),
                imageVector = Icons.Filled.SlowMotionVideo,
                tint = colorResource(id = R.color.matisse_video_icon_color),
                contentDescription = mediaResource.displayName
            )
        }
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