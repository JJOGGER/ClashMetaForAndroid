package com.xboard.ui.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.network.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isSendingCode: Boolean = false,
    val countdownSeconds: Int = 0,
    val codeSent: Boolean = false,
    val error: String? = null
)

class ForgotPasswordViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(RetrofitClient.getApiService())
    private var countdownJob: Job? = null
    
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()
    
    fun setEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }
    
    fun setCode(code: String) {
        _uiState.update { it.copy(code = code) }
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
    
    fun sendVerificationCode(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val email = _uiState.value.email.trim()
        
        if (email.isEmpty()) {
            onError("请输入邮箱")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onError("邮箱格式不正确")
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCode = true, error = null) }
            
            val result = authRepository.sendEmailVerifyCode(email, "forget")
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isSendingCode = false,
                        codeSent = true
                    ) 
                }
                startCountdown()
                onSuccess()
            }.onError { error ->
                _uiState.update { it.copy(isSendingCode = false) }
                onError(error.message)
            }
        }
    }
    
    private fun startCountdown() {
        countdownJob?.cancel()
        var seconds = 60
        
        countdownJob = viewModelScope.launch {
            while (seconds > 0) {
                _uiState.update { it.copy(countdownSeconds = seconds) }
                delay(1000)
                seconds--
            }
            _uiState.update { it.copy(countdownSeconds = 0) }
        }
    }
    
    fun resetPassword(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val state = _uiState.value
        val email = state.email.trim()
        val code = state.code.trim()
        val newPassword = state.newPassword.trim()
        val confirmPassword = state.confirmPassword.trim()
        
        // 验证输入
        val validationError = validateInput(email, code, newPassword, confirmPassword)
        if (validationError != null) {
            onError(validationError)
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = authRepository.forgetPassword(email, code, newPassword)
            
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
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String
    ): String? {
        if (email.isEmpty() || code.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            return "请填写所有字段"
        }
        if (newPassword.length < 6) {
            return "密码至少6位"
        }
        if (newPassword != confirmPassword) {
            return "两次输入的密码不一致"
        }
        return null
    }
    
    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}

