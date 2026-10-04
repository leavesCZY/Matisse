package github.leavesczy.matisse.internal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** 选择器的 [MaterialTheme]。页面颜色来自 XML DayNight 资源。 */
@Composable
internal fun MatisseTheme(content: @Composable () -> Unit) {
    val lightColorScheme = remember {
        lightColorScheme()
    }
    MaterialTheme(
        colorScheme = lightColorScheme,
        content = content
    )
}