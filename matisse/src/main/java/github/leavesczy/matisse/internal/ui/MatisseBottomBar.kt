package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import github.leavesczy.matisse.internal.theme.LocalMatisseTheme

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 19:19
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
@Composable
internal fun MatisseBottomBar(viewModel: MatisseViewModel, onSureButtonClick: () -> Unit) {
    val viewState = viewModel.bottomBarViewState
    val bottomNavigationTheme = LocalMatisseTheme.current.bottomNavigationTheme
    val previewButtonTheme = LocalMatisseTheme.current.previewButtonTheme
    val sureButtonTheme = LocalMatisseTheme.current.sureButtonTheme
    val alphaIfDisable = LocalMatisseTheme.current.alphaIfDisable
    BottomNavigation(
        backgroundColor = bottomNavigationTheme.backgroundColor,
        elevation = 16.dp
    ) {
        if (viewState.previewText.isNotBlank()) {
            val previewTextStyle = previewButtonTheme.textStyle.let {
                if (viewState.previewButtonClickable) {
                    it
                } else {
                    it.copy(color = it.color.copy(alpha = alphaIfDisable))
                }
            }
            Text(
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .then(other = if (viewState.previewButtonClickable) {
                        Modifier.clickable {
                            viewModel.onClickPreviewButton()
                        }
                    } else {
                        Modifier
                    })
                    .fillMaxHeight()
                    .padding(horizontal = 22.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically),
                textAlign = TextAlign.Center,
                style = previewTextStyle,
                text = viewState.previewText,
            )
        }
        val sureButtonColor = if (viewState.sureButtonClickable) {
            sureButtonTheme.backgroundColor
        } else {
            sureButtonTheme.backgroundColor.copy(alpha = alphaIfDisable)
        }
        val sureButtonTextStyle = sureButtonTheme.textStyle.let {
            if (viewState.sureButtonClickable) {
                it
            } else {
                it.copy(it.color.copy(alpha = alphaIfDisable))
            }
        }
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically)
                .weight(weight = 1f, fill = false)
                .padding(end = 22.dp)
                .clip(shape = RoundedCornerShape(size = 22.dp))
                .background(color = sureButtonColor)
                .then(other = if (viewState.sureButtonClickable) {
                    Modifier.clickable {
                        onSureButtonClick()
                    }
                } else {
                    Modifier
                })
                .padding(horizontal = 22.dp, vertical = 8.dp),
            text = viewState.sureText,
            textAlign = TextAlign.Center,
            style = sureButtonTextStyle
        )
    }
}