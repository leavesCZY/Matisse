package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.R

/**
 * @Author: leavesCZY
 * @Date: 2022/5/31 14:27
 * @Desc:
 */
@Composable
internal fun MatisseCheckbox(
    modifier: Modifier,
    text: String,
    enabled: Boolean,
    checked: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickableNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape = CircleShape)
                    .background(color = colorResource(id = R.color.matisse_check_box_circle_fill_color))
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape = CircleShape)
                    .then(
                        other = if (enabled) {
                            Modifier
                                .background(color = Color(0x26000000))
                        } else {
                            Modifier
                        }
                    )
                    .border(
                        width = 1.4.dp,
                        shape = CircleShape,
                        color = colorResource(
                            id = if (enabled) {
                                R.color.matisse_check_box_circle_color
                            } else {
                                R.color.matisse_check_box_circle_color_if_disable
                            }
                        ),
                    )
            )
        }
        if (text.isNotBlank()) {
            BasicText(
                modifier = Modifier,
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 8.sp,
                    maxFontSize = 16.sp,
                    stepSize = 1.sp
                ),
                style = TextStyle(
                    color = colorResource(id = R.color.matisse_check_box_text_color),
                    fontStyle = FontStyle.Normal,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}