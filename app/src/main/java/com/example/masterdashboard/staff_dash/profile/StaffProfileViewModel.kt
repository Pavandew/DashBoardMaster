package com.example.masterdashboard.staff_dash.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StaffProfileViewModel(
    private val repository: StaffProfileRepository
) : ViewModel() {

    companion object {
        private const val TAG = "StaffProfile_Debug"
    }

    private val _uiState = MutableStateFlow<StaffProfileUiState>(StaffProfileUiState.Loading)
    val uiState: StateFlow<StaffProfileUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "🏗️ [VIEWMODEL] Initializing ViewModel and starting profile load.")
        loadStaffProfile()
    }

    fun loadStaffProfile() {
        viewModelScope.launch {
            Log.d(TAG, "🏗️ [VIEWMODEL] Requesting profile stream from repository.")
            _uiState.value = StaffProfileUiState.Loading
            repository.getStaffProfileStream().collect { result ->
                result.fold(
                    onSuccess = { profile ->
                        Log.i(TAG, "🏗️ [VIEWMODEL] Successfully fetched profile for '${profile.staffName}'.")
                        _uiState.value = StaffProfileUiState.Success(profile)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "🏗️ [VIEWMODEL] Failed to fetch profile: ${error.message}")
                        _uiState.value = StaffProfileUiState.Error(
                            error.localizedMessage ?: "Failed to load profile details"
                        )
                    }
                )
            }
        }
    }
}

// Fixed ViewModel Factory
class StaffProfileViewModelFactory(
    private val repository: StaffProfileRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StaffProfileViewModel::class.java)) {
            return StaffProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
