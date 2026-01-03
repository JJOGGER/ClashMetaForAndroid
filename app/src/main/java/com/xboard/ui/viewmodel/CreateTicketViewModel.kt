package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.event.TicketCreatedEvent
import com.xboard.network.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * 创建工单 ViewModel
 */
class CreateTicketViewModel(application: Application) : AndroidViewModel(application) {

    private val ticketRepository = TicketRepository(RetrofitClient.getApiService())

    data class CreateTicketUiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val subject: String = "",
        val description: String = ""
    )

    private val _uiState = MutableStateFlow(CreateTicketUiState())
    val uiState: StateFlow<CreateTicketUiState> = _uiState.asStateFlow()

    /**
     * 更新主题
     */
    fun updateSubject(subject: String) {
        _uiState.value = _uiState.value.copy(subject = subject)
    }

    /**
     * 更新描述
     */
    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    /**
     * 提交工单
     */
    fun submitTicket(onSuccess: () -> Unit) {
        val subject = _uiState.value.subject.trim()
        val description = _uiState.value.description.trim()

        if (subject.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "请输入问题标题")
            return
        }

        if (description.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "请输入问题描述")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            ticketRepository.createTicket(subject = subject, description = description)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // 发送工单创建事件，通知其他页面刷新
                    EventBus.getDefault().post(TicketCreatedEvent())
                    onSuccess()
                }
                .onError { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "提交失败"
                    )
                }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

