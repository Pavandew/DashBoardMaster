package com.example.masterdashboard.manager_single_res_dash.home.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.home.models.StaffDataModel
import com.example.masterdashboard.manager_single_res_dash.home.repo.StaffManagementRepository
import com.example.masterdashboard.manager_single_res_dash.home.uistate.StaffListUiState
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StaffManagementViewModel: ViewModel() {
    companion object{
        private const val TAG = "StaffManagementViewModel ----> "
    }

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

    /**
     * Deletes a specific staff member from the restaurant's staff sub-collection.
     */
    fun deleteStaffMember(ownerUid: String, staffId: String, staffName: String) {
        Log.i(TAG, "Initiating deletion sequence for staff member: '$staffName' (ID: $staffId)")

        viewModelScope.launch {
            val firestore = FirebaseFirestore.getInstance()

            val staffDocRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .collection(AppConstants.COLLECTION_STAFF)
                .document(staffId)

            staffDocRef.delete()
                .addOnSuccessListener {
                    Log.i(TAG, "Successfully deleted staff: '$staffName'")
                    // Refresh the staff list to reflect changes in the UI
                    loadStaffMembers(ownerUid)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failure during staff removal from Firestore", exception)
                }
        }
    }
}
