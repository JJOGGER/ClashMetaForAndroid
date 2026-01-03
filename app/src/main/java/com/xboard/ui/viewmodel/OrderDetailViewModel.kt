package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.event.OrderPayEvent
import com.xboard.model.CheckoutResponse
import com.xboard.model.OrderDetailResponse
import com.xboard.model.PaymentMethod
import com.xboard.network.OrderRepository
import com.xboard.network.UserRepository
import com.xboard.util.AutoSubscriptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * 订单详情 ViewModel
 */
class OrderDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepository = OrderRepository(RetrofitClient.getApiService())
    private val userRepository = UserRepository(RetrofitClient.getApiService())
    private val autoSubscriptionManager = AutoSubscriptionManager(userRepository, viewModelScope)

    data class OrderDetailUiState(
        val isLoading: Boolean = false,
        val isLoadingPayment: Boolean = false,
        val isPolling: Boolean = false,
        val isLoadingPaymentMethods: Boolean = false,
        val error: String? = null,
        val order: OrderDetailResponse? = null,
        val paymentMethods: List<PaymentMethod> = emptyList(),
        val selectedPaymentMethodId: Int? = null,
        val loadingMessage: String? = null
    )

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    /**
     * 加载订单详情
     */
    fun loadOrderDetail(tradeNo: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = orderRepository.getOrderDetail(tradeNo)
            result.onSuccess { order ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    order = order
                )
                // 如果是待支付状态，加载支付方式
                if (order.status == OrderDetailResponse.STATUS_WAITING) {
                    loadPaymentMethods()
                }
            }.onError { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "加载订单详情失败"
                )
            }
        }
    }

    /**
     * 加载支付方式列表
     */
    fun loadPaymentMethods() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPaymentMethods = true)
            try {
                val response = RetrofitClient.getApiService().getPaymentMethods()
                if (response.isSuccess() && response.data != null) {
                    val methods = response.data
                    val selectedId = methods.firstOrNull()?.id
                    _uiState.value = _uiState.value.copy(
                        isLoadingPaymentMethods = false,
                        paymentMethods = methods,
                        selectedPaymentMethodId = selectedId
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPaymentMethods = false,
                        error = "加载支付方式失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPaymentMethods = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 选择支付方式
     */
    fun selectPaymentMethod(methodId: Int) {
        _uiState.value = _uiState.value.copy(selectedPaymentMethodId = methodId)
    }

    /**
     * 发起支付
     */
    fun startPayment(tradeNo: String, onOpenBrowser: (String) -> Unit) {
        val selectedMethodId = _uiState.value.selectedPaymentMethodId
        if (selectedMethodId == null) {
            _uiState.value = _uiState.value.copy(error = "请选择支付方式")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingPayment = true,
                loadingMessage = "正在发起支付..."
            )
            val result = orderRepository.checkout(tradeNo, selectedMethodId)
            _uiState.value = _uiState.value.copy(isLoadingPayment = false, loadingMessage = null)

            result.onSuccess { checkoutData ->
                when (checkoutData?.type) {
                    1 -> {
                        // 支付链接，打开浏览器
                        try {
                            val url = checkoutData.data.toString()
                            onOpenBrowser(url)
                            startPollingPaymentStatus(tradeNo)
                        } catch (e: Exception) {
                            _uiState.value = _uiState.value.copy(error = "无法打开浏览器")
                        }
                    }
                    -1 -> {
                        // 免费订单，直接成功
                        updateSubscribeUrlAfterPayment()
                        loadOrderDetail(tradeNo)
                        EventBus.getDefault().post(OrderPayEvent())
                    }
                }
            }.onError { error ->
                // 处理错误码 99010（订单正在处理中）
                if (error.message?.contains("99010") == true) {
                    // 尝试使用缓存的 URL
                    val cachedUrl = com.xboard.storage.MMKVManager.getOrderCacheUrl()
                    if (!cachedUrl.isNullOrEmpty()) {
                        try {
                            onOpenBrowser(cachedUrl)
                            startPollingPaymentStatus(tradeNo)
                        } catch (e: Exception) {
                            _uiState.value = _uiState.value.copy(error = "无法打开浏览器")
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            }
        }
    }

    /**
     * 开始轮询支付状态
     */
    private var isPolling = false
    fun startPollingPaymentStatus(tradeNo: String) {
        if (isPolling) return
        isPolling = true
        _uiState.value = _uiState.value.copy(isPolling = true, loadingMessage = "支付中...")

        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 120 // 2分钟（每2秒检查一次）

            while (isPolling && attempts < maxAttempts) {
                try {
                    val result = orderRepository.checkOrderStatus(tradeNo)
                    result.onSuccess { orderStatus ->
                        when (orderStatus) {
                            1, 3, 4 -> {
                                // 已支付、已完成、已折抵
                                updateSubscribeUrlAfterPayment()
                                loadOrderDetail(tradeNo)
                                stopPolling()
                                EventBus.getDefault().post(OrderPayEvent())
                            }
                            -1 -> {
                                // 已取消
                                loadOrderDetail(tradeNo)
                                stopPolling()
                                EventBus.getDefault().post(OrderPayEvent())
                            }
                            0 -> {
                                // 待支付，继续轮询
                            }
                            else -> {
                                stopPolling()
                            }
                        }
                    }.onError {
                        // 继续轮询
                    }

                    attempts++
                    kotlinx.coroutines.delay(2000) // 每2秒检查一次
                } catch (e: Exception) {
                    attempts++
                    kotlinx.coroutines.delay(2000)
                }
            }

            // 轮询超时
            if (isPolling) {
                stopPolling()
            }
        }
    }

    /**
     * 停止轮询
     */
    fun stopPolling() {
        isPolling = false
        _uiState.value = _uiState.value.copy(
            isPolling = false,
            loadingMessage = null
        )
    }

    /**
     * 取消订单
     */
    fun cancelOrder(tradeNo: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadingMessage = "取消中...")
            val result = orderRepository.cancelOrder(tradeNo)
            _uiState.value = _uiState.value.copy(isLoading = false, loadingMessage = null)

            result.onSuccess {
                loadOrderDetail(tradeNo)
            }.onError { error ->
                _uiState.value = _uiState.value.copy(error = "取消订单失败: ${error.message}")
            }
        }
    }

    /**
     * 支付成功后自动导入和应用订阅
     */
    private fun updateSubscribeUrlAfterPayment() {
        viewModelScope.launch {
            try {
                autoSubscriptionManager.autoImportAndApply()
            } catch (e: Exception) {
                // 静默处理异常
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

