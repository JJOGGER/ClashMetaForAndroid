package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.model.OrderDetailResponse
import com.xboard.network.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrderHistoryUiState(
    val orders: List<OrderDetailResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1
)

class OrderHistoryViewModel(
    application: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(OrderHistoryUiState())
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()
    
    fun loadOrderHistory(refresh: Boolean = false) {
        viewModelScope.launch {
            val page = if (refresh) 1 else _uiState.value.currentPage
            
            _uiState.value = _uiState.value.copy(
                isLoading = !refresh && _uiState.value.orders.isEmpty(),
                isRefreshing = refresh,
                errorMessage = null
            )
            
            userRepository.getOrderHistory(page = page, perPage = 20)
                .onSuccess { orders ->
                    _uiState.value = _uiState.value.copy(
                        orders = if (refresh) orders else _uiState.value.orders + orders,
                        isLoading = false,
                        isRefreshing = false,
                        currentPage = page,
                        errorMessage = null
                    )
                }
                .onError { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    fun refresh() {
        loadOrderHistory(refresh = true)
    }
}

