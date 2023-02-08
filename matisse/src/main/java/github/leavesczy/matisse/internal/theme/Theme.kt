package github.leavesczy.matisse.internal.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val colors = lightColorScheme()

private val Typography = Typography()

private val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

@Composable
internal fun MatisseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors, typography = Typography, shapes = Shapes, content = content
    )
}