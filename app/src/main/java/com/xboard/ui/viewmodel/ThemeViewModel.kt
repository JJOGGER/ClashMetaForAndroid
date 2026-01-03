package com.xboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ColorSchemeSelection {
    PURPLE,
    BLUE,
    GREEN,
    ORANGE
}

data class ThemeState(
    val colorScheme: ColorSchemeSelection = ColorSchemeSelection.PURPLE,
    val isDarkMode: Boolean = false
)

/**
 * 主题 ViewModel
 * 使用单例模式确保所有页面共享同一个主题状态
 */
class ThemeViewModel : ViewModel() {

    companion object {
        @Volatile
        private var INSTANCE: ThemeViewModel? = null
        
        fun getInstance(): ThemeViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeViewModel().also { INSTANCE = it }
            }
        }
    }

    private val _uiState = MutableStateFlow(ThemeState())
    val uiState: StateFlow<ThemeState> = _uiState.asStateFlow()

    fun changeColorScheme(colorScheme: ColorSchemeSelection) {
        if (_uiState.value.colorScheme != colorScheme) {
            _uiState.value = _uiState.value.copy(colorScheme = colorScheme)
        }
    }

    fun toggleDarkMode(isDark: Boolean) {
        if (_uiState.value.isDarkMode != isDark) {
            _uiState.value = _uiState.value.copy(isDarkMode = isDark)
        }
    }
    
    /**
     * 更新主题状态（不触发通知，用于初始化）
     */
    fun updateThemeState(
        colorScheme: ColorSchemeSelection,
        isDarkMode: Boolean
    ) {
        _uiState.value = ThemeState(colorScheme, isDarkMode)
    }
}
