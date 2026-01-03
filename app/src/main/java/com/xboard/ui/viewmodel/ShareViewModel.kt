package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.model.InviteCode
import com.xboard.model.InviteDetailsResponse
import com.xboard.network.InviteRepository
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 分享/返利页面 ViewModel
 */
class ShareViewModel(application: Application) : AndroidViewModel(application) {
    
    private val inviteRepository = InviteRepository(RetrofitClient.getApiService())
    private val userRepository = UserRepository(RetrofitClient.getApiService())
    
    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()
    
    /**
     * UI 状态
     */
    data class ShareUiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        
        // 佣金信息
        val commissionBalance: Double = 0.0, // 佣金余额（元）
        val commissionRate: Int = 0, // 佣金比例（%）
        val currency: String = "CNY",
        
        // 邀请统计
        val inviteCount: Int = 0, // 已注册用户数
        val reconfirmBalance: Double = 0.0, // 确认中的佣金（元）
        val totalBalance: Double = 0.0, // 累计获得佣金（元）
        
        // 邀请码列表
        val inviteCodes: List<InviteCode> = emptyList(),
        
        // 划转相关
        val showTransferDialog: Boolean = false,
        val transferAmount: String = "",
        val isTransferring: Boolean = false
    )
    
    init {
        loadData()
    }
    
    /**
     * 加载数据
     */
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            // 加载用户信息（获取返利余额和返利比例）
            val userResult = userRepository.getUserInfo()
            userResult.onSuccess { userInfo ->
                _uiState.value = _uiState.value.copy(
                    commissionBalance = userInfo.commissionBalance / 100.0,
                    commissionRate = userInfo.commissionRate,
                    currency = MMKVManager.getUserConfig()?.currency ?: "CNY"
                )
            }.onError { error ->
                _uiState.value = _uiState.value.copy(
                    error = "获取用户信息失败: ${error.message}",
                    isLoading = false
                )
                return@launch
            }
            
            // 加载邀请信息
            val infoResult = inviteRepository.getInviteInfo()
            infoResult.onSuccess { inviteInfo ->
                updateInviteInfo(inviteInfo)
            }.onError { error ->
                _uiState.value = _uiState.value.copy(
                    error = "获取邀请信息失败: ${error.message}",
                    isLoading = false
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            // 加载用户信息
            val userResult = userRepository.getUserInfo()
            userResult.onSuccess { userInfo ->
                _uiState.value = _uiState.value.copy(
                    commissionBalance = userInfo.commissionBalance / 100.0,
                    commissionRate = userInfo.commissionRate
                )
            }
            
            // 加载邀请信息
            val infoResult = inviteRepository.getInviteInfo()
            infoResult.onSuccess { inviteInfo ->
                updateInviteInfo(inviteInfo)
            }
            
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }
    
    /**
     * 更新邀请信息
     */
    private fun updateInviteInfo(inviteInfo: InviteDetailsResponse) {
        val state = _uiState.value
        val newState = if (inviteInfo.codes.isNotEmpty() && inviteInfo.stat.isNotEmpty()) {
            state.copy(
                inviteCount = inviteInfo.stat.getOrNull(0) ?: 0,
                commissionRate = inviteInfo.stat.getOrNull(3) ?: state.commissionRate,
                reconfirmBalance = (inviteInfo.stat.getOrNull(1) ?: 0) / 100.0,
                totalBalance = (inviteInfo.stat.getOrNull(2) ?: 0) / 100.0,
                inviteCodes = inviteInfo.codes
            )
        } else {
            state.copy(inviteCodes = inviteInfo.codes)
        }
        _uiState.value = newState
    }
    
    /**
     * 生成邀请码
     */
    fun generateInviteCode() {
        viewModelScope.launch {
            val result = inviteRepository.generateInviteCode()
            result.onSuccess { success ->
                if (success) {
                    // 生成成功，重新加载邀请信息
                    val infoResult = inviteRepository.getInviteInfo()
                    infoResult.onSuccess { inviteInfo ->
                        updateInviteInfo(inviteInfo)
                    }.onError { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "获取邀请码失败: ${error.message}"
                        )
                    }
                }
            }.onError { error ->
                _uiState.value = _uiState.value.copy(
                    error = "生成邀请码失败: ${error.message}"
                )
            }
        }
    }
    
    /**
     * 显示划转对话框
     */
    fun showTransferDialog() {
        _uiState.value = _uiState.value.copy(
            showTransferDialog = true,
            transferAmount = ""
        )
    }
    
    /**
     * 隐藏划转对话框
     */
    fun hideTransferDialog() {
        _uiState.value = _uiState.value.copy(
            showTransferDialog = false,
            transferAmount = ""
        )
    }
    
    /**
     * 更新划转金额
     */
    fun updateTransferAmount(amount: String) {
        _uiState.value = _uiState.value.copy(transferAmount = amount)
    }
    
    /**
     * 执行划转
     */
    fun transferCommission() {
        val amount = _uiState.value.transferAmount.toDoubleOrNull() ?: 0.0
        val maxBalance = _uiState.value.commissionBalance
        
        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(
                error = "请输入有效的划转金额"
            )
            return
        }
        
        if (amount > maxBalance) {
            _uiState.value = _uiState.value.copy(
                error = "划转金额不能超过余额"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTransferring = true)
            
            val result = inviteRepository.transferCommission(amount)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    showTransferDialog = false,
                    transferAmount = "",
                    isTransferring = false
                )
                // 重新加载数据
                loadData()
            }.onError { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "划转失败",
                    isTransferring = false
                )
            }
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

