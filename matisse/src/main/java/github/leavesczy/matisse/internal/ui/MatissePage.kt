package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseViewModel

/**
 * @Author: leavesCZY
 * @Date: 2022/5/31 16:36
 * @Desc:
 */
@Composable
internal fun MatissePage(
    matisseViewModel: MatisseViewModel,
    onRequestTakePicture: () -> Unit,
    onClickSure: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(id = R.color.matisse_main_page_background_color),
        topBar = {
            MatisseTopBar(topBarViewState = matisseViewModel.matisseTopBarViewState)
        },
        bottomBar = {
            MatisseBottomBar(
                bottomBarViewState = matisseViewModel.matisseBottomBarViewState,
                onClickSure = onClickSure
            )
        }
    ) { innerPadding ->
        val captureStrategy by remember {
            derivedStateOf {
                matisseViewModel.matissePageViewState.selectedBucket.captureStrategy
            }
        }
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            state = matisseViewModel.matissePageViewState.lazyGridState,
            columns = GridCells.Fixed(count = integerResource(id = R.integer.matisse_image_span_count)),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            if (captureStrategy != null) {
                item(
                    key = "Capture",
                    contentType = "Capture",
                    content = {
                        CaptureItem(onClick = onRequestTakePicture)
                    }
                )
            }
            items(
                items = matisseViewModel.matissePageViewState.selectedBucket.resources,
                key = {
                    it.id
                },
                contentType = {
                    "Media"
                },
                itemContent = {
                    val mediaPlacement by remember {
                        derivedStateOf {
                            val index = matisseViewModel.selectedResources.indexOf(element = it)
                            val isSelected = index > -1
                            val enabled =
                                isSelected || matisseViewModel.selectedResources.size < matisseViewModel.maxSelectable
                            val position = if (isSelected) {
                                (index + 1).toString()
                            } else {
                                ""
                            }
                            MediaPlacement(
                                isSelected = isSelected,
                                enabled = enabled,
                                position = position
                            )
                        }
                    }
                    MediaItem(
                        mediaResource = it,
                        mediaPlacement = mediaPlacement,
                        imageEngine = matisseViewModel.matissePageViewState.imageEngine,
                        onClickMedia = matisseViewModel.matissePageViewState.onClickMedia,
                        onClickCheckBox = matisseViewModel.matissePageViewState.onMediaCheckChanged
                    )
                }
            )
        }
    }
}

@Composable
private fun LazyGridItemScope.CaptureItem(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .animateItemPlacement()
            .padding(all = 1.dp)
            .aspectRatio(ratio = 1f)
            .clip(shape = RoundedCornerShape(size = 4.dp))
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

@Stable
private data class MediaPlacement(
    val isSelected: Boolean,
    val enabled: Boolean,
    val position: String
)

@Composable
private fun LazyGridItemScope.MediaItem(
    mediaResource: MediaResource,
    mediaPlacement: MediaPlacement,
    imageEngine: ImageEngine,
    onClickMedia: (MediaResource) -> Unit,
    onClickCheckBox: (MediaResource) -> Unit
) {
    Box(
        modifier = Modifier
            .animateItemPlacement()
            .padding(all = 1.dp)
            .aspectRatio(ratio = 1f)
            .clip(shape = RoundedCornerShape(size = 4.dp))
            .background(color = colorResource(id = R.color.matisse_image_item_background_color))
            .then(
                other = if (mediaPlacement.isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = colorResource(id = R.color.matisse_image_item_border_color_when_selected),
                        shape = RoundedCornerShape(size = 4.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable {
                onClickMedia(mediaResource)
            },
        contentAlignment = Alignment.Center
    ) {
        imageEngine.Thumbnail(mediaResource = mediaResource)
        MatisseCheckbox(
            modifier = Modifier
                .align(alignment = Alignment.TopEnd)
                .padding(all = 4.dp),
            text = mediaPlacement.position,
            checked = mediaPlacement.isSelected,
            enabled = mediaPlacement.enabled,
            onClick = {
                onClickCheckBox(mediaResource)
            }
        )
        if (mediaResource.isVideo) {
            Icon(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .size(size = 32.dp),
                imageVector = Icons.Filled.PlayCircleOutline,
                tint = colorResource(id = R.color.matisse_video_icon_color),
                contentDescription = mediaResource.name
            )
        }
    }
}