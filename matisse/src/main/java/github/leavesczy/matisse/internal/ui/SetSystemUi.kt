package github.leavesczy.matisse.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import github.leavesczy.matisse.internal.theme.LocalMatisseTheme

/**
 * @Author: CZY
 * @Date: 2022/8/19 15:25
 * @Desc:
 */
@Composable
internal fun SetSystemUi(previewPageVisible: Boolean) {
    val systemBarsTheme = LocalMatisseTheme.current.systemBarsTheme
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = Color.Transparent,
        darkIcons = if (previewPageVisible) {
            false
        } else {
            systemBarsTheme.statusBarDarkIcons
        }
    )
    systemUiController.setNavigationBarColor(
        color = Color.Transparent,
        darkIcons = if (previewPageVisible) {
            false
        } else {
            systemBarsTheme.navigationBarDarkIcons
        }
    )
}