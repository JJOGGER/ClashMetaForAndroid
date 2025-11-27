package com.xboard.ui.activity

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.databinding.ActivityTicketDetailBinding
import com.xboard.api.RetrofitClient
import com.xboard.base.BaseActivity
import com.xboard.ex.gone
import com.xboard.ex.showToast
import com.xboard.ex.visible
import com.xboard.model.TicketResponse
import com.xboard.network.TicketRepository
import com.xboard.ui.adapter.TicketReply
import com.xboard.ui.adapter.TicketReplyAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TicketDetailActivity : BaseActivity<ActivityTicketDetailBinding>() {

    companion object {
        private const val TAG = "TicketDetailActivity"
        private const val REFRESH_INTERVAL = 10000L // 3秒刷新一次
    }

    private val ticketRepository by lazy { TicketRepository(RetrofitClient.getApiService()) }
    private lateinit var replyAdapter: TicketReplyAdapter
    private var ticketId: Int = -1
    private var ticket: TicketResponse? = null
    private var isClosed = false
    private var refreshJob: Job? = null

    override fun getViewBinding(): ActivityTicketDetailBinding {
        return ActivityTicketDetailBinding.inflate(layoutInflater)
    }

    override fun initView() {
        ticketId = intent.getIntExtra("ticketId", -1)
        ticket = intent.getSerializableExtra("ticket") as? TicketResponse? ?: return

        if (ticketId == -1) {
            finish()
            return
        }

        setupReplyAdapter()
        setupBackButton()

        binding.btnReply.setOnClickListener {
            replyTicket()
        }

        binding.btnClose.setOnClickListener {
            closeTicket()
        }
    }

    override fun initData() {
        loadTicketDetail()
        loadReplies()
        startAutoRefresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoRefresh()
    }

    private fun setupBackButton() {
        binding.vBack.setOnClickListener {
            finish()
        }
    }

    private fun setupReplyAdapter() {
        replyAdapter = TicketReplyAdapter()
        binding.rvReplies.adapter = replyAdapter
        binding.rvReplies.layoutManager = LinearLayoutManager(this)
    }

    private fun loadTicketDetail() {
        ticket?.let {
            binding.tvTicketStatus.text = if (it.status == 0) "处理中" else "已关闭"
            isClosed = it.status != 0
            updateReplyInputState()
        }
    }

    /**
     * 启动定时刷新
     */
    private fun startAutoRefresh() {
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL)
                if (isActive && !isClosed) {
                    loadReplies()
                }
            }
        }
    }

    /**
     * 停止定时刷新
     */
    private fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    private fun loadReplies() {
        lifecycleScope.launch {
            try {
                val result = ticketRepository.getTicketDetail(ticketId)
                result.onSuccess { ticket ->
                    this@TicketDetailActivity.ticket = ticket
                    isClosed = ticket.status != 0
                    updateReplyInputState()

                    // 构建回复列表
                    val replies = mutableListOf<TicketReply>()

                    // 添加消息列表（新格式）
                    ticket.message?.forEach { msg ->
                        replies.add(
                            TicketReply(
                                id = msg.id,
                                message = msg.message,
                                createdAt = msg.createdAt ?: System.currentTimeMillis() / 1000,
                                isAdmin = !msg.isMe
                            )
                        )
                    }

                    // 兼容旧格式：添加回复消息
                    ticket.replies?.forEach { reply ->
                        replies.add(
                            TicketReply(
                                id = reply.id,
                                message = reply.message,
                                createdAt = reply.createdAt ?: System.currentTimeMillis() / 1000,
                                isAdmin = reply.isAdmin == 1
                            )
                        )
                    }

                    replyAdapter.submitList(replies)
                    binding.rvReplies.scrollToPosition(replies.size - 1)
                }.onError { error ->
                    // 定时刷新时不显示错误提示
                }
            } catch (e: Exception) {
                // 定时刷新时不显示错误提示
            }
        }
    }

    private fun updateReplyInputState() {
        if (isClosed) {
            binding.llReplyContainer.gone()
            binding.llClosedTip.visible()
        } else {
            binding.llReplyContainer.visible()
            binding.llClosedTip.gone()
        }
    }

    private fun replyTicket() {
        val message = binding.etReply.text.toString().trim()

        if (message.isEmpty()) {
            showToast("请输入回复内容")
            return
        }

        lifecycleScope.launch {
            try {
                val result = ticketRepository.replyTicket(
                    ticketId = ticketId,
                    message = message
                )
                result.onSuccess {
                    showToast("回复成功")
                    binding.etReply.text.clear()
                    // 立即刷新获取最新回复
                    loadReplies()
                }.onError { error ->
                    showToast("回复失败: ${error.message}")
                }
            } catch (e: Exception) {
                showToast("回复失败: ${e.message}")
            }
        }
    }

    private fun closeTicket() {
        lifecycleScope.launch {
            try {
                val result = ticketRepository.closeTicket(ticketId)
                result.onSuccess {
                    showToast("工单已关闭")
                    isClosed = true
                    updateReplyInputState()
                }.onError { error ->
                    showToast("关闭失败: ${error.message}")
                }
            } catch (e: Exception) {
                showToast("关闭失败: ${e.message}")
            }
        }
    }
}
