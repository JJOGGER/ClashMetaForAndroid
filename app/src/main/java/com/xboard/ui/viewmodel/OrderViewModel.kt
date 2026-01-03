package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.model.CouponResponse
import com.xboard.model.Plan
import com.xboard.network.OrderRepository
import com.xboard.network.PlanRepository
import com.xboard.storage.MMKVManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PeriodOption(
    val periodKey: String,
    val label: String,
    val price: Double
)

data class OrderUiState(
    val plan: Plan? = null,
    val periodOptions: List<PeriodOption> = emptyList(),
    val selectedPeriod: String? = null,
    val couponCode: String = "",
    val coupon: CouponResponse? = null,
    val isLoading: Boolean = false,
    val isLoadingCoupon: Boolean = false,
    val isLoadingOrder: Boolean = false,
    val error: String? = null,
    val shouldShowSubscriptionWarning: Boolean = false
)

class OrderViewModel(
    application: Application,
    private val orderRepository: OrderRepository,
    private val planRepository: PlanRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    fun setPlan(plan: Plan) {
        val periodOptions = buildPeriodOptions(plan)
        val initialPeriod = periodOptions.firstOrNull()?.periodKey

        _uiState.value = _uiState.value.copy(
            plan = plan,
            periodOptions = periodOptions,
            selectedPeriod = initialPeriod
        )
    }

    fun selectPeriod(periodKey: String) {
        _uiState.value = _uiState.value.copy(selectedPeriod = periodKey)
    }

    fun setCouponCode(code: String) {
        _uiState.value = _uiState.value.copy(couponCode = code)
    }

    fun checkCoupon(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val plan = _uiState.value.plan
        val period = _uiState.value.selectedPeriod
        val couponCode = _uiState.value.couponCode.trim()

        if (couponCode.isEmpty()) {
            onError("请输入优惠券代码")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCoupon = true, error = null)
            val result = planRepository.checkCoupon(couponCode, plan?.id, period)
            _uiState.value = _uiState.value.copy(isLoadingCoupon = false)

            result.onSuccess { coupon ->
                _uiState.value = _uiState.value.copy(coupon = coupon)
                onSuccess()
            }.onError { error ->
                val errorMessage = error.message ?: "优惠券无效"
                _uiState.value = _uiState.value.copy(error = errorMessage)
                onError(errorMessage)
            }
        }
    }

    fun createOrder(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val plan = _uiState.value.plan
        val period = _uiState.value.selectedPeriod

        if (plan == null || period == null) {
            onError("请选择订阅周期")
            return
        }

        // 检查是否需要显示订阅覆盖警告
        val subscription = MMKVManager.getSubscribe()
        val shouldWarn = subscription != null && subscription.planId != null && subscription.planId > 0

        if (shouldWarn) {
            _uiState.value = _uiState.value.copy(shouldShowSubscriptionWarning = true)
            return
        }

        performCreateOrder(onSuccess, onError)
    }

    fun confirmCreateOrder(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        _uiState.value = _uiState.value.copy(shouldShowSubscriptionWarning = false)
        performCreateOrder(onSuccess, onError)
    }

    private fun performCreateOrder(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val plan = _uiState.value.plan ?: return
        val period = _uiState.value.selectedPeriod ?: return
        val couponCode = _uiState.value.couponCode.trim().takeIf { it.isNotEmpty() }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingOrder = true, error = null)
            val result = orderRepository.createOrder(
                planId = plan.id,
                period = period,
                couponCode = couponCode
            )
            _uiState.value = _uiState.value.copy(isLoadingOrder = false)

            result.onSuccess { tradeNo ->
                onSuccess(tradeNo ?: "")
            }.onError { error ->
                val errorMessage = error.message ?: "创建订单失败"
                _uiState.value = _uiState.value.copy(error = errorMessage)
                onError(errorMessage)
            }
        }
    }

    fun calculateTotalPrice(): Double {
        val plan = _uiState.value.plan ?: return 0.0
        val period = _uiState.value.selectedPeriod ?: return 0.0
        val couponDiscount = _uiState.value.coupon?.value ?: 0.0
        return plan.getRealPlanPrice(period) - couponDiscount
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun dismissSubscriptionWarning() {
        _uiState.value = _uiState.value.copy(shouldShowSubscriptionWarning = false)
    }

    private fun buildPeriodOptions(plan: Plan): List<PeriodOption> {
        val options = mutableListOf<PeriodOption>()
        
        if (plan.monthPrice != null && plan.monthPrice != 0.0) {
            options.add(PeriodOption("month_price", "月付", plan.monthPrice!!))
        }
        if (plan.quarterPrice != null && plan.quarterPrice != 0.0) {
            options.add(PeriodOption("quarter_price", "季付", plan.quarterPrice!!))
        }
        if (plan.halfYearPrice != null && plan.halfYearPrice != 0.0) {
            options.add(PeriodOption("half_year_price", "半年付", plan.halfYearPrice!!))
        }
        if (plan.yearPrice != null && plan.yearPrice != 0.0) {
            options.add(PeriodOption("year_price", "年付", plan.yearPrice!!))
        }
        if (plan.twoYearPrice != null && plan.twoYearPrice != 0.0) {
            options.add(PeriodOption("two_year_price", "两年付", plan.twoYearPrice!!))
        }
        if (plan.threeYearPrice != null && plan.threeYearPrice != 0.0) {
            options.add(PeriodOption("three_year_price", "三年付", plan.threeYearPrice!!))
        }
        
        if (options.isEmpty()) {
            options.add(PeriodOption("onetime_price", "一次性", plan.onetimePrice ?: 0.0))
        }
        
        return options
    }
}

