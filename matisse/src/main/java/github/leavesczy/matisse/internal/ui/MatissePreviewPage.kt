package github.leavesczy.matisse.internal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.utils.clickableNoRipple

/**
 * @Author: CZY
 * @Date: 2022/6/1 19:14
 * @Desc:
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MatissePreviewPage(viewModel: MatisseViewModel) {
    val matissePreviewViewState = viewModel.matissePreviewViewState
    val visible = matissePreviewViewState.visible
    var controllerVisible by remember(key1 = visible) {
        mutableStateOf(value = true)
    }
    BackHandler(enabled = visible, onBack = {
        viewModel.dismissPreviewPage()
    })
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(animationSpec = tween(
            durationMillis = 280, easing = LinearEasing
        ), initialOffsetX = { it }),
        exit = slideOutHorizontally(animationSpec = tween(
            durationMillis = 280, easing = LinearEasing
        ), targetOffsetX = { it })
    ) {
        val previewResources = matissePreviewViewState.previewResources
        val initialPage = matissePreviewViewState.initialPage
        val pagerState = rememberPagerState(initialPage = initialPage)
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
            if (previewResources.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = paddingValues)
                ) {
                    HorizontalPager(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickableNoRipple {
                                controllerVisible = !controllerVisible
                            },
                        count = previewResources.size,
                        state = pagerState,
                        key = { index ->
                            previewResources[index].key
                        }
                    ) { pageIndex ->
                        val mediaResource = previewResources[pageIndex]
                        AsyncImage(
                            modifier = Modifier
                                .align(alignment = Alignment.Center)
                                .verticalScroll(state = rememberScrollState())
                                .fillMaxWidth(),
                            model = mediaResource.uri,
                            contentScale = ContentScale.FillWidth,
                            contentDescription = mediaResource.displayName
                        )
                    }
                    BottomController(
                        visible = controllerVisible,
                        viewModel = viewModel,
                        pagerState = pagerState,
                        mediaResource = previewResources[pagerState.currentPage],
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.BottomController(
    visible: Boolean,
    viewModel: MatisseViewModel,
    pagerState: PagerState,
    mediaResource: MediaResource
) {
    AnimatedVisibility(
        modifier = Modifier
            .align(alignment = Alignment.BottomCenter)
            .clickableNoRipple {

            },
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
        val currentImageIndex by remember {
            derivedStateOf {
                val viewState = viewModel.matissePreviewViewState
                viewState.selectedResources.indexOf(element = viewState.previewResources[pagerState.currentPage])
            }
        }
        val checkboxEnabled by remember {
            derivedStateOf {
                val viewState = viewModel.matissePreviewViewState
                currentImageIndex > -1 || viewState.selectedResources.size < viewState.matisse.maxSelectable
            }
        }
        val bottomBarViewState = viewModel.bottomBarViewState
        Box(
            modifier = Modifier
                .shadow(elevation = 4.dp)
                .background(color = colorResource(id = R.color.matisse_preview_page_controller_background_color))
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(height = 56.dp)
        ) {
            Text(
                modifier = Modifier
                    .align(alignment = Alignment.CenterStart)
                    .clickable(onClick = viewModel::dismissPreviewPage)
                    .fillMaxHeight()
                    .padding(horizontal = 22.dp)
                    .wrapContentSize(align = Alignment.Center),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = colorResource(id = R.color.matisse_back_text_color),
                    fontSize = 14.sp
                ),
                text = stringResource(id = R.string.matisse_back)
            )
            MatisseCheckbox(
                modifier = Modifier.align(alignment = Alignment.Center),
                size = 24.dp,
                text = if (currentImageIndex > -1) {
                    (currentImageIndex + 1).toString()
                } else {
                    ""
                },
                checked = currentImageIndex > -1,
                enabled = checkboxEnabled,
                onClick = {
                    viewModel.onMediaCheckChanged(mediaResource = mediaResource)
                }
            )
            Text(
                modifier = Modifier
                    .align(alignment = Alignment.CenterEnd)
                    .then(
                        other = if (bottomBarViewState.sureButtonClickable) {
                            Modifier.clickable(onClick = bottomBarViewState.onClickSureButton)
                        } else {
                            Modifier
                        }
                    )
                    .fillMaxHeight()
                    .padding(horizontal = 22.dp)
                    .wrapContentSize(align = Alignment.Center),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = colorResource(
                        id = if (bottomBarViewState.sureButtonClickable) {
                            R.color.matisse_sure_text_color
                        } else {
                            R.color.matisse_sure_text_color_if_disable
                        }
                    ),
                    fontSize = 14.sp
                ),
                text = bottomBarViewState.sureText
            )
        }
    }
}