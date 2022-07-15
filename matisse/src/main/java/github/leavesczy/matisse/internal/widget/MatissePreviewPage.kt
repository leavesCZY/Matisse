package github.leavesczy.matisse.internal.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import github.leavesczy.matisse.internal.model.MatissePreviewViewState
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
    systemUiController.setStatusBarColor(
        color = Color.Transparent,
        darkIcons = false,
    )
    systemUiController.setNavigationBarColor(
        color = Color.Transparent,
        darkIcons = false,
    )
    val matisse = viewState.matisse
    val initialPage = viewState.initialPage
    val previewResource = viewState.previewResource
    val selectedMediaResources = viewState.selectedMediaResources
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
    Scaffold(
        modifier = Modifier,
        backgroundColor = LocalMatisseTheme.current.onPreviewSurfaceColor
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
                count = previewResource.size,
                state = pagerState,
                key = { index ->
                    previewResource[index].key
                },
            ) { pageIndex ->
                val mediaResource = previewResource[pageIndex]
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(alignment = Alignment.Center)
                        .verticalScroll(state = rememberScrollState())
                        .graphicsLayer {
                            val pageOffset = calculateCurrentOffsetForPage(pageIndex).absoluteValue
                            lerp(
                                start = 0.85f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            ).also { scale ->
                                scaleX = scale
                                scaleY = scale
                            }
                            alpha = lerp(
                                start = 0.5f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            )
                        },
                    model = mediaResource.uri,
                    contentScale = ContentScale.FillWidth,
                    filterQuality = FilterQuality.None,
                    contentDescription = null
                )
            }
            val isSelected = selectedMediaResources.contains(previewResource[currentPageIndex])
            val enabled =
                isSelected || selectedMediaResources.size < matisse.maxSelectable
            MatisseCheckbox(
                modifier = Modifier
                    .align(alignment = Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 10.dp, end = 30.dp),
                theme = LocalMatisseTheme.current.checkBoxTheme,
                text = if (isSelected) {
                    (selectedMediaResources.indexOf(previewResource[currentPageIndex]) + 1).toString()
                } else {
                    ""
                },
                checked = isSelected,
                enabled = enabled,
                onCheckedChange = {
                    onMediaCheckChanged(previewResource[currentPageIndex])
                }
            )
        }
    }
}