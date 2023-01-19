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
    val matisseTheme = LocalMatisseTheme.current
    val systemBarsTheme = matisseTheme.systemBarsTheme
    val statusBarColor = Color.Transparent
    val statusBarDarkIcons = if (previewPageVisible) {
        false
    } else {
        systemBarsTheme.statusBarDarkIcons
    }
    val navigationBarColor = if (previewPageVisible) {
        Color(color = matisseTheme.previewBackgroundColor)
    } else {
        Color(color = systemBarsTheme.navigationBarColor)
    }
    val navigationBarDarkIcons = if (previewPageVisible) {
        false
    } else {
        systemBarsTheme.navigationBarDarkIcons
    }
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = statusBarColor,
        darkIcons = statusBarDarkIcons,
    )
    systemUiController.setNavigationBarColor(
        color = navigationBarColor,
        darkIcons = navigationBarDarkIcons,
    )
}