package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseBottomBarViewState

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 19:19
 * @Desc:
 */
@Composable
internal fun MatisseBottomBar(
    modifier: Modifier,
    viewState: MatisseBottomBarViewState,
    onClickSure: () -> Unit
) {
    Row(
        modifier = modifier
            .shadow(elevation = 4.dp)
            .background(color = colorResource(id = R.color.matisse_navigation_bar_color))
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(height = 56.dp)
            .background(color = colorResource(id = R.color.matisse_bottom_navigation_bar_background_color)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .then(
                    other = if (viewState.previewButtonClickable) {
                        Modifier
                            .clip(shape = CircleShape)
                            .clickable(onClick = viewState.onClickPreviewButton)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 20.dp, vertical = 6.dp),
            text = viewState.previewButtonText,
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal,
            color = if (viewState.previewButtonClickable) {
                colorResource(id = R.color.matisse_preview_text_color)
            } else {
                colorResource(id = R.color.matisse_preview_text_color_if_disable)
            }
        )
        Text(
            modifier = Modifier
                .then(
                    other = if (viewState.sureButtonClickable) {
                        Modifier
                            .clip(shape = CircleShape)
                            .clickable(onClick = onClickSure)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 20.dp, vertical = 6.dp),
            text = viewState.sureButtonText,
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal,
            color = colorResource(
                id = if (viewState.sureButtonClickable) {
                    R.color.matisse_sure_text_color
                } else {
                    R.color.matisse_sure_text_color_if_disable
                }
            )
        )
    }
}