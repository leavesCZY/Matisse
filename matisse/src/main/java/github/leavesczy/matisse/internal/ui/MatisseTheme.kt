package github.leavesczy.matisse.internal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * @Author: CZY
 * @Date: 2025/5/7 11:53
 * @Desc:
 */
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