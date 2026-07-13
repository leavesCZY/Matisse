package github.leavesczy.matisse.internal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatissePreviewImagePageViewState
import kotlin.math.absoluteValue

@Composable
internal fun MatissePreviewImagePage(
    pageViewState: MatissePreviewImagePageViewState,
    imageEngine: ImageEngine,
    selectionLimitReached: Boolean,
    onConfirmClick: () -> Unit
) {
    AnimatedVisibility(
        modifier = Modifier
            .fillMaxSize(),
        visible = pageViewState.visible,
        enter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = 350,
                easing = FastOutSlowInEasing
            ),
            initialOffsetX = { it }
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = 350,
                easing = FastOutSlowInEasing
            ),
            targetOffsetX = { it }
        )
    ) {
        MatissePreviewImagePageContent(
            pageViewState = pageViewState,
            imageEngine = imageEngine,
            selectionLimitReached = selectionLimitReached,
            onConfirmClick = onConfirmClick
        )
    }
}

@Composable
private fun MatissePreviewImagePageContent(
    pageViewState: MatissePreviewImagePageViewState,
    imageEngine: ImageEngine,
    selectionLimitReached: Boolean,
    onConfirmClick: () -> Unit
) {
    BackHandler(
        enabled = pageViewState.visible,
        onBack = pageViewState.onDismissRequest
    )
    val pagerState = rememberPagerState(initialPage = pageViewState.initialPage) {
        pageViewState.previewMediaItems.size
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .clickableNoRipple(onClick = {}),
        contentWindowInsets = WindowInsets(),
        containerColor = colorResource(id = R.color.matisse_preview_page_background_color)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .fillMaxSize()
        ) {
            HorizontalPager(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f),
                state = pagerState,
                key = { index ->
                    pageViewState.previewMediaItems[index].mediaId
                }
            ) { pageIndex ->
                PreviewPage(
                    modifier = Modifier
                        .fillMaxSize(),
                    pagerState = pagerState,
                    pageIndex = pageIndex,
                    imageEngine = imageEngine,
                    mediaResource = pageViewState.previewMediaItems[pageIndex].mediaResource,
                    onOpenVideoClick = pageViewState.onOpenVideoClick
                )
            }
            PreviewBottomBar(
                modifier = Modifier
                    .fillMaxWidth(),
                pageViewState = pageViewState,
                pagerState = pagerState,
                selectionLimitReached = selectionLimitReached,
                onConfirmClick = onConfirmClick
            )
        }
    }
}

@Composable
private fun PreviewPage(
    modifier: Modifier,
    pagerState: PagerState,
    pageIndex: Int,
    imageEngine: ImageEngine,
    mediaResource: MediaResource,
    onOpenVideoClick: (MediaResource) -> Unit
) {
    val fraction by remember {
        derivedStateOf {
            val pageOffset =
                (pagerState.currentPage - pageIndex + pagerState.currentPageOffsetFraction).absoluteValue
            val progress = 1f - pageOffset.coerceIn(0f, 1f)
            lerp(
                start = 0.80f,
                stop = 1f,
                fraction = progress
            )
        }
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = fraction
                    scaleY = fraction
                    alpha = fraction
                },
            contentAlignment = Alignment.Center
        ) {
            imageEngine.Image(mediaResource = mediaResource)
            if (mediaResource.isVideo) {
                VideoIcon(
                    modifier = Modifier
                        .clip(shape = CircleShape)
                        .clickable {
                            onOpenVideoClick(mediaResource)
                        }
                        .padding(all = 10.dp)
                        .size(size = 50.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewBottomBar(
    modifier: Modifier,
    pageViewState: MatissePreviewImagePageViewState,
    pagerState: PagerState,
    selectionLimitReached: Boolean,
    onConfirmClick: () -> Unit
) {
    val currentResource by remember {
        derivedStateOf {
            pageViewState.previewMediaItems[pagerState.currentPage]
        }
    }
    val onCheckedChange = remember(
        key1 = currentResource.mediaId,
        key2 = pageViewState.onMediaCheckChanged
    ) {
        {
            pageViewState.onMediaCheckChanged(currentResource)
        }
    }
    Box(
        modifier = modifier
            .background(color = colorResource(id = R.color.matisse_preview_page_bottom_bar_background_color))
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(height = 56.dp)
    ) {
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterStart)
                .clip(shape = CircleShape)
                .clickable(onClick = pageViewState.onDismissRequest)
                .padding(horizontal = 22.dp, vertical = 6.dp),
            text = stringResource(id = R.string.matisse_action_back),
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal,
            color = colorResource(id = R.color.matisse_preview_page_back_text_color)
        )
        MatisseCheckbox(
            modifier = Modifier
                .align(alignment = Alignment.Center)
                .size(size = 27.dp),
            selectionState = currentResource.selectionState,
            selectionLimitReached = selectionLimitReached,
            maxSelectable = pageViewState.maxSelectable,
            onCheckedChange = onCheckedChange
        )
        val selectedMediaCount = pageViewState.selectedMediaCount
        val maxSelectable = pageViewState.maxSelectable
        val isConfirmEnabled = selectedMediaCount in 1..maxSelectable
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .then(
                    other = if (isConfirmEnabled) {
                        Modifier
                            .clip(shape = CircleShape)
                            .clickable(onClick = onConfirmClick)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 22.dp, vertical = 6.dp),
            text = if (maxSelectable > 1) {
                stringResource(
                    id = R.string.matisse_action_confirm_with_count,
                    selectedMediaCount,
                    maxSelectable
                )
            } else {
                stringResource(id = R.string.matisse_action_confirm)
            },
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal,
            color = colorResource(
                id = if (isConfirmEnabled) {
                    R.color.matisse_preview_page_confirm_text_color
                } else {
                    R.color.matisse_preview_page_confirm_text_disabled_color
                }
            )
        )
    }
}