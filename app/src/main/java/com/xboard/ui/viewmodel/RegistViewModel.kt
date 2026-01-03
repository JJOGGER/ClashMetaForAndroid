package com.xboard.ui.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.api.TokenManager
import com.xboard.model.CommConfigResponse
import com.xboard.network.AuthRepository
import com.xboard.storage.MMKVManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistUiState(
    val email: String = "",
    val emailCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val inviteCode: String = "",
    val isAgreementChecked: Boolean = false,
    val isLoading: Boolean = false,
    val isSendingCode: Boolean = false,
    val countdownSeconds: Int = 0,
    val error: String? = null,
    val config: CommConfigResponse? = MMKVManager.getCommConfig()
)

class RegistViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(RetrofitClient.getApiService())
    private var countdownJob: Job? = null
    
    private val _uiState = MutableStateFlow(RegistUiState())
    val uiState: StateFlow<RegistUiState> = _uiState.asStateFlow()
    
    fun setEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }
    
    fun setEmailCode(emailCode: String) {
        _uiState.update { it.copy(emailCode = emailCode) }
    }
    
    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }
    
    fun setConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword) }
    }
    
    fun setInviteCode(inviteCode: String) {
        _uiState.update { it.copy(inviteCode = inviteCode) }
    }
    
    fun setAgreementChecked(checked: Boolean) {
        _uiState.update { it.copy(isAgreementChecked = checked) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun sendEmailCode(
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
            
            val result = authRepository.sendEmailVerifyCode(email, "register")
            
            result.onSuccess {
                _uiState.update { it.copy(isSendingCode = false) }
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
    
    fun register(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password.trim()
        val emailCode = state.emailCode.trim()
        val inviteCode = state.inviteCode.trim()
        val config = state.config
        
        // 验证输入
        val validationError = validateInput(email, password, state.confirmPassword.trim(), emailCode, inviteCode, state.isAgreementChecked, config)
        if (validationError != null) {
            onError(validationError)
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = authRepository.register(
                email,
                password,
                emailCode,
                inviteCode.ifEmpty { null }
            )
            
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
    
    private fun validateInput(
        email: String,
        password: String,
        confirmPassword: String,
        emailCode: String,
        inviteCode: String,
        isAgreementChecked: Boolean,
        config: CommConfigResponse?
    ): String? {
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
        if (password != confirmPassword) {
            return "两次密码输入不一致"
        }
        if (config?.isEmailVerify == 1 && emailCode.isEmpty()) {
            return "请输入邮箱验证码"
        }
        if (config?.isInviteForce == 1 && inviteCode.isEmpty()) {
            return "请输入邀请码"
        }
        if (!isAgreementChecked) {
            return "请勾选用户协议和隐私政策"
        }
        return null
    }
    
    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}

