package github.leavesczy.matisse

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * @Author: CZY
 * @Date: 2022/6/10 11:01
 * @Desc:
 */
/**
 * @param backgroundColor 主页的背景色
 * @param previewBackgroundColor 图片预览页的背景色
 * @param imageBackgroundColor 图片的背景色
 * @param alphaIfDisable 当控件处于不可点击状态时文本颜色和背景色的透明度
 * @param topAppBarTheme 主页顶部标题栏的主题
 * @param bottomNavigationTheme 主页底部导航栏的主题
 * @param captureIconTheme 拍照 Icon 的主题
 * @param dropdownMenuTheme 下拉菜单的主题
 * @param checkBoxTheme CheckBox 的主题
 * @param previewButtonTheme 预览按钮的主题
 * @param sureButtonTheme 确定按钮的主题
 * @param systemBarsTheme 系统状态栏和导航栏的主题
 */
@Parcelize
data class MatisseTheme(
    val backgroundColor: Long,
    val previewBackgroundColor: Long,
    val imageBackgroundColor: Long,
    val alphaIfDisable: Float,
    val topAppBarTheme: TopAppBarTheme,
    val bottomNavigationTheme: BottomNavigationTheme,
    val captureIconTheme: CaptureIconTheme,
    val dropdownMenuTheme: DropdownMenuTheme,
    val checkBoxTheme: CheckBoxTheme,
    val previewButtonTheme: PreviewButtonTheme,
    val sureButtonTheme: SureButtonTheme,
    val systemBarsTheme: SystemBarsTheme,
) : Parcelable

@Parcelize
data class TextTheme(val color: Long, val fontSize: Int) : Parcelable {

    @IgnoredOnParcel
    internal val textStyle = TextStyle(color = Color(color = color), fontSize = fontSize.sp)

}

@Parcelize
data class CaptureIconTheme(
    val backgroundColor: Long,
    val iconTint: Long,
) : Parcelable

@Parcelize
data class TopAppBarTheme(
    val defaultBucketName: String,
    val backgroundColor: Long,
    val iconColor: Long,
    val textTheme: TextTheme
) : Parcelable

@Parcelize
data class BottomNavigationTheme(
    val backgroundColor: Long
) : Parcelable

@Parcelize
data class DropdownMenuTheme(
    val backgroundColor: Long, val textTheme: TextTheme
) : Parcelable

@Parcelize
data class PreviewButtonTheme(
    val textTheme: TextTheme, val textBuilder: (selectedSize: Int, maxSelectable: Int) -> String
) : Parcelable

@Parcelize
data class SureButtonTheme(
    val backgroundColor: Long,
    val textTheme: TextTheme,
    val textBuilder: (selectedSize: Int, maxSelectable: Int) -> String
) : Parcelable

@Parcelize
data class CheckBoxTheme(
    val countable: Boolean,
    val frameColor: Long,
    val circleColor: Long,
    val circleFillColor: Long,
    val textTheme: TextTheme
) : Parcelable

@Parcelize
data class SystemBarsTheme(
    val statusBarColor: Long,
    val statusBarDarkIcons: Boolean,
    val navigationBarColor: Long,
    val navigationBarDarkIcons: Boolean
) : Parcelable

private const val whiteColor: Long = 0xFFFFFFFF
private const val blackColor: Long = 0xFF000000
private const val previewBackgroundColor: Long = 0xFF2B2A34
private const val blueColor: Long = 0xFF2196F3
private const val greenColor: Long = 0xFF009688

private const val lightSystemBarColor: Long = 0xFFFFFFFF
private const val lightBackgroundColor: Long = 0xFFFFFFFF
private const val lightImageBackgroundColor: Long = 0x66CCCCCC
private const val lightCaptureIconBackgroundColor: Long = 0xFFF1F1F1
private const val lightCaptureIconTintColor: Long = 0x66000000

private const val darkSystemBarColor: Long = previewBackgroundColor
private const val darkBackgroundColor: Long = 0xFF22202A
private const val darkImageBackgroundColor: Long = 0x66CCCCCC
private const val darkCaptureIconBackgroundColor: Long = 0x66CCCCCC
private const val darkCaptureIconTintColor: Long = 0xFFFFFFFF

private val previewTextBuilder = { selectedSize: Int, maxSelectable: Int ->
    "预览($selectedSize/$maxSelectable)"
}
private val sureTextBuilder = { _: Int, _: Int ->
    "确定"
}
private const val alphaIfDisable = 0.5f
private const val defaultBucketName = "全部图片"

val LightMatisseTheme = MatisseTheme(
    backgroundColor = lightBackgroundColor,
    previewBackgroundColor = previewBackgroundColor,
    imageBackgroundColor = lightImageBackgroundColor,
    alphaIfDisable = alphaIfDisable,
    captureIconTheme = CaptureIconTheme(
        backgroundColor = lightCaptureIconBackgroundColor, iconTint = lightCaptureIconTintColor
    ),
    topAppBarTheme = TopAppBarTheme(
        defaultBucketName = defaultBucketName,
        backgroundColor = lightSystemBarColor,
        iconColor = blackColor,
        textTheme = TextTheme(
            fontSize = 19, color = blackColor
        )
    ),
    bottomNavigationTheme = BottomNavigationTheme(
        backgroundColor = lightSystemBarColor,
    ),
    dropdownMenuTheme = DropdownMenuTheme(
        backgroundColor = lightBackgroundColor,
        textTheme = TextTheme(
            fontSize = 14, color = blackColor
        ),
    ),
    checkBoxTheme = CheckBoxTheme(
        frameColor = blueColor,
        circleColor = whiteColor,
        circleFillColor = blueColor,
        countable = true,
        textTheme = TextTheme(
            fontSize = 14, color = whiteColor
        )
    ),
    previewButtonTheme = PreviewButtonTheme(
        textBuilder = previewTextBuilder, textTheme = TextTheme(
            fontSize = 14, color = blackColor
        )
    ),
    sureButtonTheme = SureButtonTheme(
        textBuilder = sureTextBuilder, textTheme = TextTheme(
            fontSize = 14, color = whiteColor
        ), backgroundColor = blueColor
    ),
    systemBarsTheme = SystemBarsTheme(
        statusBarColor = lightSystemBarColor,
        statusBarDarkIcons = true,
        navigationBarColor = lightSystemBarColor,
        navigationBarDarkIcons = true
    )
)

val DarkMatisseTheme = MatisseTheme(
    backgroundColor = darkBackgroundColor,
    previewBackgroundColor = previewBackgroundColor,
    imageBackgroundColor = darkImageBackgroundColor,
    alphaIfDisable = alphaIfDisable,
    captureIconTheme = CaptureIconTheme(
        backgroundColor = darkCaptureIconBackgroundColor, iconTint = darkCaptureIconTintColor
    ),
    topAppBarTheme = TopAppBarTheme(
        defaultBucketName = defaultBucketName,
        backgroundColor = darkSystemBarColor,
        iconColor = whiteColor,
        textTheme = TextTheme(
            fontSize = 19, color = whiteColor
        )
    ),
    bottomNavigationTheme = BottomNavigationTheme(
        backgroundColor = darkSystemBarColor
    ),
    dropdownMenuTheme = DropdownMenuTheme(
        backgroundColor = darkSystemBarColor, textTheme = TextTheme(
            fontSize = 14, color = whiteColor
        )
    ),
    checkBoxTheme = CheckBoxTheme(
        countable = true,
        frameColor = greenColor,
        circleColor = whiteColor,
        circleFillColor = greenColor,
        textTheme = TextTheme(
            fontSize = 14, color = whiteColor
        )
    ),
    previewButtonTheme = PreviewButtonTheme(
        textBuilder = previewTextBuilder,
        textTheme = TextTheme(
            fontSize = 14, color = whiteColor
        ),
    ),
    sureButtonTheme = SureButtonTheme(
        textBuilder = sureTextBuilder,
        textTheme = TextTheme(
            fontSize = 14, color = whiteColor
        ), backgroundColor = greenColor
    ),
    systemBarsTheme = SystemBarsTheme(
        statusBarColor = darkSystemBarColor,
        statusBarDarkIcons = false,
        navigationBarColor = darkSystemBarColor,
        navigationBarDarkIcons = false
    )
)