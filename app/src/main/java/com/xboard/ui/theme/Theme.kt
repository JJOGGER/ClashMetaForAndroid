package com.xboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.xboard.ui.viewmodel.ColorSchemeSelection

private val LightPurpleColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFBF7FF), // 柔和的紫色调背景
    surface = Color(0xFFFBF7FF),
    surfaceContainerHigh = Color(0xFFF5F0FF), // 稍深的表面容器
    primaryContainer = Color(0xFFEADDFF), // 浅紫色容器
    onPrimaryContainer = Color(0xFF21005D), // 深紫色文字
    secondaryContainer = Color(0xFFE8DEF8), // 浅紫色灰色容器
    onSecondaryContainer = Color(0xFF1D192B), // 深色文字
)

private val DarkPurpleColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    surfaceContainerHigh = Color(0xFF2A2730), // 深色模式下的稍亮表面
    primaryContainer = Color(0xFF4F378B), // 深色模式下的主色容器
    onPrimaryContainer = Color(0xFFEADDFF), // 浅色文字
    secondaryContainer = Color(0xFF332D41), // 深色模式下的次色容器
    onSecondaryContainer = Color(0xFFE8DEF8), // 浅色文字
)

private val LightBlueColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    tertiary = BlueTertiary,
    background = Color(0xFFF5F7FA), // 柔和的蓝色调背景
    surface = Color(0xFFF5F7FA),
    surfaceContainerHigh = Color(0xFFE8F1F8), // 稍深的蓝色调表面
    primaryContainer = Color(0xFFD6E3F2), // 浅蓝色容器
    onPrimaryContainer = Color(0xFF0D47A1), // 深蓝色文字
    secondaryContainer = Color(0xFFE1ECF4), // 浅蓝色灰色容器
    onSecondaryContainer = Color(0xFF1A237E), // 深色文字
)

private val DarkBlueColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),  // Material Blue 200
    secondary = Color(0xFF64B5F6),  // Material Blue 300
    tertiary = Color(0xFF42A5F5),  // Material Blue 400
    background = Color(0xFF191C1D),
    surface = Color(0xFF191C1D),
    surfaceContainerHigh = Color(0xFF252A2E), // 深色模式下的稍亮表面
    primaryContainer = Color(0xFF1565C0), // 深色模式下的主色容器
    onPrimaryContainer = Color(0xFFD6E3F2), // 浅色文字
    secondaryContainer = Color(0xFF1E2830), // 深色模式下的次色容器
    onSecondaryContainer = Color(0xFFE1ECF4), // 浅色文字
)

private val LightGreenColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = GreenSecondary,
    tertiary = GreenTertiary,
    background = Color(0xFFF1F8F4), // 柔和的绿色调背景
    surface = Color(0xFFF1F8F4),
    surfaceContainerHigh = Color(0xFFE8F5EB), // 稍深的绿色调表面
    primaryContainer = Color(0xFFC8E6C9), // 浅绿色容器
    onPrimaryContainer = Color(0xFF1B5E20), // 深绿色文字
    secondaryContainer = Color(0xFFD4EDDA), // 浅绿色灰色容器
    onSecondaryContainer = Color(0xFF2E7D32), // 深色文字
)

private val DarkGreenColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),  // Material Green 300
    secondary = Color(0xFF66BB6A),  // Material Green 400
    tertiary = Color(0xFF4CAF50),  // Material Green 500
    background = Color(0xFF1A1D1A),
    surface = Color(0xFF1A1D1A),
    surfaceContainerHigh = Color(0xFF242A26), // 深色模式下的稍亮表面
    primaryContainer = Color(0xFF2E7D32), // 深色模式下的主色容器
    onPrimaryContainer = Color(0xFFC8E6C9), // 浅色文字
    secondaryContainer = Color(0xFF1E3320), // 深色模式下的次色容器
    onSecondaryContainer = Color(0xFFD4EDDA), // 浅色文字
)

private val LightOrangeColorScheme = lightColorScheme(
    primary = OrangePrimary,
    secondary = OrangeSecondary,
    tertiary = OrangeTertiary,
    background = Color(0xFFFEF7F0), // 柔和的橙色调背景
    surface = Color(0xFFFEF7F0),
    surfaceContainerHigh = Color(0xFFF8EDE0), // 稍深的橙色调表面
    primaryContainer = Color(0xFFFFE0B2), // 浅橙色容器
    onPrimaryContainer = Color(0xFFE65100), // 深橙色文字
    secondaryContainer = Color(0xFFFFE8CC), // 浅橙色灰色容器
    onSecondaryContainer = Color(0xFFF57C00), // 深色文字
)

private val DarkOrangeColorScheme = darkColorScheme(
    primary = Color(0xFFFFB74D),  // Material Orange 300
    secondary = Color(0xFFFFA726),  // Material Orange 400
    tertiary = Color(0xFFFF9800),  // Material Orange 500
    background = Color(0xFF1F1B16),
    surface = Color(0xFF1F1B16),
    surfaceContainerHigh = Color(0xFF2A2520), // 深色模式下的稍亮表面
    primaryContainer = Color(0xFFE65100), // 深色模式下的主色容器
    onPrimaryContainer = Color(0xFFFFE0B2), // 浅色文字
    secondaryContainer = Color(0xFF2E261F), // 深色模式下的次色容器
    onSecondaryContainer = Color(0xFFFFE8CC), // 浅色文字
)

/**
 * 获取颜色方案
 * 此函数设计为易于扩展新主题颜色
 */
@Composable
private fun getColorScheme(
    colorSchemeSelection: ColorSchemeSelection,
    darkTheme: Boolean
): androidx.compose.material3.ColorScheme {
    return when (colorSchemeSelection) {
        ColorSchemeSelection.PURPLE -> if (darkTheme) DarkPurpleColorScheme else LightPurpleColorScheme
        ColorSchemeSelection.BLUE -> if (darkTheme) DarkBlueColorScheme else LightBlueColorScheme
        ColorSchemeSelection.GREEN -> if (darkTheme) DarkGreenColorScheme else LightGreenColorScheme
        ColorSchemeSelection.ORANGE -> if (darkTheme) DarkOrangeColorScheme else LightOrangeColorScheme
        // 添加新主题时，只需在此处添加新的 case
        // 并在文件顶部定义对应的 LightXxxColorScheme 和 DarkXxxColorScheme
    }
}

@Composable
fun MaClashTheme(
    colorSchemeSelection: ColorSchemeSelection = ColorSchemeSelection.PURPLE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(colorSchemeSelection, darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            // 暗黑模式使用深色背景，浅色模式使用主色背景
            val statusBarColor = if (darkTheme) {
                colorScheme.surface.toArgb()
            } else {
                colorScheme.primary.toArgb()
            }
            window.statusBarColor = statusBarColor
            // 设置状态栏图标颜色：暗黑模式用浅色图标，浅色模式用深色图标
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
            // 确保窗口使用沉浸式模式
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }
    
    // 使用 LaunchedEffect 确保在主题变化时立即更新状态栏
    LaunchedEffect(colorSchemeSelection, darkTheme) {
        if (!view.isInEditMode) {
            val window = (view.context as android.app.Activity).window
            val statusBarColor = if (darkTheme) {
                colorScheme.surface.toArgb()
            } else {
                colorScheme.primary.toArgb()
            }
            window.statusBarColor = statusBarColor
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
