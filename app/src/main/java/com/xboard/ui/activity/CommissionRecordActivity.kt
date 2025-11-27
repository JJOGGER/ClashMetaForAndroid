package com.xboard.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.ActivityCommissionRecordBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.network.InviteRepository
import com.xboard.ui.adapter.CommissionRecordAdapter
import kotlinx.coroutines.launch
import java.text.DecimalFormat

/**
 * 佣金发放记录列表页
 */
class CommissionRecordActivity : BaseActivity<ActivityCommissionRecordBinding>() {

    private val inviteRepository by lazy { InviteRepository(RetrofitClient.getApiService()) }
    private lateinit var adapter: CommissionRecordAdapter
    private val priceFormatter = DecimalFormat("#.##")
    private var currentPage = 1
    private var pageSize = 20
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
        initListener()
        loadData()
    }

    override fun getViewBinding(): ActivityCommissionRecordBinding {
        return ActivityCommissionRecordBinding.inflate(layoutInflater)

    }

    override fun initView() {
        // 设置返回按钮
        binding.vBack.setOnClickListener {
            finish()
        }

        // 设置列表
        adapter = CommissionRecordAdapter()
        binding.rvCommissionRecord.apply {
            layoutManager = LinearLayoutManager(this@CommissionRecordActivity)
            adapter = this@CommissionRecordActivity.adapter
        }

        // 设置下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            currentPage = 1
            loadData()
        }
    }

    override fun initListener() {
        // 监听列表滚动，实现分页加载
        binding.rvCommissionRecord.addOnScrollListener(
            object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoading && visibleItemCount + firstVisibleItemPosition >= totalItemCount - 5) {
                        // 接近底部，加载下一页
                        loadMoreData()
                    }
                }
            }
        )
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        isLoading = true

        lifecycleScope.launch {
            val result = inviteRepository.getInviteDetails(current = currentPage, pageSize = pageSize)

            result
                .onSuccess { details ->
                    if (currentPage == 1) {
                        // 首次加载，替换数据
                        details?.data?.let { adapter.updateData(it) }
                    } else {
                        // 分页加载，追加数据
                        details?.data?.let { adapter.appendData(it) }
                    }

                    // 更新总数显示
                    binding.tvTotalCount.text = "共 ${details?.total} 条记录"
                }
                .onError { error ->
                    showError(error.message ?: "加载失败")
                }

            binding.swipeRefresh.isRefreshing = false
            isLoading = false
        }
    }

    private fun loadMoreData() {
        if (isLoading) return

        currentPage++
        isLoading = true

        lifecycleScope.launch {
            val result = inviteRepository.getInviteDetails(current = currentPage, pageSize = pageSize)

            result
                .onSuccess { details ->
                    details?.data?.let { adapter.appendData(it) }
                }
                .onError { error ->
                    currentPage-- // 恢复页码
                    showError(error.message ?: "加载失败")
                }

            isLoading = false
        }
    }

}
