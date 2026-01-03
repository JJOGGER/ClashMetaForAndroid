package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.api.RetrofitClient
import com.xboard.model.InviteDetail
import com.xboard.network.InviteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 佣金发放记录页面 ViewModel
 */
class CommissionRecordViewModel(application: Application) : AndroidViewModel(application) {
    
    private val inviteRepository = InviteRepository(RetrofitClient.getApiService())
    
    private val _uiState = MutableStateFlow(CommissionRecordUiState())
    val uiState: StateFlow<CommissionRecordUiState> = _uiState.asStateFlow()
    
    private var currentPage = 1
    private val pageSize = 20
    private var isLoadingMore = false
    
    /**
     * UI 状态
     */
    data class CommissionRecordUiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val records: List<InviteDetail> = emptyList(),
        val totalCount: Int = 0,
        val hasMore: Boolean = true
    )
    
    init {
        loadData()
    }
    
    /**
     * 加载数据
     */
    fun loadData() {
        viewModelScope.launch {
            currentPage = 1
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                hasMore = true
            )
            
            val result = inviteRepository.getInviteDetails(current = currentPage, pageSize = pageSize)
            
            result.onSuccess { response ->
                val data = response?.data ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    records = data,
                    totalCount = response?.total ?: 0,
                    hasMore = data.size >= pageSize
                )
                currentPage = 1
            }.onError { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "加载失败"
                )
            }
        }
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        viewModelScope.launch {
            currentPage = 1
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                error = null,
                hasMore = true
            )
            
            val result = inviteRepository.getInviteDetails(current = currentPage, pageSize = pageSize)
            
            result.onSuccess { response ->
                val data = response?.data ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    records = data,
                    totalCount = response?.total ?: 0,
                    hasMore = data.size >= pageSize
                )
                currentPage = 1
            }.onError { error ->
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = error.message ?: "刷新失败"
                )
            }
        }
    }
    
    /**
     * 加载更多
     */
    fun loadMore() {
        if (isLoadingMore || !_uiState.value.hasMore) return
        
        viewModelScope.launch {
            isLoadingMore = true
            currentPage++
            
            val result = inviteRepository.getInviteDetails(current = currentPage, pageSize = pageSize)
            
            result.onSuccess { response ->
                val data = response?.data ?: emptyList()
                val currentRecords = _uiState.value.records
                _uiState.value = _uiState.value.copy(
                    records = currentRecords + data,
                    hasMore = data.size >= pageSize
                )
                isLoadingMore = false
            }.onError { error ->
                currentPage-- // 恢复页码
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "加载失败"
                )
                isLoadingMore = false
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

