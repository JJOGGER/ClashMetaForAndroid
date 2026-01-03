package com.xboard.ui.activity

import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.base.BaseComposeActivity
import com.xboard.ui.compose.OrderDetailScreen
import com.xboard.ui.viewmodel.OrderDetailViewModel

/**
 * 订单详情页面
 * 如果是未支付订单，进入PaymentActivity
 * 如果是取消和已完成的，展示商品信息和订单信息
 */
class OrderDetailActivity : BaseComposeActivity() {

    companion object {
        const val EXTRA_TRADE_NO = "trade_no"
        const val EXTRA_ORDER_STATUS = "order_status"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tradeNo = intent.getStringExtra(EXTRA_TRADE_NO)
        if (tradeNo.isNullOrEmpty()) {
            finish()
            return
        }

        setThemeContent {
            val viewModel: OrderDetailViewModel = viewModel()
            OrderDetailScreen(
                tradeNo = tradeNo,
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

