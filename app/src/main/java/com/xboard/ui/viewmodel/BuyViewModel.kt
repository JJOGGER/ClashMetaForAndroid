package com.xboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.model.Plan
import com.xboard.network.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BuyUiState(
    val plans: List<Plan> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedPlan: Plan? = null
)

class BuyViewModel(application: Application) : AndroidViewModel(application) {
    
    private val planRepository by lazy { 
        PlanRepository(com.xboard.api.RetrofitClient.getApiService()) 
    }
    
    private val _uiState = MutableStateFlow(BuyUiState())
    val uiState: StateFlow<BuyUiState> = _uiState.asStateFlow()
    
    init {
        loadPlans()
    }
    
    fun loadPlans() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            planRepository.getAllPlans()
                .onSuccess { plans ->
                    _uiState.value = _uiState.value.copy(
                        plans = plans,
                        isLoading = false
                    )
                }
                .onError { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    fun selectPlan(plan: Plan) {
        _uiState.value = _uiState.value.copy(selectedPlan = plan)
    }
    
    fun clearSelectedPlan() {
        _uiState.value = _uiState.value.copy(selectedPlan = null)
    }
}

