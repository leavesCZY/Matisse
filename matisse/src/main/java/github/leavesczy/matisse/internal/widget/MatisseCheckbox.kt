package github.leavesczy.matisse.internal.widget

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.CheckBoxTheme
import github.leavesczy.matisse.internal.theme.LocalMatisseTheme

/**
 * @Author: CZY
 * @Date: 2022/5/31 14:27
 * @Desc:
 */
private val defaultSize = 22.dp
private val defaultStokeWidth = 2.dp

@Composable
internal fun MatisseCheckbox(
    modifier: Modifier,
    theme: CheckBoxTheme,
    text: String,
    enabled: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val textPaint by remember {
        mutableStateOf(Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            isDither = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = android.graphics.Paint.Align.CENTER
        })
    }
    val alphaIfDisable = LocalMatisseTheme.current.alphaIfDisable
    Canvas(
        modifier = modifier
            .clickable {
                onCheckedChange(!checked)
            }
            .padding(all = 4.dp)
            .size(size = defaultSize)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val checkBoxSide = minOf(canvasWidth, canvasHeight)
        val stokeWidth = defaultStokeWidth.toPx()
        val outRadius = (checkBoxSide - stokeWidth) / 2f
        drawCircle(
            color = if (enabled) {
                theme.circleColor
            } else {
                theme.circleColor.copy(alpha = alphaIfDisable)
            },
            radius = outRadius,
            style = Stroke(width = stokeWidth)
        )
        if (checked) {
            drawCircle(
                color = theme.circleFillColor,
                radius = outRadius
            )
        }
        if (theme.countable && text.isNotBlank()) {
            drawTextToCenter(
                text = text,
                textPaint = textPaint,
                textSize = theme.fontSize.toPx(),
                textColor = theme.textColor.toArgb()
            )
        }
    }
}

private fun DrawScope.drawTextToCenter(
    text: String,
    textPaint: android.graphics.Paint,
    textSize: Float,
    textColor: Int
) {
    textPaint.textSize = textSize
    textPaint.color = textColor
    val fontMetrics = textPaint.fontMetrics
    val x = size.width / 2f
    val y = size.height / 2f - (fontMetrics.top + fontMetrics.bottom) / 2f
    drawContext.canvas.nativeCanvas.drawText(text, x, y, textPaint)
}