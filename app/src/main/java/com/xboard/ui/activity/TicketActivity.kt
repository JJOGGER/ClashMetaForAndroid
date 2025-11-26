package com.xboard.ui.activity

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.ActivityTicketBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.network.TicketRepository
import com.xboard.ui.adapter.TicketAdapter
import kotlinx.coroutines.launch

class TicketActivity : BaseActivity<ActivityTicketBinding>() {

    companion object {
        private const val TAG = "TicketActivity"
    }

    private val ticketRepository by lazy { TicketRepository(RetrofitClient.getApiService()) }
    private lateinit var ticketAdapter: TicketAdapter
    private var currentPage = 1

    override fun getViewBinding(): ActivityTicketBinding {
        return ActivityTicketBinding.inflate(layoutInflater)
    }

    override fun initView() {
        setupAdapter()
        setupRefresh()

        binding.btnCreateTicket.setOnClickListener {
            startActivity(Intent(this, CreateTicketActivity::class.java))
        }
    }

    override fun initData() {
        loadTickets()
    }

    private fun setupAdapter() {
        ticketAdapter = TicketAdapter { ticket ->
            // 点击工单项，进入工单详情
            val intent = Intent(this, TicketDetailActivity::class.java)
            intent.putExtra("ticketId", ticket.id)
            intent.putExtra("ticket", ticket)
            startActivity(intent)
        }
        binding.rvTickets.adapter = ticketAdapter
        binding.rvTickets.layoutManager = LinearLayoutManager(this)
    }

    private fun loadTickets() {
        lifecycleScope.launch {
            try {
                val result = ticketRepository.getTickets(page = currentPage, perPage = 20)
                result.onSuccess { tickets ->
                    ticketAdapter.submitList(tickets)
                    binding.swipeRefresh.isRefreshing = false
                }.onError { error ->
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            currentPage = 1
            loadTickets()
        }
    }
}
