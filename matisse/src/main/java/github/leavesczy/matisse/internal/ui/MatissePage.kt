package github.leavesczy.matisse.internal.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseBottomBarViewState
import github.leavesczy.matisse.internal.logic.MatisseMediaItem
import github.leavesczy.matisse.internal.logic.MatisseMediaSelectState
import github.leavesczy.matisse.internal.logic.MatissePageViewState
import github.leavesczy.matisse.internal.logic.MatissePlaceholderState

@Composable
internal fun MatissePage(
    pageViewState: MatissePageViewState,
    bottomBarViewState: MatisseBottomBarViewState,
    selectionLimitReached: Boolean,
    onTakePictureRequest: () -> Unit,
    onConfirmClick: () -> Unit,
    onFastSelectMedia: (MediaResource) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = colorResource(id = R.color.matisse_main_page_background_color),
        topBar = {
            MatisseTopBar(
                modifier = Modifier,
                bucketName = pageViewState.selectedBucket.bucketName,
                mediaBuckets = pageViewState.mediaBuckets,
                onBucketClick = pageViewState.onBucketClick,
                imageEngine = pageViewState.matisse.imageEngine
            )
        },
        bottomBar = {
            if (!pageViewState.matisse.fastSelect) {
                MatisseBottomBar(
                    modifier = Modifier,
                    bottomBarViewState = bottomBarViewState,
                    onConfirmClick = onConfirmClick
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .fillMaxSize()
        ) {
            when (val placeholderState = pageViewState.placeholderState) {
                is MatissePlaceholderState.Ready -> {
                    if (placeholderState.hasReadMediaPermission) {
                        MediaList(
                            modifier = Modifier
                                .fillMaxSize(),
                            pageViewState = pageViewState,
                            selectionLimitReached = selectionLimitReached,
                            onTakePictureRequest = onTakePictureRequest,
                            onFastSelectMedia = onFastSelectMedia
                        )
                    }
                }

                is MatissePlaceholderState.NoPermission -> {
                    MatisseNoPermissionPlaceholder(
                        modifier = Modifier
                            .align(alignment = Alignment.Center)
                    )
                }

                is MatissePlaceholderState.NoMedia -> {
                    if (pageViewState.selectedBucket.supportsCapture) {
                        CaptureItem(
                            modifier = Modifier,
                            gridColumns = pageViewState.matisse.gridColumns,
                            onTakePictureRequest = onTakePictureRequest
                        )
                    }
                    MatisseEmptyPlaceholder(
                        modifier = Modifier
                            .align(alignment = Alignment.Center),
                        includesImages = placeholderState.includesImages,
                        includesVideos = placeholderState.includesVideos
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaList(
    modifier: Modifier,
    pageViewState: MatissePageViewState,
    selectionLimitReached: Boolean,
    onTakePictureRequest: () -> Unit,
    onFastSelectMedia: (MediaResource) -> Unit
) {
    val lazyGridState = rememberLazyGridState()
    LaunchedEffect(key1 = pageViewState.selectedBucket.bucketId) {
        lazyGridState.animateScrollToItem(index = 0)
    }
    LazyVerticalGrid(
        modifier = modifier,
        state = lazyGridState,
        columns = GridCells.Fixed(count = pageViewState.matisse.gridColumns),
        horizontalArrangement = Arrangement.spacedBy(space = 1.dp),
        verticalArrangement = Arrangement.spacedBy(space = 1.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        if (pageViewState.selectedBucket.supportsCapture) {
            item(
                key = "CaptureItem",
                contentType = "CaptureItem"
            ) {
                CaptureItem(
                    modifier = Modifier
                        .matisseAnimateItem(lazyGridItemScope = this),
                    onTakePictureClick = onTakePictureRequest
                )
            }
        }
        items(
            items = pageViewState.selectedBucket.mediaItems,
            key = {
                it.mediaId
            },
            contentType = {
                "MediaItem"
            }
        ) {
            if (pageViewState.matisse.fastSelect) {
                MediaItemFastSelect(
                    modifier = Modifier
                        .matisseAnimateItem(lazyGridItemScope = this),
                    mediaResource = it.mediaResource,
                    imageEngine = pageViewState.matisse.imageEngine,
                    onMediaClick = onFastSelectMedia
                )
            } else {
                MediaItem(
                    modifier = Modifier
                        .matisseAnimateItem(lazyGridItemScope = this),
                    mediaItem = it,
                    imageEngine = pageViewState.matisse.imageEngine,
                    selectionLimitReached = selectionLimitReached,
                    maxSelectable = pageViewState.matisse.maxSelectable,
                    onMediaClick = pageViewState.onMediaClick,
                    onMediaCheckChanged = pageViewState.onMediaCheckChanged
                )
            }
        }
    }
}

@Composable
private fun CaptureItem(
    modifier: Modifier,
    onTakePictureClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clip(shape = RoundedCornerShape(size = 4.dp))
            .background(color = colorResource(id = R.color.matisse_capture_background_color))
            .clickable(onClick = onTakePictureClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(fraction = 0.5f),
            painter = painterResource(id = R.drawable.ic_matisse_photo_camera),
            tint = colorResource(id = R.color.matisse_capture_icon_color),
            contentDescription = null
        )
    }
}

@Composable
private fun CaptureItem(
    modifier: Modifier,
    gridColumns: Int,
    onTakePictureRequest: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CaptureItem(
            modifier = Modifier
                .weight(weight = 1f),
            onTakePictureClick = onTakePictureRequest
        )
        Spacer(
            modifier = Modifier
                .weight(weight = (gridColumns - 1).toFloat())
        )
    }
}

@Composable
private fun MediaItem(
    modifier: Modifier,
    mediaItem: MatisseMediaItem,
    imageEngine: ImageEngine,
    selectionLimitReached: Boolean,
    maxSelectable: Int,
    onMediaClick: (MatisseMediaItem) -> Unit,
    onMediaCheckChanged: (MatisseMediaItem) -> Unit
) {
    val onCheckedChange = remember(key1 = mediaItem.mediaId, key2 = onMediaCheckChanged) {
        {
            onMediaCheckChanged(mediaItem)
        }
    }
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clickable {
                onMediaClick(mediaItem)
            },
        contentAlignment = Alignment.Center
    ) {
        imageEngine.Thumbnail(mediaResource = mediaItem.mediaResource)
        if (mediaItem.mediaResource.isVideo) {
            VideoIcon(
                modifier = Modifier
                    .fillMaxSize(fraction = 0.24f)
            )
        }
        MediaItemSelectionOverlay(
            selectionState = mediaItem.selectionState,
            selectionLimitReached = selectionLimitReached,
            maxSelectable = maxSelectable,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun BoxScope.MediaItemSelectionOverlay(
    selectionState: State<MatisseMediaSelectState>,
    selectionLimitReached: Boolean,
    maxSelectable: Int,
    onCheckedChange: () -> Unit
) {
    MediaItemScrimColor(
        modifier = Modifier,
        isSelected = selectionState.value.isSelected
    )
    Box(
        modifier = Modifier
            .align(alignment = Alignment.TopEnd)
            .fillMaxSize(fraction = 0.28f),
        contentAlignment = Alignment.Center
    ) {
        MatisseCheckbox(
            modifier = Modifier
                .fillMaxSize(fraction =  0.80f),
            selectionState = selectionState,
            selectionLimitReached = selectionLimitReached,
            maxSelectable = maxSelectable,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun MediaItemScrimColor(
    modifier: Modifier,
    isSelected: Boolean
) {
    Spacer(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = colorResource(
                    id = if (isSelected) {
                        R.color.matisse_media_item_scrim_selected_color
                    } else {
                        R.color.matisse_media_item_scrim_unselected_color
                    }
                )
            )
    )
}

@Composable
private fun MediaItemFastSelect(
    modifier: Modifier,
    mediaResource: MediaResource,
    imageEngine: ImageEngine,
    onMediaClick: (MediaResource) -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clickable {
                onMediaClick(mediaResource)
            },
        contentAlignment = Alignment.Center
    ) {
        imageEngine.Thumbnail(mediaResource = mediaResource)
        if (mediaResource.isVideo) {
            VideoIcon(
                modifier = Modifier
                    .fillMaxSize(fraction = 0.24f)
            )
        }
    }
}

@Composable
internal fun VideoIcon(modifier: Modifier) {
    Box(
        modifier = modifier
            .shadow(elevation = 1.dp, shape = CircleShape)
            .clip(shape = CircleShape)
            .background(color = colorResource(id = R.color.matisse_media_video_icon_background_color)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(fraction = 0.62f),
            painter = painterResource(id = R.drawable.ic_matisse_play_arrow),
            tint = colorResource(id = R.color.matisse_media_video_icon_color),
            contentDescription = null
        )
    }
}

@Stable
private fun Modifier.matisseAnimateItem(lazyGridItemScope: LazyGridItemScope): Modifier {
    return with(receiver = lazyGridItemScope) {
        animateItem(
            fadeInSpec = spring(stiffness = Spring.StiffnessMedium),
            fadeOutSpec = spring(stiffness = Spring.StiffnessMedium),
            placementSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold
            )
        )
    }
}