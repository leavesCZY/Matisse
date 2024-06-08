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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
    pageViewState: MatissePreviewPageViewState,
    requestOpenVideo: (MediaResource) -> Unit,
    onClickSure: () -> Unit
) {
    AnimatedVisibility(
        visible = pageViewState.visible,
        enter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            ),
            initialOffsetX = { it }
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            ),
            targetOffsetX = { it }
        )
    ) {
        BackHandler(
            enabled = pageViewState.visible,
            onBack = pageViewState.onDismissRequest
        )
        val previewResources = pageViewState.previewResources
        val pagerState = rememberPagerState(
            initialPage = pageViewState.initialPage,
            initialPageOffsetFraction = 0f
        ) {
            previewResources.size
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(
                left = 0.dp,
                right = 0.dp,
                top = 0.dp,
                bottom = 0.dp
            ),
            containerColor = colorResource(id = R.color.matisse_preview_page_background_color)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = paddingValues)
            ) {
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(bottom = bottomControllerHeight),
                    state = pagerState,
                    pageSpacing = 0.dp,
                    verticalAlignment = Alignment.CenterVertically,
                    key = { index ->
                        previewResources[index].id
                    }
                ) { pageIndex ->
                    PreviewPage(
                        pagerState = pagerState,
                        pageIndex = pageIndex,
                        imageEngine = pageViewState.imageEngine,
                        mediaResource = previewResources[pageIndex],
                        requestOpenVideo = requestOpenVideo
                    )
                }
                BottomController(
                    maxSelectable = pageViewState.maxSelectable,
                    pageViewState = pageViewState,
                    currentPageIndex = pagerState.currentPage,
                    onClickSure = onClickSure
                )
            }
        }
    }
}

@Composable
private fun PreviewPage(
    pagerState: PagerState,
    pageIndex: Int,
    imageEngine: ImageEngine,
    mediaResource: MediaResource,
    requestOpenVideo: (MediaResource) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val pageOffset =
                        ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue
                    val fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    lerp(
                        start = 0.84f,
                        stop = 1f,
                        fraction = fraction
                    ).also { scale ->
                        scaleX = scale
                        scaleY = scale
                    }
                    alpha = lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = fraction
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            imageEngine.Image(mediaResource = mediaResource)
            if (mediaResource.isVideo) {
                Icon(
                    modifier = Modifier
                        .clip(shape = CircleShape)
                        .clickable {
                            requestOpenVideo(mediaResource)
                        }
                        .padding(all = 10.dp)
                        .size(size = 48.dp),
                    imageVector = Icons.Filled.PlayCircleOutline,
                    tint = colorResource(id = R.color.matisse_video_icon_color),
                    contentDescription = mediaResource.name
                )
            }
        }
    }
}

private val bottomControllerHeight = 56.dp

@Composable
private fun BoxScope.BottomController(
    maxSelectable: Int,
    pageViewState: MatissePreviewPageViewState,
    currentPageIndex: Int,
    onClickSure: () -> Unit
) {
    val selectedResources = pageViewState.selectedResources
    val previewResources = pageViewState.previewResources
    val imagePosition by remember(key1 = selectedResources, key2 = currentPageIndex) {
        mutableIntStateOf(
            value = selectedResources.indexOf(element = previewResources[currentPageIndex])
        )
    }
    val checkboxEnabled by remember(key1 = imagePosition) {
        mutableStateOf(
            value = imagePosition > -1 || selectedResources.size < maxSelectable
        )
    }
    Box(
        modifier = Modifier
            .align(alignment = Alignment.BottomCenter)
            .background(color = colorResource(id = R.color.matisse_preview_page_controller_background_color))
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(height = bottomControllerHeight)
    ) {
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterStart)
                .clickable(onClick = pageViewState.onDismissRequest)
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
                .wrapContentSize(align = Alignment.Center),
            text = stringResource(id = R.string.matisse_back),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = colorResource(id = R.color.matisse_back_text_color)
        )
        MatisseCheckbox(
            modifier = Modifier
                .align(alignment = Alignment.Center),
            text = if (imagePosition > -1) {
                (imagePosition + 1).toString()
            } else {
                ""
            },
            checked = imagePosition > -1,
            enabled = checkboxEnabled,
            onClick = {
                pageViewState.onMediaCheckChanged(previewResources[currentPageIndex])
            }
        )
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .then(
                    other = if (pageViewState.sureButtonClickable) {
                        Modifier.clickable(onClick = onClickSure)
                    } else {
                        Modifier
                    }
                )
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
                .wrapContentSize(align = Alignment.Center),
            text = pageViewState.sureButtonText,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = colorResource(
                id = if (pageViewState.sureButtonClickable) {
                    R.color.matisse_sure_text_color
                } else {
                    R.color.matisse_sure_text_color_if_disable
                }
            )
        )
    }
}