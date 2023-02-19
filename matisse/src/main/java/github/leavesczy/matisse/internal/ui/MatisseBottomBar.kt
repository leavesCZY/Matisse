package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
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
internal fun MatisseBottomBar(viewState: MatisseBottomBarViewState) {
    Row(
        modifier = Modifier
            .shadow(elevation = 4.dp)
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(height = 56.dp)
            .background(color = colorResource(id = R.color.matisse_bottom_navigation_bar_background_color)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically)
                .then(
                    other = if (viewState.previewButtonClickable) {
                        Modifier.clickable(onClick = viewState.onClickPreviewButton)
                    } else {
                        Modifier
                    }
                )
                .fillMaxHeight()
                .padding(horizontal = 22.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            style = TextStyle(
                color = if (viewState.previewButtonClickable) {
                    colorResource(id = R.color.matisse_preview_button_text_color)
                } else {
                    colorResource(id = R.color.matisse_preview_button_text_color_if_disable)
                },
                fontSize = 14.sp
            ),
            text = viewState.previewText
        )
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically)
                .weight(weight = 1f, fill = false)
                .padding(end = 22.dp)
                .clip(shape = RoundedCornerShape(size = 22.dp))
                .background(
                    color = if (viewState.sureButtonClickable) {
                        colorResource(id = R.color.matisse_sure_button_background_color)
                    } else {
                        colorResource(id = R.color.matisse_sure_button_background_color_if_disable)
                    }
                )
                .then(
                    other = if (viewState.sureButtonClickable) {
                        Modifier.clickable(onClick = viewState.onClickSureButton)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 22.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = TextStyle(
                color = if (viewState.sureButtonClickable) {
                    colorResource(id = R.color.matisse_sure_button_text_color)
                } else {
                    colorResource(id = R.color.matisse_sure_button_text_color_if_disable)
                }, fontSize = 14.sp
            ),
            text = viewState.sureText
        )
    }
}