package com.example.masterdashboard.staff_dash.waiter_screens.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlertsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    // Local state tracking to remember which items the user has expanded in this session
    private val expandedItemIds = MutableStateFlow<Set<String>>(emptySet())

    fun fetchLiveAlertsFeed() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            // Mocking data here based on your screenshots.
            // In production, swap this with your Firestore real-time snapshotFlow listener stream!
            val dynamicMockList = listOf(
                StaffAlertItem("1", "T-07", "Table T-07", "Call Waiter", "11:35 AM", NotificationType.ACTIONABLE_REQUEST, RequestStatus.PENDING, isRead = false),
                StaffAlertItem("2", "T-02", "Table T-02", "Bill Requested", "11:32 AM", NotificationType.ACTIONABLE_REQUEST, RequestStatus.PENDING, isRead = true),
                StaffAlertItem("3", "T-08", "Table T-08", "Order is Ready", "11:30 AM", NotificationType.INFORMATIONAL, RequestStatus.DONE, isRead = false),
                StaffAlertItem("4", "T-11", "Table T-11", "Extra Spoon", "11:25 AM", NotificationType.ACTIONABLE_REQUEST, RequestStatus.PENDING, isRead = false),
                StaffAlertItem("5", "T-05", "Table T-05", "Order Confirmed", "11:20 AM", NotificationType.INFORMATIONAL, RequestStatus.DONE, isRead = true)
            )

            // Combine the underlying data stream with our local expansion set
            expandedItemIds.collect { expandedIds ->
                val mappedItems = dynamicMockList.map { item ->
                    item.copy(isExpanded = expandedIds.contains(item.id))
                }
                _uiState.update { it.copy(isLoading = false, alertsList = mappedItems) }
            }
        }
    }

    fun handleCardExpansionToggle(alertItem: StaffAlertItem) {
        if (alertItem.type == NotificationType.INFORMATIONAL) return // Ignore purely informational messages

        expandedItemIds.update { currentSet ->
            if (currentSet.contains(alertItem.id)) currentSet - alertItem.id else currentSet + alertItem.id
        }
    }

    fun updateAlertRequestStatus(alertId: String, nextStatus: RequestStatus) {
        viewModelScope.launch {
            // 1. Update your remote database architecture (e.g., Firestore document reference)
            // repository.updateNotificationStatus(alertId, nextStatus)

            // 2. Clear out local state allocations if the item processing is complete
            if (nextStatus == RequestStatus.DONE) {
                expandedItemIds.update { it - alertId }
            }

            // 3. Force re-fetch/sync pipeline trigger down across listeners
            fetchLiveAlertsFeed()
        }
    }

    class AlertsViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlertsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AlertsViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class context allocation configuration")
        }
    }
}