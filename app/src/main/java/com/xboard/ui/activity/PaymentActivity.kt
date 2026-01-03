package com.xboard.ui.activity

import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseComposeActivity
import com.xboard.network.OrderRepository
import com.xboard.network.UserRepository
import com.xboard.ui.compose.PaymentScreen
import com.xboard.ui.viewmodel.PaymentViewModel

/**
 * 支付页面
 * 已重构为 Compose 实现
 */
class PaymentActivity : BaseComposeActivity() {

    companion object {
        const val EXTRA_TRADE_NO = "trade_no"
        const val EXTRA_AMOUNT = "amount"
    }

    private val orderRepository by lazy { OrderRepository(RetrofitClient.getApiService()) }
    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tradeNo = intent.getStringExtra(EXTRA_TRADE_NO) ?: ""
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0).takeIf { it > 0 }

        setThemeContent {
            val viewModel: PaymentViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return PaymentViewModel(
                            application,
                            orderRepository,
                            userRepository
                        ) as T
                    }
                }
            )

            PaymentScreen(
                viewModel = viewModel,
                tradeNo = tradeNo,
                amount = amount,
                onNavigateBack = { finish() },
                onPaymentSuccess = {
                    setResult(RESULT_OK)
                    finish()
                }
            )
        }
    }
}
