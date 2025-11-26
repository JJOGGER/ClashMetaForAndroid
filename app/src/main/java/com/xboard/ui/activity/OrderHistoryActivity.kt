package com.xboard.ui.activity

import android.content.Intent
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.ActivityOrderHistoryBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.network.UserRepository
import com.xboard.ui.adapter.OrderHistoryAdapter
import kotlinx.coroutines.launch

/**
 * 订单历史列表页面
 */
class OrderHistoryActivity : BaseActivity<ActivityOrderHistoryBinding>() {

    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    private lateinit var orderAdapter: OrderHistoryAdapter
    private var currentPage = 1
    private var isLoading = false

    override fun getViewBinding(): ActivityOrderHistoryBinding {
        return ActivityOrderHistoryBinding.inflate(layoutInflater)
    }

    override fun initView() {
        setupUI()
    }

    override fun initData() {
        loadOrderHistory()
    }

    private fun setupUI() {
        binding.vBack.setOnClickListener {
            finish()
        }

        // 订单列表
        orderAdapter = OrderHistoryAdapter { order ->
            // 点击订单项，进入订单详情页
            val intent = Intent(this, OrderDetailActivity::class.java)
            intent.putExtra(OrderDetailActivity.EXTRA_TRADE_NO, order.tradeNo)
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_STATUS, order.status)
            startActivity(intent)
        }

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(this@OrderHistoryActivity)
            adapter = orderAdapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            currentPage = 1
            loadOrderHistory()
        }
    }

    private fun loadOrderHistory() {
        if (isLoading) return
        isLoading = true
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            val result = userRepository.getOrderHistory(page = currentPage, perPage = 20)

            result
                .onSuccess { orders ->
                    if (orders.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvOrders.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvOrders.visibility = View.VISIBLE
                        orderAdapter.updateData(orders)
                    }
                }
                .onError { error ->
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "加载失败: ${error.message}"
                    showError("加载订单失败: ${error.message}")
                }

            binding.swipeRefresh.isRefreshing = false
            isLoading = false
        }
    }
}
