package com.xboard.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.model.PaymentMethod
import com.xboard.network.OrderRepository
import com.xboard.util.AutoSubscriptionManager
import com.xboard.network.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PaymentUiState(
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val selectedMethodId: Int? = null,
    val isLoading: Boolean = false,
    val isLoadingPayment: Boolean = false,
    val loadingMessage: String? = null,
    val isPolling: Boolean = false,
    val pollingAttempts: Int = 0,
    val error: String? = null
)

class PaymentViewModel(
    application: Application,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var tradeNo: String? = null
    private var isPollingActive = false
    private val autoSubscriptionManager by lazy {
        AutoSubscriptionManager(userRepository, viewModelScope)
    }

    fun setTradeNo(tradeNo: String) {
        this.tradeNo = tradeNo
    }

    fun loadPaymentMethods() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = RetrofitClient.getApiService().getPaymentMethods()
                if (response.isSuccess() && response.data != null) {
                    _uiState.value = _uiState.value.copy(
                        paymentMethods = response.data,
                        isLoading = false
                    )
                    // 默认选择第一个支付方式
                    if (response.data.isNotEmpty() && _uiState.value.selectedMethodId == null) {
                        selectPaymentMethod(response.data.first().id)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "加载支付方式失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    fun selectPaymentMethod(methodId: Int) {
        _uiState.value = _uiState.value.copy(selectedMethodId = methodId)
    }

    fun checkout(
        onOpenBrowser: (String) -> Unit,
        onPaymentSuccess: () -> Unit
    ) {
        val currentTradeNo = tradeNo
        val selectedMethodId = _uiState.value.selectedMethodId

        if (currentTradeNo == null || selectedMethodId == null) {
            _uiState.value = _uiState.value.copy(error = "参数错误")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingPayment = true,
                loadingMessage = "正在发起支付...",
                error = null
            )

            val result = orderRepository.checkout(currentTradeNo, selectedMethodId)
            _uiState.value = _uiState.value.copy(
                isLoadingPayment = false,
                loadingMessage = null
            )

            result.onSuccess { checkoutData ->
                when (checkoutData?.type) {
                    1 -> {
                        // 支付链接，打开浏览器
                        try {
                            val url = checkoutData.data.toString()
                            onOpenBrowser(url)
                            startPollingPaymentStatus(currentTradeNo, onPaymentSuccess)
                        } catch (e: Exception) {
                            _uiState.value = _uiState.value.copy(error = "无法打开浏览器")
                        }
                    }
                    0 -> {
                        // 二维码（暂不处理，直接轮询）
                        startPollingPaymentStatus(currentTradeNo, onPaymentSuccess)
                    }
                    -1 -> {
                        // 免费订单，直接成功
                        updateSubscribeUrlAfterPayment(onPaymentSuccess)
                    }
                }
            }.onError { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "支付失败")
            }
        }
    }

    private fun startPollingPaymentStatus(
        tradeNo: String,
        onPaymentSuccess: () -> Unit
    ) {
        if (isPollingActive) return
        isPollingActive = true

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPolling = true, pollingAttempts = 0)
            var attempts = 0
            val maxAttempts = 120 // 2分钟（每秒检查一次）

            while (isPollingActive && attempts < maxAttempts) {
                try {
                    val result = orderRepository.checkOrderStatus(tradeNo)
                    _uiState.value = _uiState.value.copy(pollingAttempts = attempts)

                    result.onSuccess { status ->
                        when (status) {
                            1 -> {
                                // 已支付
                                stopPolling()
                                updateSubscribeUrlAfterPayment(onPaymentSuccess)
                                return@launch
                            }
                            -1, 3 -> {
                                // 已取消或已过期
                                stopPolling()
                                _uiState.value = _uiState.value.copy(
                                    error = "订单已取消",
                                    isPolling = false
                                )
                                return@launch
                            }
                            0 -> {
                                // 待支付，继续轮询
                            }
                        }
                    }
                    attempts++
                    delay(1000) // 每秒检查一次
                } catch (e: Exception) {
                    attempts++
                    delay(1000)
                }
            }

            // 轮询超时
            if (isPollingActive) {
                stopPolling()
                _uiState.value = _uiState.value.copy(
                    error = "支付超时，请确认支付结果或联系客服",
                    isPolling = false
                )
            }
        }
    }

    fun stopPolling() {
        isPollingActive = false
        _uiState.value = _uiState.value.copy(isPolling = false)
    }

    private suspend fun updateSubscribeUrlAfterPayment(onPaymentSuccess: () -> Unit) {
        try {
            // 自动导入和应用订阅
            autoSubscriptionManager.autoImportAndApply()
            delay(2000)
            onPaymentSuccess()
        } catch (e: Exception) {
            // 即使失败也继续返回
            delay(2000)
            onPaymentSuccess()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

