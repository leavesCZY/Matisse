package github.leavesczy.matisse.internal.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ProgressIndicatorDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseBottomBarViewState
import github.leavesczy.matisse.internal.logic.MatisseMediaCheckChangedHandler
import github.leavesczy.matisse.internal.logic.MatisseMediaItem
import github.leavesczy.matisse.internal.logic.MatisseMediaResourceClickHandler
import github.leavesczy.matisse.internal.logic.MatisseMediaSelectState
import github.leavesczy.matisse.internal.logic.MatissePageViewState
import github.leavesczy.matisse.internal.logic.MatissePlaceholderState

@Composable
internal fun MatissePage(
    pageViewState: MatissePageViewState,
    bottomBarViewState: MatisseBottomBarViewState,
    isSelectionLimitReached: Boolean,
    onCaptureClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onFastSelectMediaClick: MatisseMediaResourceClickHandler
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
                isMediaBucketsLoading = pageViewState.isMediaBucketsLoading,
                onBucketMenuOpen = pageViewState.onBucketMenuOpen,
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
                            isSelectionLimitReached = isSelectionLimitReached,
                            onCaptureClick = onCaptureClick,
                            onFastSelectMediaClick = onFastSelectMediaClick
                        )
                    }
                }

                is MatissePlaceholderState.NoPermission -> {
                    MatisseNoPermissionPlaceholder(
                        modifier = Modifier
                            .align(alignment = Alignment.Center)
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
    isSelectionLimitReached: Boolean,
    onCaptureClick: () -> Unit,
    onFastSelectMediaClick: MatisseMediaResourceClickHandler
) {
    val lazyPagingItems = pageViewState.mediaPagingDataFlow.collectAsLazyPagingItems()
    val lazyGridState = rememberLazyGridState()
    val refreshLoadState = lazyPagingItems.loadState.refresh
    val capturedMediaItems = if (pageViewState.selectedBucket.supportsCapture) {
        pageViewState.capturedMediaItems
    } else {
        emptyList()
    }
    LaunchedEffect(key1 = pageViewState.selectedBucket.bucketId) {
        lazyGridState.animateScrollToItem(index = 0)
    }
    LaunchedEffect(key1 = capturedMediaItems.firstOrNull()?.mediaId) {
        if (capturedMediaItems.isNotEmpty()) {
            lazyGridState.animateScrollToItem(index = 0)
        }
    }
    Box(modifier = modifier) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize(),
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
                        onCaptureClick = onCaptureClick
                    )
                }
            }
            items(
                count = capturedMediaItems.size,
                key = { index ->
                    "captured_${capturedMediaItems[index].mediaId}"
                },
                contentType = {
                    "MediaItem"
                }
            ) { index ->
                val mediaItem = capturedMediaItems[index]
                MediaListItem(
                    lazyGridItemScope = this,
                    pageViewState = pageViewState,
                    mediaItem = mediaItem,
                    isSelectionLimitReached = isSelectionLimitReached,
                    capturedMediaItems = capturedMediaItems,
                    lazyPagingItems = lazyPagingItems,
                    onFastSelectMediaClick = onFastSelectMediaClick
                )
            }
            items(
                count = lazyPagingItems.itemCount,
                key = { index ->
                    val mediaId = lazyPagingItems.peek(index = index)?.mediaId
                    if (mediaId != null) {
                        "media_$mediaId"
                    } else {
                        "placeholder_$index"
                    }
                },
                contentType = {
                    "MediaItem"
                }
            ) { index ->
                val mediaItem = lazyPagingItems[index] ?: return@items
                MediaListItem(
                    lazyGridItemScope = this,
                    pageViewState = pageViewState,
                    mediaItem = mediaItem,
                    isSelectionLimitReached = isSelectionLimitReached,
                    capturedMediaItems = capturedMediaItems,
                    lazyPagingItems = lazyPagingItems,
                    onFastSelectMediaClick = onFastSelectMediaClick
                )
            }
        }
        when {
            refreshLoadState is LoadState.Loading &&
                    lazyPagingItems.itemCount == 0 &&
                    capturedMediaItems.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(size = 42.dp)
                        .align(alignment = Alignment.Center),
                    strokeWidth = 3.dp,
                    color = colorResource(id = R.color.matisse_loading_indicator_color),
                    trackColor = Color.Transparent,
                    strokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap
                )
            }

            lazyPagingItems.itemCount == 0 && capturedMediaItems.isEmpty() -> {
                MatisseEmptyPlaceholder(
                    modifier = Modifier
                        .align(alignment = Alignment.Center),
                    includesImage = pageViewState.matisse.mediaType.includesImage,
                    includesVideo = pageViewState.matisse.mediaType.includesVideo
                )
            }
        }
    }
}

@Composable
private fun MediaListItem(
    lazyGridItemScope: LazyGridItemScope,
    pageViewState: MatissePageViewState,
    mediaItem: MatisseMediaItem,
    isSelectionLimitReached: Boolean,
    capturedMediaItems: List<MatisseMediaItem>,
    lazyPagingItems: LazyPagingItems<MatisseMediaItem>,
    onFastSelectMediaClick: MatisseMediaResourceClickHandler
) {
    if (pageViewState.matisse.fastSelect) {
        MediaItemFastSelect(
            modifier = Modifier
                .matisseAnimateItem(lazyGridItemScope = lazyGridItemScope),
            mediaResource = mediaItem.mediaResource,
            imageEngine = pageViewState.matisse.imageEngine,
            onMediaClick = onFastSelectMediaClick
        )
    } else {
        MediaItem(
            modifier = Modifier
                .matisseAnimateItem(lazyGridItemScope = lazyGridItemScope),
            mediaItem = mediaItem,
            imageEngine = pageViewState.matisse.imageEngine,
            isSelectionLimitReached = isSelectionLimitReached,
            maxSelectable = pageViewState.matisse.maxSelectable,
            onMediaClick = {
                val previewMediaItems = buildList {
                    addAll(elements = capturedMediaItems)
                    addAll(elements = lazyPagingItems.itemSnapshotList.items)
                }
                pageViewState.onMediaClick(
                    mediaItem = mediaItem,
                    previewMediaItems = previewMediaItems
                )
            },
            onMediaCheckChanged = pageViewState.onMediaCheckChanged
        )
    }
}

@Composable
private fun CaptureItem(
    modifier: Modifier,
    onCaptureClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clip(shape = RoundedCornerShape(size = 4.dp))
            .background(color = colorResource(id = R.color.matisse_capture_background_color))
            .clickable(onClick = onCaptureClick),
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
private fun MediaItem(
    modifier: Modifier,
    mediaItem: MatisseMediaItem,
    imageEngine: ImageEngine,
    isSelectionLimitReached: Boolean,
    maxSelectable: Int,
    onMediaClick: () -> Unit,
    onMediaCheckChanged: MatisseMediaCheckChangedHandler
) {
    val onCheckedChange = remember(key1 = mediaItem.mediaId, key2 = onMediaCheckChanged) {
        {
            onMediaCheckChanged(mediaItem = mediaItem)
        }
    }
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clickable(onClick = onMediaClick),
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
            isSelectionLimitReached = isSelectionLimitReached,
            maxSelectable = maxSelectable,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun BoxScope.MediaItemSelectionOverlay(
    selectionState: State<MatisseMediaSelectState>,
    isSelectionLimitReached: Boolean,
    maxSelectable: Int,
    onCheckedChange: () -> Unit
) {
    MediaItemScrim(
        modifier = Modifier,
        isSelected = selectionState.value.isSelected
    )
    Box(
        modifier = Modifier
            .align(alignment = Alignment.TopEnd)
            .fillMaxSize(fraction = 0.29f),
        contentAlignment = Alignment.Center
    ) {
        MatisseCheckbox(
            modifier = Modifier
                .fillMaxSize(fraction = 0.80f),
            selectionState = selectionState,
            isSelectionLimitReached = isSelectionLimitReached,
            maxSelectable = maxSelectable,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun MediaItemScrim(
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
    onMediaClick: MatisseMediaResourceClickHandler
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clickable {
                onMediaClick(mediaResource = mediaResource)
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
            fadeInSpec = tween(durationMillis = 120),
            fadeOutSpec = tween(durationMillis = 120),
            placementSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold
            )
        )
    }
}
