package com.example.masterdashboard.manager_single_res_dash.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.repo.RestaurantDetailsRepository
import com.example.masterdashboard.manager_single_res_dash.uistate.RestaurantDetailsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RestaurantDetailsViewModel : ViewModel() {

    private val repository = RestaurantDetailsRepository()

    private val _uiState = MutableStateFlow<RestaurantDetailsUiState>(RestaurantDetailsUiState.Loading)
    val uiState: StateFlow<RestaurantDetailsUiState> = _uiState.asStateFlow()

    /**
     * Fetches restaurant details using the repository and updates the UI state.
     */
    fun fetchRestaurantDetails(restaurantId: String) {
        viewModelScope.launch {
            Log.d("ResDetailsVM", "Action: Fetching restaurant details for ID: $restaurantId")
            _uiState.value = RestaurantDetailsUiState.Loading
            
            val result = repository.getRestaurantDetails(restaurantId)
            
            result.onSuccess { details ->
                if (details != null) {
                    _uiState.value = RestaurantDetailsUiState.Success(details)
                } else {
                    _uiState.value = RestaurantDetailsUiState.Error("No data found")
                }
            }.onFailure { e ->
                _uiState.value = RestaurantDetailsUiState.Error(e.message ?: "Failed to fetch data")
            }
        }
    }
}
