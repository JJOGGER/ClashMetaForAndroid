package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.event.TicketClosedEvent
import com.xboard.model.TicketResponse
import com.xboard.network.TicketRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * 工单详情 ViewModel
 */
class TicketDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val ticketRepository = TicketRepository(RetrofitClient.getApiService())

    data class TicketReply(
        val id: Int,
        val message: String,
        val createdAt: Long,
        val isAdmin: Boolean = false
    )

    data class TicketDetailUiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val ticket: TicketResponse? = null,
        val replies: List<TicketReply> = emptyList(),
        val isClosed: Boolean = false,
        val replyText: String = ""
    )

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var ticketId: Int = -1

    companion object {
        private const val REFRESH_INTERVAL = 10000L // 10秒刷新一次
    }

    /**
     * 初始化工单详情
     */
    fun initTicket(ticketId: Int, ticket: TicketResponse?) {
        this.ticketId = ticketId
        _uiState.value = _uiState.value.copy(
            ticket = ticket,
            isClosed = ticket?.status != 0
        )
        loadTicketDetail()
        startAutoRefresh()
    }

    /**
     * 加载工单详情和回复
     */
    private fun loadTicketDetail() {
        if (ticketId == -1) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            ticketRepository.getTicketDetail(ticketId)
                .onSuccess { ticket ->
                    val replies = buildRepliesList(ticket)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        ticket = ticket,
                        replies = replies,
                        isClosed = ticket.status != 0,
                        error = null
                    )
                    // 如果已关闭，停止自动刷新
                    if (ticket.status != 0) {
                        stopAutoRefresh()
                    }
                }
                .onError { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载工单详情失败"
                    )
                }
        }
    }

    /**
     * 构建回复列表
     */
    private fun buildRepliesList(ticket: TicketResponse): List<TicketReply> {
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

        return replies
    }

    /**
     * 启动定时刷新
     */
    private fun startAutoRefresh() {
        stopAutoRefresh() // 先停止之前的刷新任务
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL)
                if (isActive && !_uiState.value.isClosed && ticketId != -1) {
                    ticketRepository.getTicketDetail(ticketId)
                        .onSuccess { ticket ->
                            val replies = buildRepliesList(ticket)
                            _uiState.value = _uiState.value.copy(
                                ticket = ticket,
                                replies = replies,
                                isClosed = ticket.status != 0
                            )
                            if (ticket.status != 0) {
                                stopAutoRefresh()
                            }
                        }
                        .onError {
                            // 定时刷新时不更新错误状态
                        }
                }
            }
        }
    }

    /**
     * 停止定时刷新
     */
    fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    /**
     * 回复工单
     */
    fun replyTicket(message: String) {
        if (message.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入回复内容")
            return
        }

        if (ticketId == -1) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            ticketRepository.replyTicket(ticketId = ticketId, message = message)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        replyText = "",
                        error = null
                    )
                    // 立即刷新获取最新回复
                    loadTicketDetail()
                }
                .onError { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "回复失败"
                    )
                }
        }
    }

    /**
     * 关闭工单
     */
    fun closeTicket() {
        if (ticketId == -1) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            ticketRepository.closeTicket(ticketId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isClosed = true,
                        error = null
                    )
                    stopAutoRefresh()
                    // 刷新工单详情
                    loadTicketDetail()
                    // 发送工单关闭事件，通知其他页面刷新
                    EventBus.getDefault().post(TicketClosedEvent())
                }
                .onError { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "关闭失败"
                    )
                }
        }
    }

    /**
     * 更新回复文本
     */
    fun updateReplyText(text: String) {
        _uiState.value = _uiState.value.copy(replyText = text)
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

