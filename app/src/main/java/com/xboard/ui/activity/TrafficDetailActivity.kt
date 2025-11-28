package com.xboard.ui.activity

import android.R.attr.x
import android.system.Os.stat
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.ActivityTrafficDetailBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.network.UserRepository
import com.xboard.ui.adapter.TrafficDetailAdapter
import kotlinx.coroutines.launch

/**
 * 流量明细页面
 */
class TrafficDetailActivity : BaseActivity<ActivityTrafficDetailBinding>() {

    private val userRepository by lazy { UserRepository(RetrofitClient.getApiService()) }
    private lateinit var trafficAdapter: TrafficDetailAdapter
    private var currentPage = 1
    private var isLoading = false

    override fun getViewBinding(): ActivityTrafficDetailBinding {
        return ActivityTrafficDetailBinding.inflate(layoutInflater)
    }

    override fun initView() {
        setupUI()
    }

    override fun initData() {
        loadTrafficDetails()
    }

    private fun setupUI() {
        // 返回按钮
        binding.vBack.setOnClickListener {
            finish()
        }

        // 流量明细列表
        trafficAdapter = TrafficDetailAdapter()

        binding.rvTrafficDetails.apply {
            layoutManager = LinearLayoutManager(this@TrafficDetailActivity)
            adapter = trafficAdapter
        }

        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            currentPage = 1
            loadTrafficDetails()
        }
    }

    private fun loadTrafficDetails() {
        if (isLoading) return
        isLoading = true
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            val trafficLogResult = userRepository.getTrafficLog(currentPage)

            trafficLogResult
                .onSuccess { trafficLogs ->
                    if (trafficLogs.isNullOrEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvTrafficDetails.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvTrafficDetails.visibility = View.VISIBLE
                        trafficAdapter.updateData(trafficLogs)
                    }
                }
                .onError { error ->
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "加载失败: ${error.message}"
                    showError("加载流量日志失败: ${error.message}")
                }

            binding.swipeRefresh.isRefreshing = false
            isLoading = false
        }
    }
}

data class TrafficDetailItem(
    val date: String,
    val upload: Long,
    val download: Long,
    val total: Long,
    val isHeader: Boolean = false
)
