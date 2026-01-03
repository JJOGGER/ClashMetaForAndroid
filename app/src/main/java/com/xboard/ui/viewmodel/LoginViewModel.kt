package com.xboard.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.api.TokenManager
import com.xboard.network.AuthRepository
import com.xboard.storage.MMKVManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = MMKVManager.getUserEmail() ?: "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(RetrofitClient.getApiService())
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun setEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }
    
    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun login(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()
        
        // 验证输入
        val validationError = validateInput(email, password)
        if (validationError != null) {
            onError(validationError)
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = authRepository.login(email, password)
            
            result.onSuccess { response ->
                TokenManager.saveToken(response.token, response.authData, email, password)
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            }.onError { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
                onError(error.message)
            }
        }
    }
    
    private fun validateInput(email: String, password: String): String? {
        if (email.isEmpty()) {
            return "请输入邮箱"
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "邮箱格式不正确"
        }
        if (password.isEmpty()) {
            return "请输入密码"
        }
        if (password.length < 6) {
            return "密码至少6位"
        }
        return null
    }
}

