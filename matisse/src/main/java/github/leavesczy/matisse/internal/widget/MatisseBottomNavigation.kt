package github.leavesczy.matisse.internal.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.internal.theme.LocalMatisseTheme

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 19:19
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
@Composable
internal fun MatisseBottomNavigation(
    previewText: String,
    previewBtnClickable: Boolean,
    sureText: String,
    sureBtnClickable: Boolean,
    onPreview: () -> Unit,
    onSure: () -> Unit
) {
    val bottomNavigationTheme = LocalMatisseTheme.current.bottomNavigationTheme
    val previewButtonTheme = LocalMatisseTheme.current.previewButtonTheme
    val sureButtonTheme = LocalMatisseTheme.current.sureButtonTheme
    val alphaIfDisable = LocalMatisseTheme.current.alphaIfDisable
    BottomNavigation(backgroundColor = bottomNavigationTheme.backgroundColor) {
        if (previewText.isNotBlank()) {
            val previewTextStyle = previewButtonTheme.textStyle.let {
                if (previewBtnClickable) {
                    it
                } else {
                    it.copy(color = it.color.copy(alpha = alphaIfDisable))
                }
            }
            Text(
                modifier = Modifier
                    .clickable {
                        onPreview()
                    }
                    .fillMaxHeight()
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .padding(start = 20.dp, end = 20.dp),
                textAlign = TextAlign.Center,
                style = previewTextStyle,
                text = previewText,
            )
        }
        val sureButtonColor = sureButtonTheme.backgroundColor.let {
            if (sureBtnClickable) {
                it
            } else {
                it.copy(alpha = alphaIfDisable)
            }
        }
        val sureButtonTextStyle = sureButtonTheme.textStyle.let {
            if (sureBtnClickable) {
                it
            } else {
                it.copy(
                    it.color.copy(alpha = alphaIfDisable)
                )
            }
        }
        Text(
            modifier = Modifier
                .weight(weight = 1f, fill = true)
                .align(alignment = Alignment.CenterVertically)
                .wrapContentWidth(align = Alignment.End)
                .padding(end = 20.dp)
                .clip(shape = RoundedCornerShape(percent = 64))
                .then(other = Modifier
                    .background(color = sureButtonColor)
                    .let {
                        if (sureBtnClickable) {
                            it.clickable {
                                onSure()
                            }
                        } else {
                            it
                        }
                    }
                )
                .padding(horizontal = 20.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = sureButtonTextStyle,
            text = sureText,
        )
    }
}