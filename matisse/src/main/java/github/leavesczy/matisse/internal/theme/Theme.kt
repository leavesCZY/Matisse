package github.leavesczy.matisse.internal.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val colors = lightColors()

private val Typography = Typography()

private val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(0.dp)
)

@Composable
internal fun MatisseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = colors, typography = Typography, shapes = Shapes, content = content
    )
}