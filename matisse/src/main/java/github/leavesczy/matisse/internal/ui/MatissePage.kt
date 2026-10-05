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
    galleryItems: MatisseGalleryItems,
    bottomBarViewState: MatisseBottomBarViewState,
    onCaptureClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onReturnOnTapMediaClick: (mediaResource: MediaResource) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = colorResource(id = R.color.matisse_main_page_background_color),
        topBar = {
            MatisseTopBar(
                modifier = Modifier,
                selectedBucketId = pageViewState.selectedBucket.bucketId,
                selectedBucketName = pageViewState.selectedBucket.bucketName,
                mediaBuckets = pageViewState.mediaBuckets,
                isMediaBucketsLoading = pageViewState.isMediaBucketsLoading,
                onBucketMenuOpen = pageViewState.onBucketMenuOpen,
                onBucketClick = pageViewState.onBucketClick,
                imageEngine = pageViewState.matisse.imageEngine
            )
        },
        bottomBar = {
            if (!pageViewState.matisse.returnOnTap) {
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
            val showMediaList = pageViewState.placeholderState is MatissePlaceholderState.Granted ||
                    pageViewState.selectedBucket.supportsCapture
            if (showMediaList) {
                MediaList(
                    modifier = Modifier
                        .fillMaxSize(),
                    pageViewState = pageViewState,
                    galleryItems = galleryItems,
                    onCaptureClick = onCaptureClick,
                    onReturnOnTapMediaClick = onReturnOnTapMediaClick
                )
            } else if (pageViewState.placeholderState is MatissePlaceholderState.NoPermission) {
                MatisseNoPermissionPlaceholder(
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
private fun MediaList(
    modifier: Modifier,
    pageViewState: MatissePageViewState,
    galleryItems: MatisseGalleryItems,
    onCaptureClick: () -> Unit,
    onReturnOnTapMediaClick: (mediaResource: MediaResource) -> Unit
) {
    val lazyGridState = rememberLazyGridState()
    val refreshLoadState = galleryItems.pagingItems.loadState.refresh
    val capturedMediaItems = galleryItems.capturedMediaItems
    LaunchedEffect(key1 = pageViewState.selectedBucket.bucketId) {
        lazyGridState.scrollToItem(index = 0)
    }
    LaunchedEffect(key1 = capturedMediaItems.firstOrNull()?.mediaId) {
        if (capturedMediaItems.isNotEmpty()) {
            lazyGridState.animateScrollToItem(index = 0)
        }
    }
    val gridSpacing = 1.dp
    val hasCaptureItem = pageViewState.selectedBucket.supportsCapture
    Box(modifier = modifier) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize(),
            state = lazyGridState,
            columns = GridCells.Fixed(count = pageViewState.matisse.gridColumns),
            horizontalArrangement = Arrangement.spacedBy(space = gridSpacing),
            verticalArrangement = Arrangement.spacedBy(space = gridSpacing),
            contentPadding = PaddingValues(
                start = gridSpacing,
                end = gridSpacing,
                bottom = 20.dp
            )
        ) {
            if (hasCaptureItem) {
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
                count = galleryItems.itemCount,
                key = galleryItems::keyAt,
                contentType = {
                    "MediaItem"
                }
            ) { index ->
                val mediaItem = galleryItems[index] ?: return@items
                MediaListItem(
                    lazyGridItemScope = this,
                    pageViewState = pageViewState,
                    mediaItem = mediaItem,
                    galleryIndex = index,
                    onReturnOnTapMediaClick = onReturnOnTapMediaClick
                )
            }
        }
        val galleryEmpty = galleryItems.itemCount == 0
        val placeholderState = pageViewState.placeholderState
        val showNoPermission =
            placeholderState is MatissePlaceholderState.NoPermission && galleryEmpty
        val showGrantedOverlay = placeholderState is MatissePlaceholderState.Granted && galleryEmpty
        if (showNoPermission || showGrantedOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    showNoPermission -> {
                        MatisseNoPermissionPlaceholder(
                            modifier = Modifier,
                            includesImage = pageViewState.matisse.mediaType.includesImage,
                            includesVideo = pageViewState.matisse.mediaType.includesVideo
                        )
                    }

                    refreshLoadState is LoadState.Error -> {
                        MatisseLoadErrorPlaceholder(
                            modifier = Modifier,
                            onRetry = galleryItems.pagingItems::retry
                        )
                    }

                    refreshLoadState is LoadState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(size = 42.dp),
                            strokeWidth = 3.dp,
                            color = colorResource(id = R.color.matisse_loading_indicator_color),
                            trackColor = Color.Transparent,
                            strokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap
                        )
                    }

                    else -> {
                        MatisseEmptyPlaceholder(
                            modifier = Modifier,
                            includesImage = pageViewState.matisse.mediaType.includesImage,
                            includesVideo = pageViewState.matisse.mediaType.includesVideo
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaListItem(
    lazyGridItemScope: LazyGridItemScope,
    pageViewState: MatissePageViewState,
    mediaItem: MatisseMediaItem,
    galleryIndex: Int,
    onReturnOnTapMediaClick: (mediaResource: MediaResource) -> Unit
) {
    if (pageViewState.matisse.returnOnTap) {
        MediaItemReturnOnTap(
            modifier = Modifier
                .matisseAnimateItem(lazyGridItemScope = lazyGridItemScope),
            mediaResource = mediaItem.mediaResource,
            imageEngine = pageViewState.matisse.imageEngine,
            onMediaClick = onReturnOnTapMediaClick
        )
    } else {
        MediaItem(
            modifier = Modifier
                .matisseAnimateItem(lazyGridItemScope = lazyGridItemScope),
            mediaItem = mediaItem,
            imageEngine = pageViewState.matisse.imageEngine,
            selectionStateFor = pageViewState.selectionStateFor,
            isSelectionLimitReached = pageViewState.isSelectionLimitReached,
            maxSelectable = pageViewState.matisse.maxSelectable,
            onMediaClick = {
                pageViewState.onMediaClick(galleryIndex)
            },
            onToggleMediaSelection = pageViewState.onToggleMediaSelection
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
    selectionStateFor: (mediaId: Long) -> MatisseMediaSelectState,
    isSelectionLimitReached: () -> Boolean,
    maxSelectable: Int,
    onMediaClick: () -> Unit,
    onToggleMediaSelection: (mediaItem: MatisseMediaItem) -> Unit
) {
    val onCheckedChange = remember(key1 = mediaItem.mediaId, key2 = onToggleMediaSelection) {
        {
            onToggleMediaSelection(mediaItem)
        }
    }
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f),
        contentAlignment = Alignment.Center
    ) {
        imageEngine.Thumbnail(mediaResource = mediaItem.mediaResource)
        if (mediaItem.mediaResource.isVideo) {
            MatisseVideoIcon(
                modifier = Modifier
                    .fillMaxSize(fraction = 0.24f)
            )
        }
        MediaItemSelectionOverlay(
            mediaId = mediaItem.mediaId,
            selectionStateFor = selectionStateFor,
            isSelectionLimitReached = isSelectionLimitReached,
            maxSelectable = maxSelectable,
            onMediaClick = onMediaClick,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 选中状态只在这一层读取：选中表是同一个 SnapshotStateMap，任何写入都会让所有读取方失效，
 * 若在 [MediaItem] 中读取会让每个可见格子的缩略图一起重组。
 */
@Composable
private fun BoxScope.MediaItemSelectionOverlay(
    mediaId: Long,
    selectionStateFor: (mediaId: Long) -> MatisseMediaSelectState,
    isSelectionLimitReached: () -> Boolean,
    maxSelectable: Int,
    onMediaClick: () -> Unit,
    onCheckedChange: () -> Unit
) {
    val selectionState = selectionStateFor(mediaId)
    MediaItemScrim(
        modifier = Modifier
            .clickable(onClick = onMediaClick),
        isSelected = selectionState.isSelected
    )
    Box(
        modifier = Modifier
            .align(alignment = Alignment.TopEnd)
            .fillMaxSize(fraction = 0.27f),
        contentAlignment = Alignment.Center
    ) {
        MatisseCheckbox(
            modifier = Modifier
                .fillMaxSize(fraction = 0.70f),
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
private fun MediaItemReturnOnTap(
    modifier: Modifier,
    mediaResource: MediaResource,
    imageEngine: ImageEngine,
    onMediaClick: (mediaResource: MediaResource) -> Unit
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
            MatisseVideoIcon(
                modifier = Modifier
                    .fillMaxSize(fraction = 0.24f)
            )
        }
    }
}

@Composable
internal fun MatisseVideoIcon(modifier: Modifier) {
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
