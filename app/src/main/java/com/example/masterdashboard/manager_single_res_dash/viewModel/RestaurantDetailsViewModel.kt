package com.example.masterdashboard.manager_single_res_dash.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.repo.RestaurantDetailsRepository
import com.example.masterdashboard.manager_single_res_dash.uistate.RestaurantDetailsUiState
import com.example.masterdashboard.utils.SessionManager
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
     * Implements "Fast-Open" by checking local cache first.
     */
    fun fetchRestaurantDetails(restaurantId: String, sessionManager: SessionManager) {
        // 1. FAST-OPEN: Try to load from cache immediately
        val cachedData = sessionManager.getCachedRestaurantDetails()
        if (cachedData != null) {
            Log.d("ResDetailsVM", "Fast-Open: Emitting cached restaurant details")
            _uiState.value = RestaurantDetailsUiState.Success(cachedData)
        } else {
            _uiState.value = RestaurantDetailsUiState.Loading
        }

        // 2. BACKGROUND REFRESH: Fetch from Firestore
        viewModelScope.launch {
            Log.d("ResDetailsVM", "Background Refresh: Fetching latest details for ID: $restaurantId")
            
            val result = repository.getRestaurantDetails(restaurantId)
            
            result.onSuccess { details ->
                if (details != null) {
                    // Update cache and state
                    sessionManager.saveRestaurantDetails(details)
                    _uiState.value = RestaurantDetailsUiState.Success(details)
                } else if (_uiState.value !is RestaurantDetailsUiState.Success) {
                    _uiState.value = RestaurantDetailsUiState.Error("No data found")
                }
            }.onFailure { e ->
                if (_uiState.value !is RestaurantDetailsUiState.Success) {
                    _uiState.value = RestaurantDetailsUiState.Error(e.message ?: "Failed to fetch data")
                }
                Log.e("ResDetailsVM", "Background fetch failed", e)
            }
        }
    }
}
