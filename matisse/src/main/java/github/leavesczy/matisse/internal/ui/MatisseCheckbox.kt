package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
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

/** 相对媒体格子边长，右上角容器区域比例 */
internal const val GridMatisseCheckboxContainerFraction = 0.28f

/** 容器内 checkbox 占比，居中后形成动态边距（0.28 × 0.80 = 0.224） */
internal const val GridMatisseCheckboxInnerFraction = 0.80f

@Composable
internal fun MatisseCheckbox(
    modifier: Modifier,
    selectionState: State<MatisseMediaSelectState>,
    onCheckedChange: () -> Unit
) {
    val state = selectionState.value
    Box(modifier = modifier) {
        CheckboxCircle(
            modifier = Modifier
                .fillMaxSize(),
            isSelected = state.isSelected,
            isEnabled = state.isEnabled,
            onCheckedChange = onCheckedChange
        )
        val positionFormatted = state.positionFormatted
        if (!positionFormatted.isNullOrBlank()) {
            CheckboxPositionText(
                modifier = Modifier
                    .align(alignment = Alignment.Center),
                text = positionFormatted,
                color = colorResource(id = R.color.matisse_checkbox_text_color)
            )
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
                val radius = size.minDimension / 2f
                val center = Offset(x = size.width / 2f, y = size.height / 2f)
                if (isSelected) {
                    drawCircle(
                        color = selectedFillColor,
                        radius = radius,
                        center = center
                    )
                } else {
                    drawCircle(
                        color = unselectedFillColor,
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = strokeColor,
                        radius = radius - strokeWidth.toPx() / 2f,
                        center = center,
                        style = Stroke(width = strokeWidth.toPx())
                    )
                }
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
            maxFontSize = 36.sp,
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