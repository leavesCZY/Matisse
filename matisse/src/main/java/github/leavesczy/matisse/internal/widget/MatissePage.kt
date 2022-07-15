package github.leavesczy.matisse.internal.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import github.leavesczy.matisse.MediaResources
import github.leavesczy.matisse.internal.model.MatissePageAction
import github.leavesczy.matisse.internal.model.MatisseViewState
import github.leavesczy.matisse.internal.theme.LocalMatisseTheme

/**
 * @Author: CZY
 * @Date: 2022/5/31 16:36
 * @Desc:
 */
@Composable
internal fun MatissePage(
    matisseViewState: MatisseViewState,
    pageAction: MatissePageAction,
    lazyGridState: LazyGridState
) {
    val systemBarsTheme = LocalMatisseTheme.current.systemBarsTheme
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = systemBarsTheme.statusBarColor,
        darkIcons = systemBarsTheme.statusBarDarkIcons,
    )
    systemUiController.setNavigationBarColor(
        color = systemBarsTheme.navigationBarColor,
        darkIcons = systemBarsTheme.navigationBarDarkIcons,
    )
    val displayResources = matisseViewState.selectedBucket.displayResources
    val selectedMediaResources = matisseViewState.selectedMediaResources
    val matisse = matisseViewState.matisse
    val keyBuilder: (index: Int) -> Any =
        remember(
            key1 = matisseViewState.selectedBucket.bucketId +
                    matisseViewState.selectedBucket.displayResources.size
        ) {
            {
                displayResources[it].key
            }
        }
    val contentTypeBuilder: (index: Int) -> Any? =
        remember(
            key1 = matisseViewState.selectedBucket.bucketId +
                    matisseViewState.selectedBucket.displayResources.size
        ) {
            {
                val resources = displayResources[it]
                if (pageAction.isCaptureMediaResources(resources)) {
                    "capture"
                } else {
                    "album"
                }
            }
        }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        backgroundColor = LocalMatisseTheme.current.surfaceColor,
        topBar = {
            MatisseTopAppBar(
                allBucket = matisseViewState.allBucket,
                selectedBucket = matisseViewState.selectedBucket,
                onClickBackMenu = pageAction.onClickBackMenu,
                onSelectBucket = pageAction.onSelectBucket,
            )
        },
        bottomBar = {
            MatisseBottomNavigation(
                previewText = matisseViewState.previewText,
                previewBtnClickable = matisseViewState.sureBtnClickable,
                onPreview = pageAction.onPreviewSelectedResources,
                sureText = matisseViewState.sureText,
                sureBtnClickable = matisseViewState.sureBtnClickable,
                onSure = pageAction.onSure,
            )
        }
    ) { contentPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(count = matisse.spanCount),
            state = lazyGridState,
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(LayoutDirection.Ltr),
                top = contentPadding.calculateTopPadding(),
                end = contentPadding.calculateEndPadding(LayoutDirection.Ltr),
                bottom = contentPadding.calculateBottomPadding() + 32.dp
            )
        ) {
            items(
                key = keyBuilder,
                contentType = contentTypeBuilder,
                count = displayResources.size,
                itemContent = { itemIndex ->
                    val resources = displayResources[itemIndex]
                    if (pageAction.isCaptureMediaResources(resources)) {
                        CaptureItem(onClick = pageAction.onCapture)
                    } else {
                        val isSelected = selectedMediaResources.contains(resources)
                        val enabled =
                            isSelected || selectedMediaResources.size < matisse.maxSelectable
                        AlbumItem(
                            mediaResources = resources,
                            isSelected = isSelected,
                            enabled = enabled,
                            position = selectedMediaResources.indexOf(resources) + 1,
                            onClickMedia = pageAction.onClickMedia,
                            onMediaCheckChanged = pageAction.onMediaCheckChanged
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun LazyGridItemScope.AlbumItem(
    mediaResources: MediaResources,
    isSelected: Boolean,
    enabled: Boolean,
    position: Int,
    onClickMedia: (MediaResources) -> Unit,
    onMediaCheckChanged: (MediaResources) -> Unit
) {
    Box(
        modifier = Modifier
            .animateItemPlacement()
            .aspectRatio(ratio = 1f)
            .padding(top = 1.dp, start = 1.dp, end = 1.dp, bottom = 1.dp)
            .clip(shape = RoundedCornerShape(size = 4f))
            .background(color = LocalMatisseTheme.current.imageBackgroundColor)
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    other = if (isSelected) {
                        Modifier.drawFrame(color = LocalMatisseTheme.current.checkBoxTheme.frameColor)
                    } else {
                        Modifier
                    }
                )
                .clickable {
                    onClickMedia(mediaResources)
                },
            model = mediaResources.uri,
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
        MatisseCheckbox(
            modifier = Modifier
                .align(alignment = Alignment.TopEnd),
            theme = LocalMatisseTheme.current.checkBoxTheme,
            text = if (position > 0) {
                position.toString()
            } else {
                ""
            },
            checked = isSelected,
            enabled = enabled,
            onCheckedChange = {
                onMediaCheckChanged(mediaResources)
            }
        )
    }
}

@Composable
private fun LazyGridItemScope.CaptureItem(
    onClick: () -> Unit
) {
    val captureIconTheme = LocalMatisseTheme.current.captureIconTheme
    Box(
        modifier = Modifier
            .animateItemPlacement()
            .aspectRatio(ratio = 1f)
            .padding(top = 1.dp, start = 1.dp, end = 1.dp, bottom = 1.dp)
            .clip(shape = RoundedCornerShape(size = 4f))
            .background(color = captureIconTheme.backgroundColor)
            .clickable {
                onClick()
            }
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(fraction = 0.5f)
                .align(alignment = Alignment.Center),
            imageVector = captureIconTheme.icon,
            tint = captureIconTheme.tint,
            contentDescription = null,
        )
    }
}

private fun Modifier.drawFrame(color: Color): Modifier {
    return drawWithContent {
        drawContent()
        drawRect(
            color = color,
            style = Stroke(width = 22f)
        )
    }
}