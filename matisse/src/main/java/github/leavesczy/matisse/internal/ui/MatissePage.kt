package github.leavesczy.matisse.internal.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseBottomBarViewState
import github.leavesczy.matisse.internal.logic.MatissePageViewState
import github.leavesczy.matisse.internal.logic.MatisseTopBarViewState

/**
 * @Author: leavesCZY
 * @Date: 2022/5/31 16:36
 * @Desc:
 */
@Composable
internal fun MatissePage(
    pageViewState: MatissePageViewState,
    topBarViewState: MatisseTopBarViewState,
    bottomBarViewState: MatisseBottomBarViewState,
    selectedResources: List<MediaResource>,
    onRequestTakePicture: () -> Unit,
    onClickSure: () -> Unit,
    selectMediaInFastSelectMode: (MediaResource) -> Unit
) {
    val mSelectedResources by rememberUpdatedState(newValue = selectedResources)
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = colorResource(id = R.color.matisse_main_page_background_color),
        topBar = {
            MatisseTopBar(
                modifier = Modifier,
                imageEngine = pageViewState.imageEngine,
                viewState = topBarViewState
            )
        },
        bottomBar = {
            if (!pageViewState.fastSelect) {
                MatisseBottomBar(
                    modifier = Modifier,
                    viewState = bottomBarViewState,
                    onClickSure = onClickSure
                )
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            state = pageViewState.lazyGridState,
            columns = GridCells.Fixed(count = pageViewState.gridColumns),
            horizontalArrangement = Arrangement.spacedBy(space = 1.dp),
            verticalArrangement = Arrangement.spacedBy(space = 1.dp),
            contentPadding = PaddingValues(top = 1.dp, bottom = 1.dp)
        ) {
            if (pageViewState.selectedBucket.supportCapture) {
                item(
                    key = "CaptureItem",
                    contentType = "CaptureItem"
                ) {
                    CaptureItem(
                        modifier = Modifier
                            .customAnimateItem(scope = this),
                        onClick = onRequestTakePicture
                    )
                }
            }
            items(
                items = pageViewState.selectedBucket.resources,
                key = {
                    it.id
                },
                contentType = {
                    "MediaItem"
                }
            ) {
                if (pageViewState.fastSelect) {
                    MediaItemFastSelect(
                        modifier = Modifier
                            .customAnimateItem(scope = this),
                        mediaResource = it,
                        imageEngine = pageViewState.imageEngine,
                        onClickMedia = selectMediaInFastSelectMode
                    )
                } else {
                    val mediaPlacement by remember {
                        derivedStateOf {
                            val index = mSelectedResources.indexOf(element = it)
                            val isSelected = index > -1
                            val isEnabled =
                                isSelected || mSelectedResources.size < pageViewState.maxSelectable
                            val position = if (isSelected) {
                                (index + 1).toString()
                            } else {
                                ""
                            }
                            MediaPlacement(
                                isSelected = isSelected,
                                isEnabled = isEnabled,
                                position = position
                            )
                        }
                    }
                    MediaItem(
                        modifier = Modifier
                            .customAnimateItem(scope = this),
                        mediaResource = it,
                        mediaPlacement = mediaPlacement,
                        imageEngine = pageViewState.imageEngine,
                        onClickMedia = pageViewState.onClickMedia,
                        onClickCheckBox = pageViewState.onMediaCheckChanged
                    )
                }
            }
        }
    }
}

private fun Modifier.customAnimateItem(scope: LazyGridItemScope): Modifier {
    return with(scope) {
        animateItem(
            fadeInSpec = spring(stiffness = Spring.StiffnessHigh),
            fadeOutSpec = spring(stiffness = Spring.StiffnessHigh),
            placementSpec = spring(
                stiffness = 800f,
                visibilityThreshold = IntOffset.VisibilityThreshold
            )
        )
    }
}

@Composable
private fun CaptureItem(
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clip(shape = RoundedCornerShape(size = 4.dp))
            .background(color = colorResource(id = R.color.matisse_capture_item_background_color))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(fraction = 0.5f),
            imageVector = Icons.Filled.PhotoCamera,
            tint = colorResource(id = R.color.matisse_capture_item_icon_color),
            contentDescription = "Capture"
        )
    }
}

@Stable
private data class MediaPlacement(
    val isSelected: Boolean,
    val isEnabled: Boolean,
    val position: String
)

@Composable
private fun MediaItem(
    modifier: Modifier,
    mediaResource: MediaResource,
    mediaPlacement: MediaPlacement,
    imageEngine: ImageEngine,
    onClickMedia: (MediaResource) -> Unit,
    onClickCheckBox: (MediaResource) -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .background(color = colorResource(id = R.color.matisse_media_item_background_color)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    onClickMedia(mediaResource)
                },
            contentAlignment = Alignment.Center
        ) {
            imageEngine.Thumbnail(mediaResource = mediaResource)
            if (mediaResource.isVideo) {
                VideoIcon(
                    modifier = Modifier
                        .size(size = 30.dp)
                )
            }
            val scrimColor by animateColorAsState(
                targetValue = if (mediaPlacement.isSelected) {
                    colorResource(id = R.color.matisse_media_item_scrim_color_when_selected)
                } else {
                    colorResource(id = R.color.matisse_media_item_scrim_color_when_unselected)
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = scrimColor)
            )
        }
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopEnd)
                .fillMaxSize(fraction = 0.30f)
                .clickableNoRipple {
                    onClickCheckBox(mediaResource)
                },
            contentAlignment = Alignment.Center
        ) {
            MatisseCheckbox(
                modifier = Modifier
                    .fillMaxSize(fraction = 0.65f),
                text = mediaPlacement.position,
                checked = mediaPlacement.isSelected,
                enabled = mediaPlacement.isEnabled,
                onClick = {
                    onClickCheckBox(mediaResource)
                }
            )
        }
    }
}

@Composable
private fun MediaItemFastSelect(
    modifier: Modifier,
    mediaResource: MediaResource,
    imageEngine: ImageEngine,
    onClickMedia: (MediaResource) -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .background(color = colorResource(id = R.color.matisse_media_item_background_color))
            .clickable {
                onClickMedia(mediaResource)
            },
        contentAlignment = Alignment.Center
    ) {
        imageEngine.Thumbnail(mediaResource = mediaResource)
        if (mediaResource.isVideo) {
            VideoIcon(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .size(size = 30.dp)
            )
        }
    }
}

@Composable
internal fun VideoIcon(modifier: Modifier) {
    Box(
        modifier = modifier
            .shadow(elevation = 0.6.dp, shape = CircleShape)
            .clip(shape = CircleShape)
            .background(color = colorResource(id = R.color.matisse_video_icon_color)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(fraction = 0.60f),
            imageVector = Icons.Filled.PlayArrow,
            tint = Color.Black,
            contentDescription = null
        )
    }
}