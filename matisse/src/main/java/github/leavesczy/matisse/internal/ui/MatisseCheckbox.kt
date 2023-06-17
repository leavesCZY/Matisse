package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.R

/**
 * @Author: CZY
 * @Date: 2022/5/31 14:27
 * @Desc:
 */
private val CheckboxSize = 24.dp

@OptIn(ExperimentalTextApi::class)
@Composable
internal fun MatisseCheckbox(
    modifier: Modifier,
    text: String,
    enabled: Boolean,
    checked: Boolean,
    onClick: () -> Unit
) {
    val circleColor = colorResource(
        id = if (enabled) {
            R.color.matisse_check_box_circle_color
        } else {
            R.color.matisse_check_box_circle_color_if_disable
        }
    )
    val fillColor = colorResource(id = R.color.matisse_check_box_fill_color)
    val textColor = colorResource(id = R.color.matisse_check_box_text_color)
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier
            .selectable(
                selected = checked,
                onClick = onClick,
                enabled = true,
                role = Role.Checkbox,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(
                    bounded = false,
                    radius = CheckboxSize / 2
                )
            )
            .requiredSize(size = CheckboxSize)
    ) {
        val checkboxSide = this.size.width
        val checkboxRadius = checkboxSide / 2f
        val strokeWidth = checkboxSide / 10f
        drawCircle(
            color = circleColor,
            radius = checkboxRadius - strokeWidth / 2f,
            style = Stroke(width = strokeWidth)
        )
        if (checked) {
            drawCircle(
                color = fillColor,
                radius = checkboxRadius - strokeWidth
            )
        }
        if (text.isNotBlank()) {
            val textLayoutResult = textMeasurer.measure(
                text = text,
                style = TextStyle(
                    color = textColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = (checkboxSide - textLayoutResult.size.width) / 2,
                    y = (checkboxSide - textLayoutResult.size.height) / 2
                )
            )
        }
    }
}