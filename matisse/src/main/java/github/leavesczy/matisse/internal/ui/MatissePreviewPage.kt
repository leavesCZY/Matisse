package github.leavesczy.matisse.internal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatissePreviewViewState
import github.leavesczy.matisse.internal.theme.LocalMatisseTheme
import kotlin.math.absoluteValue

/**
 * @Author: CZY
 * @Date: 2022/6/1 19:14
 * @Desc:
 */
@Composable
internal fun MatissePreviewPage(viewState: MatissePreviewViewState) {
    val systemUiController = rememberSystemUiController()
    BackHandler(enabled = viewState.visible, onBack = viewState.onDismissRequest)
    AnimatedVisibility(
        modifier = Modifier,
        visible = viewState.visible,
        enter = slideInHorizontally(animationSpec = tween(
            durationMillis = 300,
            easing = LinearEasing
        ), initialOffsetX = { it }),
        exit = slideOutHorizontally(animationSpec = tween(
            durationMillis = 300,
            easing = LinearEasing
        ), targetOffsetX = { it }),
    ) {
        val matisse = viewState.matisse
        val initialPage = viewState.initialPage
        val previewResources = viewState.previewResources
        val selectedMediaResources = viewState.selectedResources
        val onMediaCheckChanged = viewState.onMediaCheckChanged
        val pagerState = rememberPagerState(initialPage = initialPage)
        var currentPageIndex by remember {
            mutableStateOf(initialPage)
        }
        LaunchedEffect(key1 = Unit) {
            snapshotFlow {
                pagerState.currentPage
            }.collect {
                currentPageIndex = it
            }
        }
        DisposableEffect(systemUiController){
            systemUiController.isStatusBarVisible = false
            onDispose {
                systemUiController.isStatusBarVisible = true
            }
        }
        Scaffold(
            modifier = Modifier,
            backgroundColor = LocalMatisseTheme.current.onPreviewSurfaceColor
        ) { paddingValues ->
            if (previewResources.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    var enableUserScroll by remember {
                        mutableStateOf(true)
                    }
                    HorizontalPager(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = paddingValues,
                        count = previewResources.size,
                        state = pagerState,
                        key = { index ->
                            previewResources[index].key
                        },
                        userScrollEnabled = enableUserScroll
                    ) { pageIndex ->
                        val mediaResource = previewResources[pageIndex]
                        ScalableAsyncImage(
                            modifier = Modifier,
                            model = mediaResource.uri,
                            contentScale = ContentScale.FillWidth,
                            contentDescription = stringResource(id = R.string.album),
                            pageOffsetProvider = { calculateCurrentOffsetForPage(pageIndex).absoluteValue },
                            updateUserScrollEnabled = { enableUserScroll = it }
                        )
                    }
                    val index = selectedMediaResources.indexOf(previewResources[currentPageIndex])
                    val isSelected = index > -1
                    val enabled = isSelected || selectedMediaResources.size < matisse.maxSelectable
                    MatisseCheckbox(
                        modifier = Modifier
                            .align(alignment = Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 48.dp, end = 30.dp),
                        theme = LocalMatisseTheme.current.checkBoxTheme,
                        text = if (isSelected) {
                            (index + 1).toString()
                        } else {
                            ""
                        },
                        checked = isSelected,
                        enabled = enabled,
                        onCheckedChange = {
                            onMediaCheckChanged(previewResources[currentPageIndex])
                        }
                    )
                }
            }
        }
    }
}

/**
 * 一个非常简单的可缩放图片
 * TODO: 优化拖动效果（双指缩放体验）
 * TODO: 加载长图
 * @created: FunnySaltyFish 2022年10月7日
 * @param modifier Modifier
 * @param model Any?
 * @param contentScale ContentScale
 * @param contentDescription String?
 * @param pageOffsetProvider Function0<Float>
 * @param maxScaleTimes Float 最大缩放倍数
 * @param updateUserScrollEnabled 用于和 Pager 交互
 */
@Composable
fun ScalableAsyncImage(
    modifier: Modifier,
    model: Any?,
    contentScale: ContentScale,
    contentDescription: String?,
    pageOffsetProvider: () -> Float,
    maxScaleTimes: Float = 4f,
    updateUserScrollEnabled: (Boolean) -> Unit,
) {
    var extraScale by remember { mutableStateOf(1f) }
    val animateScale = remember {
        Animatable(1f)
    }

    LaunchedEffect(extraScale){
        animateScale.animateTo(extraScale)
        updateUserScrollEnabled(extraScale == 1f)
    }

    var offset by remember { mutableStateOf(Offset.Zero) }

    AsyncImage(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .graphicsLayer {
                val pageOffset = pageOffsetProvider()
                if (pageOffset == 0f) {
                    scaleX = animateScale.value
                    scaleY = animateScale.value
                    translationX = offset.x
                    translationY = offset.y
                } else if (pageOffset < 1f) { // 正在切图
                    lerp(
                        start = 0.85f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    ).also { scale ->
                        scaleX = scale * extraScale
                        scaleY = scale * extraScale
                    }
                    alpha = lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                } else { // 切走了
                    extraScale = 1.0f
                    offset = Offset.Zero
                }

            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        extraScale =
                            if (extraScale <= 1f) (extraScale * 2).coerceAtMost(maxScaleTimes) else 1f
                        offset = Offset.Zero
                    }
                )
            },
        model = model,
        contentScale = contentScale,
        contentDescription = contentDescription
    )
}