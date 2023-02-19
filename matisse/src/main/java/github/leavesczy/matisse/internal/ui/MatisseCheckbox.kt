package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.R

/**
 * @Author: CZY
 * @Date: 2022/5/31 14:27
 * @Desc:
 */
private val CheckboxSize = 22.dp
private val StrokeWidth = 2.dp

@OptIn(ExperimentalTextApi::class)
@Composable
internal fun MatisseCheckbox(
    modifier: Modifier,
    size: Dp = CheckboxSize,
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
    var textLayoutResult by remember {
        mutableStateOf<TextLayoutResult?>(value = null)
    }
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
                    radius = 20.dp
                )
            )
            .wrapContentSize(Alignment.Center)
            .requiredSize(size = size)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints = constraints)
                textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(text = text),
                    style = TextStyle(
                        color = textColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                )
                layout(width = placeable.width, height = placeable.height) {
                    placeable.placeRelative(x = 0, y = 0)
                }
            }
    ) {
        val width = this.size.width
        val height = this.size.height
        val checkBoxSide = minOf(a = width, b = height)
        val checkBoxRadius = checkBoxSide / 2f
        val strokeWidth = StrokeWidth.toPx()
        drawCircle(
            color = circleColor,
            radius = checkBoxRadius - strokeWidth / 2f,
            style = Stroke(width = strokeWidth)
        )
        if (checked) {
            drawCircle(color = fillColor, radius = checkBoxRadius - strokeWidth)
        }
        val mTextLayoutResult = textLayoutResult
        if (mTextLayoutResult != null && text.isNotBlank()) {
            val textLayoutResultSize = mTextLayoutResult.size
            drawText(
                textLayoutResult = mTextLayoutResult, topLeft = Offset(
                    x = (width - textLayoutResultSize.width) / 2,
                    y = (height - textLayoutResultSize.height) / 2
                )
            )
        }
    }
}