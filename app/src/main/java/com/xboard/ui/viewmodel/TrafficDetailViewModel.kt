package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.model.TrafficLog
import com.xboard.network.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 流量明细 ViewModel
 */
class TrafficDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(RetrofitClient.getApiService())

    data class TrafficDetailUiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val trafficLogs: List<TrafficLog> = emptyList(),
        val currentPage: Int = 1
    )

    private val _uiState = MutableStateFlow(TrafficDetailUiState())
    val uiState: StateFlow<TrafficDetailUiState> = _uiState.asStateFlow()

    /**
     * 加载流量明细
     */
    fun loadTrafficDetails(refresh: Boolean = false) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            val page = if (refresh) 1 else _uiState.value.currentPage

            _uiState.value = _uiState.value.copy(
                isLoading = !refresh && _uiState.value.trafficLogs.isEmpty(),
                isRefreshing = refresh,
                error = null
            )

            userRepository.getTrafficLog(page = page, perPage = 20)
                .onSuccess { logs ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        trafficLogs = if (refresh) {
                            logs ?: emptyList()
                        } else {
                            _uiState.value.trafficLogs + (logs ?: emptyList())
                        },
                        currentPage = page,
                        error = null
                    )
                }
                .onError { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "加载流量明细失败"
                    )
                }
        }
    }

    /**
     * 刷新流量明细
     */
    fun refresh() {
        loadTrafficDetails(refresh = true)
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

