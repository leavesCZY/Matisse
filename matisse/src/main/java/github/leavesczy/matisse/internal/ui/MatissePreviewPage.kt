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
import androidx.compose.runtime.rememberUpdatedState
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
import github.leavesczy.matisse.internal.logic.MatissePreviewPageViewState
import kotlin.math.absoluteValue

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 19:14
 * @Desc:
 */
@Composable
internal fun MatissePreviewPage(
    viewState: MatissePreviewPageViewState,
    imageEngine: ImageEngine,
    requestOpenVideo: (MediaResource) -> Unit,
    onClickSure: () -> Unit
) {
    AnimatedVisibility(
        visible = viewState.visible,
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
        BackHandler(
            enabled = viewState.visible,
            onBack = viewState.onDismissRequest
        )
        val pagerState = rememberPagerState(initialPage = viewState.initialPage) {
            viewState.previewResources.size
        }
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            contentWindowInsets = WindowInsets(
                left = 0.dp,
                right = 0.dp,
                top = 0.dp,
                bottom = 0.dp
            ),
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
                    verticalAlignment = Alignment.CenterVertically,
                    key = { index ->
                        viewState.previewResources[index].id
                    }
                ) { pageIndex ->
                    PreviewPage(
                        modifier = Modifier
                            .fillMaxSize(),
                        pagerState = pagerState,
                        pageIndex = pageIndex,
                        imageEngine = imageEngine,
                        mediaResource = viewState.previewResources[pageIndex],
                        requestOpenVideo = requestOpenVideo
                    )
                }
                BottomController(
                    modifier = Modifier
                        .fillMaxWidth(),
                    pageViewState = viewState,
                    pagerState = pagerState,
                    onClickSure = onClickSure
                )
            }
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
    requestOpenVideo: (MediaResource) -> Unit
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
                            requestOpenVideo(mediaResource)
                        }
                        .padding(all = 10.dp)
                        .size(size = 50.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomController(
    modifier: Modifier,
    pageViewState: MatissePreviewPageViewState,
    pagerState: PagerState,
    onClickSure: () -> Unit
) {
    val mPageViewState by rememberUpdatedState(newValue = pageViewState)
    val imagePosition by remember(key1 = Unit) {
        derivedStateOf {
            mPageViewState.selectedResources.indexOf(element = mPageViewState.previewResources[pagerState.currentPage])
        }
    }
    val checkboxEnabled by remember(key1 = Unit) {
        derivedStateOf {
            imagePosition > -1 || mPageViewState.selectedResources.size < mPageViewState.maxSelectable
        }
    }
    Box(
        modifier = modifier
            .background(color = colorResource(id = R.color.matisse_preview_page_bottom_navigation_bar_background_color))
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(height = 56.dp)
    ) {
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterStart)
                .clip(shape = CircleShape)
                .clickable(onClick = mPageViewState.onDismissRequest)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            text = stringResource(id = R.string.matisse_back),
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal,
            color = colorResource(id = R.color.matisse_preview_page_back_text_color)
        )
        MatisseCheckbox(
            modifier = Modifier
                .align(alignment = Alignment.Center)
                .size(size = 22.dp),
            text = if (imagePosition >= 0) {
                (imagePosition + 1).toString()
            } else {
                ""
            },
            checked = imagePosition > -1,
            enabled = checkboxEnabled,
            onClick = {
                mPageViewState.onMediaCheckChanged(mPageViewState.previewResources[pagerState.currentPage])
            }
        )
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .then(
                    other = if (mPageViewState.sureButtonClickable) {
                        Modifier
                            .clip(shape = CircleShape)
                            .clickable(onClick = onClickSure)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 20.dp, vertical = 6.dp),
            text = mPageViewState.sureButtonText,
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal,
            color = colorResource(
                id = if (mPageViewState.sureButtonClickable) {
                    R.color.matisse_preview_page_sure_text_color
                } else {
                    R.color.matisse_preview_page_sure_text_color_if_disable
                }
            )
        )
    }
}