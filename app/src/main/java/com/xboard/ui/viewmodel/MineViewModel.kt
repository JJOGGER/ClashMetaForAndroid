package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.event.OrderPayEvent
import com.xboard.event.TicketClosedEvent
import com.xboard.event.TicketCreatedEvent
import com.xboard.model.SubscribeResponse
import com.xboard.model.UserInfo
import com.xboard.model.UserStat
import com.xboard.network.AuthRepository
import com.xboard.network.UserRepository
import com.xboard.storage.MMKVManager
import com.xboard.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.DecimalFormat

data class MineUiState(
    val isLoading: Boolean = false,
    val userEmail: String = "",
    val userBalance: String = "¥0.00",
    val commissionBalance: String = "¥0.00",
    val hasSubscribe: Boolean = false,
    val planName: String = "",
    val trafficUsed: String = "0 B",
    val trafficTotal: String = "0 B",
    val trafficPercentage: Int = 0,
    val expireDate: String = "",
    val orderCount: Int = 0,
    val ticketCount: Int = 0,
    val errorMessage: String? = null
)

class MineViewModel(application: Application, val userRepository: UserRepository) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(MineUiState())
    val uiState: StateFlow<MineUiState> = _uiState.asStateFlow()
    
    private val priceFormatter = DecimalFormat("#,##0.00")
    
    init {
        // 注册EventBus监听支付事件
        EventBus.getDefault().register(this)
        loadAllData()
    }
    
    override fun onCleared() {
        super.onCleared()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderPayEvent(event: OrderPayEvent) {
        // 支付成功后刷新数据
        loadAllData()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTicketClosed(event: TicketClosedEvent) {
        // 工单关闭后刷新工单数量
        loadUserState()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTicketCreated(event: TicketCreatedEvent) {
        // 工单创建后刷新工单数量
        loadUserState()
    }
    
    fun loadAllData() {
        loadUserInfo()
        loadUserState()
        loadInviteStats()
    }
    
    fun refreshData() {
        loadAllData()
    }
    
    private fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = userRepository.getUserInfo()
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    userEmail = user.email,
                    userBalance = "¥${formatCurrency(user.balance)}",
                    commissionBalance = "¥${formatCurrency(user.commissionBalance)}"
                )
                // 加载订阅信息
                loadSubscribeInfo()
            }.onError { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message
                )
            }
        }
    }
    
    private fun loadSubscribeInfo() {
        viewModelScope.launch {
            val result = userRepository.getSubscribe()
            result.onSuccess { subscribe ->
                MMKVManager.saveSubscribe(subscribe)
                updateSubscribeDetails(subscribe)
            }.onError {
                // API 加载失败时，尝试从缓存加载
                loadSubscribeInfoFromCache()
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * 从本地缓存加载订阅信息（不发起网络请求）
     */
    private fun loadSubscribeInfoFromCache() {
        try {
            val subscribe = MMKVManager.getSubscribe()
            if (subscribe != null) {
                updateSubscribeDetails(subscribe)
            } else {
                // 缓存也没有，重置为未订阅
                _uiState.value = _uiState.value.copy(
                    hasSubscribe = false,
                    planName = "",
                    trafficUsed = "0 B",
                    trafficTotal = "0 B",
                    trafficPercentage = 0,
                    expireDate = "未订阅"
                )
            }
        } catch (e: Exception) {
            // 缓存加载失败，保持当前状态
        }
    }
    
    private fun updateSubscribeDetails(subscribe: SubscribeResponse) {
        // 检查是否有有效订阅：需要 planId 不为 null 且 plan 存在
        // expiredAt 可能为 0 或无效，但只要 planId 和 plan 存在就认为有订阅
        val hasValidSubscribe = subscribe.planId != null && subscribe.plan != null
        
        if (!hasValidSubscribe) {
            // 没有有效订阅，重置状态
            _uiState.value = _uiState.value.copy(
                hasSubscribe = false,
                planName = "",
                trafficUsed = "0 B",
                trafficTotal = "0 B",
                trafficPercentage = 0,
                expireDate = "未订阅"
            )
            return
        }
        
        // 检查订阅是否已过期（如果有过期时间）
        val expiredAtTimestamp = subscribe.expiredAt?.toLongOrNull() ?: 0L
        val currentTime = System.currentTimeMillis() / 1000
        val isExpired = expiredAtTimestamp > 0 && expiredAtTimestamp < currentTime
        
        val totalTraffic = subscribe.transferEnable
        val uploadTraffic = subscribe.upload
        val downloadTraffic = subscribe.download
        val usedTraffic = uploadTraffic + downloadTraffic
        
        val percentage = if (totalTraffic > 0) {
            ((usedTraffic * 100) / totalTraffic).toInt().coerceIn(0, 100)
        } else {
            0
        }
        
        // 格式化到期时间
        val expireDateText = if (expiredAtTimestamp > 0) {
            val expireDate = DateUtils.getStringTime(
                expiredAtTimestamp * 1000,
                "yyyy-MM-dd"
            )
            if (isExpired) {
                "已过期：$expireDate"
            } else {
                "有效期至：$expireDate"
            }
        } else {
            "长期有效"
        }
        
        _uiState.value = _uiState.value.copy(
            hasSubscribe = true,
            planName = subscribe.plan.name,
            trafficUsed = formatTraffic(usedTraffic),
            trafficTotal = formatTraffic(totalTraffic),
            trafficPercentage = percentage,
            expireDate = expireDateText
        )
    }
    
    private fun loadUserState() {
        viewModelScope.launch {
            val result = userRepository.getUserStat()
            result.onSuccess { stat ->
                _uiState.value = _uiState.value.copy(
                    orderCount = stat.orderCount,
                    ticketCount = stat.tickerCount
                )
            }.onError {
                // 加载失败不影响页面显示
            }
        }
    }
    
    private fun loadInviteStats() {
        viewModelScope.launch {
            val result = userRepository.getUserInfo()
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    commissionBalance = "返利余额 ¥${formatCurrency(user.commissionBalance)}"
                )
            }.onError {
                // 加载失败不影响页面显示
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            val authRepository = AuthRepository(com.xboard.api.RetrofitClient.getApiService())
            authRepository.logout()
            MMKVManager.clearToken()
            MMKVManager.clearUserInfo()
            MMKVManager.clearSubscribeCache()
            MMKVManager.setUserConfigResponse(null)
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        val rounded = amount / 100.0
        return priceFormatter.format(rounded)
    }
    
    private fun formatTraffic(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            bytes < 1024L * 1024 * 1024 * 1024 -> String.format(
                "%.2f GB",
                bytes / (1024.0 * 1024 * 1024)
            )
            else -> String.format("%.2f TB", bytes / (1024.0 * 1024 * 1024 * 1024))
        }
    }
    
    companion object {
        fun Factory(application: Application, userRepository: UserRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return MineViewModel(application, userRepository) as T
                }
            }
        }
    }
}

