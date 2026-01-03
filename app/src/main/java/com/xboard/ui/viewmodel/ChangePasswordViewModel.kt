package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.network.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChangePasswordViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(RetrofitClient.getApiService())
    
    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()
    
    fun setOldPassword(password: String) {
        _uiState.update { it.copy(oldPassword = password) }
    }
    
    fun setNewPassword(password: String) {
        _uiState.update { it.copy(newPassword = password) }
    }
    
    fun setConfirmPassword(password: String) {
        _uiState.update { it.copy(confirmPassword = password) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun changePassword(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val state = _uiState.value
        val oldPassword = state.oldPassword.trim()
        val newPassword = state.newPassword.trim()
        val confirmPassword = state.confirmPassword.trim()
        
        // 验证输入
        val validationError = validateInput(oldPassword, newPassword, confirmPassword)
        if (validationError != null) {
            onError(validationError)
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = authRepository.changePassword(oldPassword, newPassword)
            
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            }.onError { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
                onError(error.message)
            }
        }
    }
    
    private fun validateInput(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): String? {
        if (oldPassword.isEmpty()) {
            return "请输入旧密码"
        }
        if (newPassword.isEmpty()) {
            return "请输入新密码"
        }
        if (confirmPassword.isEmpty()) {
            return "请确认新密码"
        }
        if (newPassword.length < 6) {
            return "新密码至少6位"
        }
        if (newPassword != confirmPassword) {
            return "两次输入的密码不一致"
        }
        if (oldPassword == newPassword) {
            return "新密码不能与旧密码相同"
        }
        return null
    }
}

