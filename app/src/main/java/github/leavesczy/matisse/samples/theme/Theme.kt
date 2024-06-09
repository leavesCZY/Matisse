package github.leavesczy.matisse.samples.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val lightColorScheme = lightColorScheme(
    primary = Color(0xFF42A5F5),
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260)
)

private val darkColorScheme = darkColorScheme(
    primary = Color(0xFF09A293),
    onPrimary = Color.White,
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

@Composable
fun MatisseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme
    } else {
        lightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}