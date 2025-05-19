package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseMediaSelectState

/**
 * @Author: leavesCZY
 * @Date: 2022/5/31 14:27
 * @Desc:
 */
@Composable
internal fun MatisseCheckbox(
    modifier: Modifier,
    selectState: MatisseMediaSelectState,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickableNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = CircleShape)
                .then(
                    other = if (selectState.isSelected) {
                        Modifier
                            .background(color = colorResource(id = R.color.matisse_check_box_circle_fill_color))
                    } else {
                        Modifier
                            .background(color = Color(0x1A000000))
                            .border(
                                width = 1.dp,
                                shape = CircleShape,
                                color = colorResource(
                                    id = if (selectState.isEnabled) {
                                        R.color.matisse_check_box_circle_color
                                    } else {
                                        R.color.matisse_check_box_circle_color_if_disable
                                    }
                                ),
                            )
                    }
                )
        )
        val positionFormatted = selectState.positionFormatted
        if (!positionFormatted.isNullOrBlank()) {
            BasicText(
                modifier = Modifier
                    .matchParentSize()
                    .wrapContentSize(align = Alignment.Center),
                text = positionFormatted,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 6.sp,
                    maxFontSize = 18.sp,
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

/**
 * @Author: leavesCZY
 * @Date: 2022/5/31 14:27
 * @Desc:
 */
@Composable
internal fun MatisseMax1Checkbox(
    modifier: Modifier,
    enabled: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val circleColor = colorResource(
        id = if (enabled) {
            R.color.matisse_check_box_circle_color
        } else {
            R.color.matisse_check_box_circle_color_if_disable
        }
    )
    val checkboxSize = 24.dp
    Box(
        modifier = modifier
            .selectable(
                selected = checked,
                onClick = onClick,
                enabled = true,
                role = Role.Checkbox,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = false,
                    radius = checkboxSize / 2
                )
            )
            .wrapContentSize(align = Alignment.Center)
            .requiredSize(size = checkboxSize),
    ) {
        if (checked) {
            Image(
                painter = painterResource(R.mipmap.ic_item_select_max_1),
                modifier = Modifier
                    .wrapContentSize(align = Alignment.Center)
                    .requiredSize(size = checkboxSize),
                contentDescription = null
            )
        } else {
            Canvas(
                modifier = Modifier
                    .wrapContentSize(align = Alignment.Center)
                    .requiredSize(size = checkboxSize),
            ) {
                val checkboxSide = size.width
                val checkboxRadius = checkboxSide / 2f
                val strokeWidth = checkboxSide / 11f
                drawCircle(
                    color = circleColor,
                    radius = checkboxRadius - strokeWidth / 2f,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}