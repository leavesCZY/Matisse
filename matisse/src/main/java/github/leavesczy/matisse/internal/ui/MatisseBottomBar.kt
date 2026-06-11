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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseBottomBarViewState

@Composable
internal fun MatisseBottomBar(
    modifier: Modifier,
    bottomBarViewState: MatisseBottomBarViewState,
    onConfirmClick: () -> Unit
) {
    Row(
        modifier = modifier
            .shadow(elevation = 4.dp)
            .background(color = colorResource(id = R.color.matisse_navigation_bar_background_color))
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(height = 56.dp)
            .background(color = colorResource(id = R.color.matisse_bottom_bar_background_color)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .then(
                    other = if (bottomBarViewState.isPreviewEnabled) {
                        Modifier
                            .clip(shape = CircleShape)
                            .clickable(onClick = bottomBarViewState.onPreviewClick)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 22.dp, vertical = 6.dp),
            text = stringResource(id = R.string.matisse_action_preview),
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal,
            color = if (bottomBarViewState.isPreviewEnabled) {
                colorResource(id = R.color.matisse_bottom_bar_preview_text_color)
            } else {
                colorResource(id = R.color.matisse_bottom_bar_preview_text_disabled_color)
            }
        )
        val selectedMediaCount = bottomBarViewState.selectedMediaCount
        val maxSelectable = bottomBarViewState.maxSelectable
        val isConfirmEnabled = selectedMediaCount in 1..maxSelectable
        Text(
            modifier = Modifier
                .then(
                    other = if (isConfirmEnabled) {
                        Modifier
                            .clip(shape = CircleShape)
                            .clickable(onClick = onConfirmClick)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 22.dp, vertical = 6.dp),
            text = if (maxSelectable > 1) {
                stringResource(
                    id = R.string.matisse_action_confirm_with_count,
                    selectedMediaCount,
                    maxSelectable
                )
            } else {
                stringResource(id = R.string.matisse_action_confirm)
            },
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal,
            color = colorResource(
                id = if (isConfirmEnabled) {
                    R.color.matisse_bottom_bar_confirm_text_color
                } else {
                    R.color.matisse_bottom_bar_confirm_text_disabled_color
                }
            )
        )
    }
}