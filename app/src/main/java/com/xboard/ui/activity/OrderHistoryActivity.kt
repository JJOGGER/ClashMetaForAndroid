package com.xboard.ui.activity

import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseComposeActivity
import com.xboard.network.UserRepository
import com.xboard.ui.compose.OrderHistoryScreen
import com.xboard.ui.viewmodel.OrderHistoryViewModel

/**
 * 订单历史列表页面
 * 已重构为 Compose 实现
 */
class OrderHistoryActivity : BaseComposeActivity() {

    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setThemeContent {
            val viewModel: OrderHistoryViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return OrderHistoryViewModel(
                            application,
                            userRepository
                        ) as T
                    }
                }
            )
            
            OrderHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}
