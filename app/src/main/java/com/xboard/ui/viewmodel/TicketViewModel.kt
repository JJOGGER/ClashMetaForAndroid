package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.event.TicketClosedEvent
import com.xboard.event.TicketCreatedEvent
import com.xboard.model.TicketResponse
import com.xboard.network.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 工单列表 ViewModel
 */
class TicketViewModel(application: Application) : AndroidViewModel(application) {

    private val ticketRepository = TicketRepository(RetrofitClient.getApiService())

    data class TicketUiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val tickets: List<TicketResponse> = emptyList(),
        val currentPage: Int = 1
    )

    private val _uiState = MutableStateFlow(TicketUiState())
    val uiState: StateFlow<TicketUiState> = _uiState.asStateFlow()

    init {
        // 注册EventBus监听工单关闭事件
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTicketClosed(event: TicketClosedEvent) {
        // 工单关闭后刷新列表
        refresh()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTicketCreated(event: TicketCreatedEvent) {
        // 工单创建后刷新列表
        refresh()
    }

    /**
     * 加载工单列表
     */
    fun loadTickets(refresh: Boolean = false) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            val page = if (refresh) 1 else _uiState.value.currentPage

            _uiState.value = _uiState.value.copy(
                isLoading = !refresh && _uiState.value.tickets.isEmpty(),
                isRefreshing = refresh,
                error = null
            )

            ticketRepository.getTickets(page = page, perPage = 20)
                .onSuccess { tickets ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        tickets = tickets ?: emptyList(),
                        currentPage = page,
                        error = null
                    )
                }
                .onError { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "加载工单列表失败"
                    )
                }
        }
    }

    /**
     * 刷新工单列表
     */
    fun refresh() {
        loadTickets(refresh = true)
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

