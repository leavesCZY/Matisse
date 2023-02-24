package github.leavesczy.matisse.internal.ui

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import github.leavesczy.matisse.R

/**
 * @Author: CZY
 * @Date: 2022/5/31 14:27
 * @Desc:
 */
private val CheckboxSize = 22.dp
private val StrokeWidth = 2.dp

@Composable
internal fun MatisseCheckbox(
    modifier: Modifier,
    size: Dp = CheckboxSize,
    text: String,
    enabled: Boolean,
    checked: Boolean,
    onClick: () -> Unit
) {
    val localDensity = LocalDensity.current
    val context = LocalContext.current
    val textPaint = remember {
        Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            isDither = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = android.graphics.Paint.Align.CENTER
            with(localDensity) {
                textSize = 14.sp.toPx()
                color = ContextCompat.getColor(context, R.color.matisse_check_box_text_color)
            }
        }
    }
    val circleColor = colorResource(
        id = if (enabled) {
            R.color.matisse_check_box_circle_color
        } else {
            R.color.matisse_check_box_circle_color_if_disable
        }
    )
    val fillColor = colorResource(id = R.color.matisse_check_box_fill_color)
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
                    radius = size / 2
                )
            )
            .wrapContentSize(Alignment.Center)
            .requiredSize(size = size)
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
        if (text.isNotBlank()) {
            drawTextToCenter(
                text = text,
                textPaint = textPaint
            )
        }
    }
}

private fun DrawScope.drawTextToCenter(text: String, textPaint: android.graphics.Paint) {
    val fontMetrics = textPaint.fontMetrics
    val x = size.width / 2f
    val y = size.height / 2f - (fontMetrics.top + fontMetrics.bottom) / 2f
    drawContext.canvas.nativeCanvas.drawText(text, x, y, textPaint)
}