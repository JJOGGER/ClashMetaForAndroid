package com.xboard.utils

import android.content.res.Configuration
import android.content.res.Resources
import com.xboard.ui.viewmodel.ColorSchemeSelection
import com.xboard.ui.viewmodel.ThemeViewModel

/**
 * 主题管理工具类
 * 统一管理主题加载和应用逻辑
 */
object ThemeHelper {
    
    /**
     * 从 MMKV 加载主题设置并应用到 ThemeViewModel
     */
    fun loadThemeSettings(themeViewModel: ThemeViewModel, resources: Resources) {
        // 加载颜色方案
        val savedColorScheme = MMKVUtil.getInstance().getIntValue("color_scheme", 0)
        val colorScheme = when (savedColorScheme) {
            0 -> ColorSchemeSelection.PURPLE
            1 -> ColorSchemeSelection.BLUE
            2 -> ColorSchemeSelection.GREEN
            3 -> ColorSchemeSelection.ORANGE
            else -> ColorSchemeSelection.PURPLE
        }
        themeViewModel.changeColorScheme(colorScheme)
        
        // 加载主题模式（0=light, 1=dark, 2=system）
        val themeMode = MMKVUtil.getInstance().getIntValue("theme_mode", 2)
        val isDarkMode = when (themeMode) {
            0 -> false
            1 -> true
            else -> {
                // 系统默认，检查系统是否处于暗黑模式
                val nightModeFlags = resources.configuration.uiMode and 
                    Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            }
        }
        themeViewModel.toggleDarkMode(isDarkMode)
    }
    
    /**
     * 获取当前是否为暗黑模式
     */
    fun isDarkMode(resources: Resources): Boolean {
        val themeMode = MMKVUtil.getInstance().getIntValue("theme_mode", 2)
        return when (themeMode) {
            0 -> false
            1 -> true
            else -> {
                val nightModeFlags = resources.configuration.uiMode and 
                    Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
    
    /**
     * 获取当前颜色方案
     */
    fun getCurrentColorScheme(): ColorSchemeSelection {
        val savedColorScheme = MMKVUtil.getInstance().getIntValue("color_scheme", 0)
        return when (savedColorScheme) {
            0 -> ColorSchemeSelection.PURPLE
            1 -> ColorSchemeSelection.BLUE
            2 -> ColorSchemeSelection.GREEN
            3 -> ColorSchemeSelection.ORANGE
            else -> ColorSchemeSelection.PURPLE
        }
    }
}

