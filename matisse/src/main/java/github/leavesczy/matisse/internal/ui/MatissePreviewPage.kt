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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import github.leavesczy.matisse.internal.logic.MatisseMediaItem
import github.leavesczy.matisse.internal.logic.MatissePreviewPageViewState
import github.leavesczy.matisse.internal.logic.MatissePreviewSource
import kotlinx.coroutines.flow.first
import kotlin.math.absoluteValue

private const val PREVIEW_PAGE_TRANSITION_MILLIS = 350

@Composable
internal fun MatissePreviewPage(
    pageViewState: MatissePreviewPageViewState,
    galleryItems: MatisseGalleryItems,
    imageEngine: ImageEngine,
    onConfirmClick: (currentItem: MatisseMediaItem?) -> Unit
) {
    AnimatedVisibility(
        modifier = Modifier
            .fillMaxSize(),
        visible = pageViewState.isVisible,
        enter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = PREVIEW_PAGE_TRANSITION_MILLIS,
                easing = FastOutSlowInEasing
            ),
            initialOffsetX = { it }
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = PREVIEW_PAGE_TRANSITION_MILLIS,
                easing = FastOutSlowInEasing
            ),
            targetOffsetX = { it }
        )
    ) {
        // 退出动画结束后 content 才会 dispose；此时再通知 VM 重置状态，避免动画中清空导致闪烁
        DisposableEffect(key1 = Unit) {
            onDispose {
                pageViewState.onExitFinished()
            }
        }
        MatissePreviewPageContent(
            pageViewState = pageViewState,
            galleryItems = galleryItems,
            imageEngine = imageEngine,
            onConfirmClick = onConfirmClick
        )
    }
}

@Composable
private fun MatissePreviewPageContent(
    pageViewState: MatissePreviewPageViewState,
    galleryItems: MatisseGalleryItems,
    imageEngine: ImageEngine,
    onConfirmClick: (currentItem: MatisseMediaItem?) -> Unit
) {
    // 退出动画期间继续拦截返回键（dismiss 对已隐藏状态是空操作），避免连按返回直接关闭选择器
    BackHandler(onBack = pageViewState.onDismissRequest)
    val source = pageViewState.source
    key(pageViewState.previewPagerKey) {
        val initialPage = pageViewState.initialPage.coerceIn(
            minimumValue = 0,
            maximumValue = (source.itemCount(galleryItems = galleryItems) - 1).coerceAtLeast(
                minimumValue = 0
            )
        )
        val pagerState = rememberPagerState(initialPage = initialPage) {
            source.itemCount(galleryItems = galleryItems).coerceAtLeast(minimumValue = 1)
        }
        LaunchedEffect(key1 = pagerState, key2 = source, key3 = galleryItems) {
            // 在组合之外向 Paging 上报当前页，滑近末尾时自动加载下一页，列表页同步可见
            snapshotFlow {
                pagerState.currentPage
            }.collect { page ->
                source.reportAccess(galleryItems = galleryItems, index = page)
            }
        }
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(key1 = Unit) {
                    // 只用于拦截落到下层列表页的触摸；不使用 clickable，避免整页被合并为一个可点击的无障碍节点
                },
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
                        source.pageKey(galleryItems = galleryItems, index = index)
                    }
                ) { pageIndex ->
                    // 组合期间只能 peek：若在组合中触发 Paging 加载，数据变化会打断该页组合，图片请求取消后画面空白
                    val mediaItem = source.peekItem(
                        galleryItems = galleryItems,
                        index = pageIndex
                    ) ?: return@HorizontalPager
                    PreviewMediaPage(
                        modifier = Modifier
                            .fillMaxSize(),
                        isPreviewVisible = pageViewState.isVisible,
                        pagerState = pagerState,
                        pageIndex = pageIndex,
                        imageEngine = imageEngine,
                        mediaResource = mediaItem.mediaResource
                    )
                }
                PreviewBottomBar(
                    modifier = Modifier
                        .fillMaxWidth(),
                    pageViewState = pageViewState,
                    galleryItems = galleryItems,
                    pagerState = pagerState,
                    onConfirmClick = onConfirmClick
                )
            }
        }
    }
}

@Composable
private fun PreviewMediaPage(
    modifier: Modifier,
    isPreviewVisible: Boolean,
    pagerState: PagerState,
    pageIndex: Int,
    imageEngine: ImageEngine,
    mediaResource: MediaResource
) {
    // HorizontalPager 按 key 复用页面组合；pageIndex 变化时必须重置，避免沿用旧下标的缩放与播放态
    var isVideoPlaying by remember(key1 = pageIndex) {
        mutableStateOf(value = false)
    }
    if (isVideoPlaying) {
        LaunchedEffect(key1 = pageIndex) {
            // 翻页停稳到其它页后结束播放，回到封面状态
            snapshotFlow {
                !pagerState.isScrollInProgress && pagerState.settledPage != pageIndex
            }.first { it }
            isVideoPlaying = false
        }
    }
    val fraction by remember(key1 = pageIndex) {
        derivedStateOf {
            val pageOffset =
                (pagerState.currentPage - pageIndex + pagerState.currentPageOffsetFraction).absoluteValue
            val progress = 1f - pageOffset.coerceIn(minimumValue = 0f, maximumValue = 1f)
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
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = fraction
                    scaleY = fraction
                    alpha = fraction
                },
            contentAlignment = Alignment.Center
        ) {
            imageEngine.Preview(mediaResource = mediaResource)
            if (mediaResource.isVideo) {
                // 预览页一开始退出就释放播放器，避免退出动画期间继续出声
                if (isVideoPlaying && isPreviewVisible) {
                    MatissePreviewVideoPlayer(
                        modifier = Modifier
                            .fillMaxSize(),
                        videoUri = mediaResource.uri,
                        onPlaybackEnded = {
                            isVideoPlaying = false
                        }
                    )
                } else {
                    MatisseVideoIcon(
                        modifier = Modifier
                            .clip(shape = CircleShape)
                            .clickable {
                                isVideoPlaying = true
                            }
                            .padding(all = 10.dp)
                            .size(size = 50.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewBottomBar(
    modifier: Modifier,
    pageViewState: MatissePreviewPageViewState,
    galleryItems: MatisseGalleryItems,
    pagerState: PagerState,
    onConfirmClick: (currentItem: MatisseMediaItem?) -> Unit
) {
    val source = pageViewState.source
    val currentResource by remember(key1 = source, key2 = galleryItems) {
        derivedStateOf {
            source.peekItem(galleryItems = galleryItems, index = pagerState.currentPage)
        }
    }
    val mediaItem = currentResource
    Box(
        modifier = modifier
            .background(color = colorResource(id = R.color.matisse_preview_page_bottom_bar_background_color))
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(height = 56.dp)
    ) {
        if (mediaItem != null) {
            val onCheckedChange = remember(
                key1 = mediaItem.mediaId,
                key2 = pageViewState.onToggleMediaSelection
            ) {
                {
                    pageViewState.onToggleMediaSelection(mediaItem)
                }
            }
            MatisseCheckbox(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .size(size = 25.dp),
                selectionState = pageViewState.selectionStateFor(mediaItem.mediaId),
                isSelectionLimitReached = pageViewState.isSelectionLimitReached,
                maxSelectable = pageViewState.maxSelectable,
                onCheckedChange = onCheckedChange
            )
        }
        val selectedMediaCount = pageViewState.selectedMediaCount
        val maxSelectable = pageViewState.maxSelectable
        // 单选确定即可带上当前页，不必先勾选
        val isConfirmEnabled = if (maxSelectable == 1) {
            mediaItem != null
        } else {
            selectedMediaCount in 1..maxSelectable
        }
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .then(
                    other = if (isConfirmEnabled) {
                        Modifier
                            .clip(shape = CircleShape)
                            .clickable {
                                onConfirmClick(mediaItem)
                            }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 22.dp, vertical = 6.dp),
            text = if (maxSelectable > 1) {
                stringResource(
                    id = R.string.matisse_action_confirm_with_count,
                    formatArgs = arrayOf(selectedMediaCount, maxSelectable)
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

private fun MatissePreviewSource.itemCount(galleryItems: MatisseGalleryItems): Int {
    return when (this) {
        MatissePreviewSource.Gallery -> galleryItems.itemCount
        is MatissePreviewSource.Selected -> mediaItems.size
    }
}

private fun MatissePreviewSource.reportAccess(
    galleryItems: MatisseGalleryItems,
    index: Int
) {
    when (this) {
        MatissePreviewSource.Gallery -> galleryItems[index]
        is MatissePreviewSource.Selected -> Unit
    }
}

private fun MatissePreviewSource.pageKey(
    galleryItems: MatisseGalleryItems,
    index: Int
): Any {
    return when (this) {
        MatissePreviewSource.Gallery -> galleryItems.keyAt(index = index)
        is MatissePreviewSource.Selected -> mediaItems.getOrNull(index = index)?.let {
            "selected_${it.mediaId}"
        } ?: "placeholder_$index"
    }
}

private fun MatissePreviewSource.peekItem(
    galleryItems: MatisseGalleryItems,
    index: Int
): MatisseMediaItem? {
    return when (this) {
        MatissePreviewSource.Gallery -> galleryItems.peek(index = index)
        is MatissePreviewSource.Selected -> mediaItems.getOrNull(index = index)
    }
}