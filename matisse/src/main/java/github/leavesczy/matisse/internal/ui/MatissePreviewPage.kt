package github.leavesczy.matisse.internal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatissePreviewPageViewState
import github.leavesczy.matisse.internal.utils.clickableNoRipple
import github.leavesczy.matisse.internal.utils.isVideo

/**
 * @Author: CZY
 * @Date: 2022/6/1 19:14
 * @Desc:
 */
@Composable
internal fun MatissePreviewPage(
    matisse: Matisse,
    pageViewState: MatissePreviewPageViewState,
    requestOpenVideo: (MediaResource) -> Unit,
    onSure: () -> Unit
) {
    val visible = pageViewState.visible
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(animationSpec = tween(
            durationMillis = 400, easing = FastOutSlowInEasing
        ), initialOffsetX = { it }),
        exit = slideOutHorizontally(animationSpec = tween(
            durationMillis = 400, easing = FastOutSlowInEasing
        ), targetOffsetX = { it })
    ) {
        BackHandler(enabled = visible, onBack = pageViewState.onDismissRequest)
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
                var controllerVisible by remember {
                    mutableStateOf(value = true)
                }
                val previewResources = pageViewState.previewResources
                val pagerState = rememberPagerState(initialPage = pageViewState.initialPage)
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickableNoRipple {
                            controllerVisible = !controllerVisible
                        },
                    state = pagerState,
                    pageCount = previewResources.size,
                    pageSpacing = 20.dp,
                    verticalAlignment = Alignment.CenterVertically,
                    key = { index ->
                        previewResources[index].id
                    }
                ) { pageIndex ->
                    val mediaResource = previewResources[pageIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        matisse.imageEngine.Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(state = rememberScrollState()),
                            model = mediaResource.uri,
                            contentScale = ContentScale.FillWidth,
                            contentDescription = mediaResource.name
                        )
                        if (mediaResource.isVideo) {
                            Icon(
                                modifier = Modifier
                                    .align(alignment = Alignment.Center)
                                    .clickableNoRipple {
                                        requestOpenVideo(mediaResource)
                                    }
                                    .padding(all = 20.dp)
                                    .size(size = 48.dp),
                                imageVector = Icons.Filled.SlowMotionVideo,
                                tint = colorResource(id = R.color.matisse_video_icon_color),
                                contentDescription = mediaResource.name
                            )
                        }
                    }
                }
                BottomController(
                    visible = controllerVisible,
                    matisse = matisse,
                    pageViewState = pageViewState,
                    currentPageIndex = pagerState.currentPage,
                    onSure = onSure
                )
            }
        }
    }
}

@Composable
private fun BoxScope.BottomController(
    visible: Boolean,
    matisse: Matisse,
    pageViewState: MatissePreviewPageViewState,
    currentPageIndex: Int,
    onSure: () -> Unit
) {
    AnimatedVisibility(
        modifier = Modifier
            .align(alignment = Alignment.BottomCenter)
            .clickableNoRipple {},
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = 100, easing = LinearEasing
            ), initialOffsetY = {
                2 * it
            }),
        exit = slideOutVertically(
            animationSpec = tween(
                durationMillis = 100, easing = LinearEasing
            ), targetOffsetY = {
                it
            })
    ) {
        val selectedResources = pageViewState.selectedResources
        val previewResources = pageViewState.previewResources
        val imagePosition by remember(key1 = selectedResources, key2 = currentPageIndex) {
            mutableStateOf(
                value = selectedResources.indexOf(element = previewResources[currentPageIndex])
            )
        }
        val checkboxEnabled by remember(key1 = imagePosition) {
            mutableStateOf(
                value = imagePosition > -1 || selectedResources.size < matisse.maxSelectable
            )
        }
        Box(
            modifier = Modifier
                .background(color = colorResource(id = R.color.matisse_preview_page_controller_background_color))
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(height = 56.dp)
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
                modifier = Modifier.align(alignment = Alignment.Center),
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
                            Modifier.clickable(onClick = onSure)
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
}