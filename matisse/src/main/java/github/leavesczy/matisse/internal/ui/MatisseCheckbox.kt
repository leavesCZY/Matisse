package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseMediaSelectState

@Composable
internal fun MatisseCheckbox(
    modifier: Modifier,
    selectionState: MatisseMediaSelectState,
    isSelectionLimitReached: Boolean,
    maxSelectable: Int,
    onCheckedChange: () -> Unit
) {
    Box(modifier = modifier) {
        CheckboxCircle(
            modifier = Modifier
                .fillMaxSize(),
            isSelected = selectionState.isSelected,
            isEnabled = selectionState.isSelected || !isSelectionLimitReached,
            onCheckedChange = onCheckedChange
        )
        if (selectionState.isSelected) {
            if (maxSelectable > 1) {
                val positionFormatted = selectionState.positionFormatted
                if (!positionFormatted.isNullOrBlank()) {
                    CheckboxPositionText(
                        modifier = Modifier
                            .align(alignment = Alignment.Center),
                        text = positionFormatted,
                        color = colorResource(id = R.color.matisse_checkbox_text_color)
                    )
                }
            } else {
                Icon(
                    modifier = Modifier
                        .align(alignment = Alignment.Center)
                        .fillMaxSize(fraction = 0.78f),
                    painter = painterResource(id = R.drawable.ic_matisse_check),
                    tint = colorResource(id = R.color.matisse_checkbox_text_color),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun CheckboxCircle(
    modifier: Modifier,
    isSelected: Boolean,
    isEnabled: Boolean,
    onCheckedChange: () -> Unit
) {
    val selectedFillColor = colorResource(id = R.color.matisse_checkbox_circle_fill_selected_color)
    val unselectedFillColor =
        colorResource(id = R.color.matisse_checkbox_circle_fill_unselected_color)
    val strokeColor = colorResource(
        id = if (isEnabled) {
            R.color.matisse_checkbox_circle_stroke_color
        } else {
            R.color.matisse_checkbox_circle_stroke_disabled_color
        }
    )
    val strokeWidth = 1.8.dp
    Box(
        modifier = modifier
            .semantics {
                role = Role.Checkbox
            }
            .drawBehind {
                val strokeWidthPx = strokeWidth.toPx()
                val radius = size.minDimension / 2f
                val center = Offset(x = size.width / 2f, y = size.height / 2f)
                drawCircle(
                    color = if (isSelected) {
                        selectedFillColor
                    } else {
                        unselectedFillColor
                    },
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = strokeColor,
                    radius = radius - strokeWidthPx / 2f,
                    center = center,
                    style = Stroke(width = strokeWidthPx)
                )
            }
            .clickableNoRipple(onClick = onCheckedChange)
    )
}

@Composable
private fun CheckboxPositionText(
    modifier: Modifier,
    text: String,
    color: Color
) {
    BasicText(
        modifier = modifier,
        text = text,
        autoSize = TextAutoSize.StepBased(
            minFontSize = 4.sp,
            maxFontSize = 20.sp,
            stepSize = 0.4.sp
        ),
        style = TextStyle(
            color = color,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    )
}