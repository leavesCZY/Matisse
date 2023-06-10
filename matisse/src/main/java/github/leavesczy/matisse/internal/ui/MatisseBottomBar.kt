package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseBottomBarViewState

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 19:19
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
@Composable
internal fun MatisseBottomBar(
    bottomBarViewState: MatisseBottomBarViewState,
    onSure: () -> Unit
) {
    Box(
        modifier = Modifier
            .shadow(elevation = 4.dp)
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(height = 56.dp)
            .background(color = colorResource(id = R.color.matisse_bottom_navigation_bar_background_color))
    ) {
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterStart)
                .then(
                    other = if (bottomBarViewState.previewButtonClickable) {
                        Modifier.clickable(onClick = bottomBarViewState.onClickPreviewButton)
                    } else {
                        Modifier
                    }
                )
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
                .wrapContentSize(align = Alignment.Center),
            text = bottomBarViewState.previewButtonText,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = if (bottomBarViewState.previewButtonClickable) {
                colorResource(id = R.color.matisse_preview_text_color)
            } else {
                colorResource(id = R.color.matisse_preview_text_color_if_disable)
            }
        )
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .then(
                    other = if (bottomBarViewState.sureButtonClickable) {
                        Modifier.clickable(onClick = onSure)
                    } else {
                        Modifier
                    }
                )
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
                .wrapContentSize(align = Alignment.Center),
            text = bottomBarViewState.sureButtonText,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = colorResource(
                id = if (bottomBarViewState.sureButtonClickable) {
                    R.color.matisse_sure_text_color
                } else {
                    R.color.matisse_sure_text_color_if_disable
                }
            )
        )
    }
}