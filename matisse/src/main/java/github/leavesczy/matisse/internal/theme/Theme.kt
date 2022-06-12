package github.leavesczy.matisse.internal.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.MatisseTheme

private val Typography = Typography()

private val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(0.dp)
)

internal val LocalMatisseTheme = staticCompositionLocalOf<MatisseTheme> {
    error("CompositionLocal LocalMatisseTheme not present")
}

@Composable
private fun ProvideMatisseTheme(
    matisseTheme: MatisseTheme,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalMatisseTheme provides matisseTheme) {
        content()
    }
}

@Composable
internal fun MatisseTheme(
    matisseTheme: MatisseTheme,
    content: @Composable () -> Unit
) {
    ProvideMatisseTheme(matisseTheme = matisseTheme) {
        MaterialTheme(
            colors = lightColors(),
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}