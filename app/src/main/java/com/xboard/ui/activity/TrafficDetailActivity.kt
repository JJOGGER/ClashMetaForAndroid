package com.xboard.ui.activity

import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.ActivityTrafficDetailBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.network.UserRepository
import com.xboard.ui.adapter.TrafficDetailAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
            // 先获取总体统计
            val statResult = userRepository.getUserStat()
            
            statResult
                .onSuccess { stat ->
                    // 再获取流量日志（按 api2.txt 的 /user/stat/getTrafficLog 接口）
                    val trafficLogResult = userRepository.getTrafficLog(currentPage)
                    
                    trafficLogResult
                        .onSuccess { trafficLogs ->
                            // 构建流量明细数据
                            val trafficDetails = mutableListOf<TrafficDetailItem>()

//                            // 添加总体统计
//                            trafficDetails.add(
//                                TrafficDetailItem(
//                                    date = "总体统计",
//                                    upload = stat[0],  // 未支付订单数
//                                    download = stat[1],  // 未关闭工单数
//                                    total = stat[0] + stat[1],
//                                    isHeader = true
//                                )
//                            )

                            // 添加日期明细（从 API 获取）
                            trafficLogs?.forEach { log ->
                                trafficDetails.add(
                                    TrafficDetailItem(
                                        date = log.record_at?.substring(0, 10) ?: "未知",
                                        upload = log.u ?: 0,
                                        download = log.d ?: 0,
                                        total = (log.u ?: 0) + (log.d ?: 0),
                                        isHeader = false
                                    )
                                )
                            }

                            if (trafficDetails.isEmpty()) {
                                binding.tvEmpty.visibility = View.VISIBLE
                                binding.rvTrafficDetails.visibility = View.GONE
                            } else {
                                binding.tvEmpty.visibility = View.GONE
                                binding.rvTrafficDetails.visibility = View.VISIBLE
                                trafficAdapter.updateData(trafficDetails)
                            }
                        }
                        .onError { error ->
                            binding.tvEmpty.visibility = View.VISIBLE
                            binding.tvEmpty.text = "加载失败: ${error.message}"
                            showError("加载流量日志失败: ${error.message}")
                        }
                }
                .onError { error ->
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "加载失败: ${error.message}"
                    showError("加载统计信息失败: ${error.message}")
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
