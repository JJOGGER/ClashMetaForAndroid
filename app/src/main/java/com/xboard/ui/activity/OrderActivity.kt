package com.xboard.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseComposeActivity
import com.xboard.model.Plan
import com.xboard.network.OrderRepository
import com.xboard.network.PlanRepository
import com.xboard.ui.compose.OrderScreen
import com.xboard.ui.viewmodel.OrderViewModel

/**
 * 订单创建和支付页面
 * 已重构为 Compose 实现
 */
class OrderActivity : BaseComposeActivity() {

    companion object {
        const val EXTRA_PLAN = "plan"
        private const val REQUEST_CODE_ORDER_DETAIL = 1001
    }

    private val orderRepository by lazy { OrderRepository(RetrofitClient.getApiService()) }
    private val planRepository by lazy { PlanRepository(RetrofitClient.getApiService()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val plan = intent.getSerializableExtra(EXTRA_PLAN) as? Plan
        if (plan == null) {
            finish()
            return
        }

        setThemeContent {
            val viewModel: OrderViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return OrderViewModel(
                            application,
                            orderRepository,
                            planRepository
                        ) as T
                    }
                }
            )

            OrderScreen(
                viewModel = viewModel,
                plan = plan,
                onNavigateBack = { finish() },
                onNavigateToOrderDetail = { tradeNo ->
                    val intent = Intent(this@OrderActivity, OrderDetailActivity::class.java)
                    intent.putExtra(PaymentActivity.EXTRA_TRADE_NO, tradeNo)
                    startActivityForResult(intent, REQUEST_CODE_ORDER_DETAIL)
                }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ORDER_DETAIL && resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }
}
