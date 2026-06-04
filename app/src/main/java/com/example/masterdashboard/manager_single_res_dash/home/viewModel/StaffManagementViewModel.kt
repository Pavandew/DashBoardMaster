package com.example.masterdashboard.manager_single_res_dash.home.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.home.repo.StaffManagementRepository
import com.example.masterdashboard.manager_single_res_dash.home.uistate.StaffListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StaffManagementViewModel: ViewModel() {

    private val repository = StaffManagementRepository()

    private val _staffListState = MutableStateFlow<StaffListUiState>(StaffListUiState.Loading)
    val staffListState: StateFlow<StaffListUiState> = _staffListState.asStateFlow()

    fun loadStaffMembers(ownerUid: String) {
        viewModelScope.launch {
            _staffListState.value = StaffListUiState.Loading

            repository.getStaffList(ownerUid).fold(
                onSuccess = { profiles ->
                    if (profiles.isEmpty()) {
                        _staffListState.value = StaffListUiState.Empty
                    } else {
                        _staffListState.value = StaffListUiState.Success(profiles)
                    }
                },
                onFailure = { exception ->
                    _staffListState.value = StaffListUiState.Error(exception.message ?: "Unknown collection retrieval exception error")
                }
            )
        }
    }
}