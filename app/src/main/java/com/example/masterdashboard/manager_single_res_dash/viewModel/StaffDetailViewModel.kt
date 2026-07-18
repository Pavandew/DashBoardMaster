package com.example.masterdashboard.manager_single_res_dash.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.repo.StaffManagementRepository
import com.example.masterdashboard.manager_single_res_dash.uistate.StaffDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StaffDetailViewModel(
    private val repository: StaffManagementRepository = StaffManagementRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<StaffDetailUiState>(StaffDetailUiState.Loading)

    val uiState: StateFlow<StaffDetailUiState> = _uiState.asStateFlow()

    fun loadStaffProfile(ownerUid: String, staffDocId: String) {
        if (ownerUid.isEmpty() || staffDocId.isEmpty()) {
            _uiState.value = StaffDetailUiState.Error("Invalid reference identity lookup indices parameters.")
            return
        }

        _uiState.value = StaffDetailUiState.Loading

        viewModelScope.launch {
            repository.getStaffCompleteDetails(ownerUid, staffDocId).fold(
                onSuccess = { parsedModel ->
                    _uiState.value = StaffDetailUiState.Success(parsedModel)
                },
                onFailure = { error ->
                    _uiState.value = StaffDetailUiState.Error(error.localizedMessage ?: "Unknown cloud transaction error caught.")
                }
            )
        }
    }
}