package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.service.store.ServiceStore
import com.xboard.api.RetrofitClient
import com.xboard.model.UpdateUserRequest
import com.xboard.network.UserRepository
import com.xboard.event.ThemeChangedEvent
import com.xboard.storage.MMKVManager
import com.xboard.utils.MMKVUtil
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenrobot.eventbus.EventBus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OtherSettingsUiState(
    val accessControlMode: AccessControlMode = AccessControlMode.AcceptAll,
    val expireNotification: Boolean = true,
    val trafficNotification: Boolean = true,
    val colorScheme: ColorSchemeSelection = ColorSchemeSelection.PURPLE,
    val isDarkMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAccessControlDialog: Boolean = false
)

class OtherSettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val service by lazy { ServiceStore(application.applicationContext) }
    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    
    private val _uiState = MutableStateFlow(OtherSettingsUiState())
    val uiState: StateFlow<OtherSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        val userInfo = MMKVManager.getUserInfo()
        val savedColorScheme = MMKVUtil.getInstance().getIntValue("color_scheme", 0)
        val colorScheme = when (savedColorScheme) {
            0 -> ColorSchemeSelection.PURPLE
            1 -> ColorSchemeSelection.BLUE
            2 -> ColorSchemeSelection.GREEN
            3 -> ColorSchemeSelection.ORANGE
            else -> ColorSchemeSelection.PURPLE
        }
        val themeMode = MMKVManager.getThemeMode() // 0=light, 1=dark, 2=system
        val isDarkMode = when (themeMode) {
            0 -> false
            1 -> true
            else -> {
                // 系统默认，检查系统是否处于暗黑模式
                val nightModeFlags = getApplication<Application>().resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
        
        _uiState.value = OtherSettingsUiState(
            accessControlMode = service::accessControlMode.get(),
            expireNotification = userInfo?.remindExpire ?: true,
            trafficNotification = userInfo?.remindTraffic ?: true,
            colorScheme = colorScheme,
            isDarkMode = isDarkMode
        )
    }
    
    fun updateAccessControlMode(mode: AccessControlMode) {
        service::accessControlMode.set(mode)
        _uiState.value = _uiState.value.copy(accessControlMode = mode)
    }
    
    fun updateNotificationSettings(type: String, enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val result = when (type) {
                    "expire" -> userRepository.updateUserInfo(
                        UpdateUserRequest(remind_expire = if (enabled) 1 else 0)
                    )
                    "traffic" -> userRepository.updateUserInfo(
                        UpdateUserRequest(remind_traffic = if (enabled) 1 else 0)
                    )
                    else -> return@launch
                }
                
                result
                    .onSuccess {
                        when (type) {
                            "expire" -> {
                                val userInfo = MMKVManager.getUserInfo()
                                userInfo?.remindExpire = enabled
                                MMKVManager.setUserInfo(userInfo)
                                _uiState.value = _uiState.value.copy(
                                    expireNotification = enabled,
                                    isLoading = false
                                )
                            }
                            "traffic" -> {
                                val userInfo = MMKVManager.getUserInfo()
                                userInfo?.remindTraffic = enabled
                                MMKVManager.setUserInfo(userInfo)
                                _uiState.value = _uiState.value.copy(
                                    trafficNotification = enabled,
                                    isLoading = false
                                )
                            }
                        }
                    }
                    .onError { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun getAccessControlModeText(mode: AccessControlMode): String {
        return when (mode) {
            AccessControlMode.AcceptAll -> "允许所有应用"
            AccessControlMode.AcceptSelected -> "仅允许已选择的应用"
            AccessControlMode.DenySelected -> "不允许已选择的应用"
            else -> "允许所有应用"
        }
    }
    
    fun shouldShowAccessControlPackages(): Boolean {
        return _uiState.value.accessControlMode != AccessControlMode.AcceptAll
    }
    
    fun updateColorScheme(colorScheme: ColorSchemeSelection) {
        val schemeValue = when (colorScheme) {
            ColorSchemeSelection.PURPLE -> 0
            ColorSchemeSelection.BLUE -> 1
            ColorSchemeSelection.GREEN -> 2
            ColorSchemeSelection.ORANGE -> 3
        }
        MMKVUtil.getInstance().setValue("color_scheme", schemeValue)
        _uiState.value = _uiState.value.copy(colorScheme = colorScheme)
        // 发送主题变化事件，通知所有页面更新主题
        EventBus.getDefault().post(ThemeChangedEvent())
    }
    
    fun showAccessControlModeDialog() {
        _uiState.value = _uiState.value.copy(showAccessControlDialog = true)
    }
    
    fun hideAccessControlModeDialog() {
        _uiState.value = _uiState.value.copy(showAccessControlDialog = false)
    }
    
    fun toggleDarkMode(isDark: Boolean) {
        val themeMode = if (isDark) 1 else 0 // 0=light, 1=dark
        MMKVUtil.getInstance().setValue("theme_mode", themeMode)
        _uiState.value = _uiState.value.copy(isDarkMode = isDark)
        // 发送主题变化事件，通知所有页面更新主题
        EventBus.getDefault().post(ThemeChangedEvent())
    }
}
