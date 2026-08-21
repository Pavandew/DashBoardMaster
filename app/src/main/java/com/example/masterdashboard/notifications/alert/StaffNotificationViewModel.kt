package com.example.masterdashboard.notifications.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlertsViewModel(
    private val sessionManager: SessionManager,
    private val repository: StaffNotificationRepository = StaffNotificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val expandedItemIds = MutableStateFlow<Set<String>>(emptySet())

    fun fetchLiveAlertsFeed() {
        val managerId = sessionManager.getUid()
        val userRole = sessionManager.getRole().lowercase().trim()
        val staffId = sessionManager.getStaffDocId()

        if (managerId.isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            repository.getNotificationsStream(managerId).collect { allAlerts ->
                // Role-based filtering
                val isManagerScope = userRole == "manager" || userRole == "owner_single" || userRole == "owner_multi"

                val filteredList = if (isManagerScope) {
                    // Managers see everything (Billing, Inventory, Staff alerts, etc.)
                    allAlerts
                } else {
                    allAlerts.filter { alert ->
                        val target = alert.targetRole.lowercase().trim()
                        when {
                            alert.targetStaffId.isNotEmpty() -> alert.targetStaffId == staffId
                            target.isNotEmpty() -> {
                                target == userRole || 
                                (userRole == "waiter_staff" && target == "waiter") ||
                                (userRole == "waiter" && target == "waiter_staff") ||
                                (userRole == "chef" && target == "kitchen") ||
                                (userRole == "kitchen" && target == "chef") ||
                                (userRole == "cashier" && target == "billing") ||
                                (userRole == "billing" && target == "cashier")
                            }
                            else -> true // General broadcast
                        }
                    }
                }

                _uiState.update { state ->
                    val updatedList = filteredList.map { item ->
                        item.copy(isExpanded = expandedItemIds.value.contains(item.id))
                    }
                    state.copy(isLoading = false, alertsList = updatedList)
                }
            }
        }
    }

    fun handleCardClicked(alertItem: StaffAlertItem) {
        // Mark as read in Firestore
        val managerId = sessionManager.getUid()
        if (managerId.isNotEmpty() && !alertItem.isRead) {
            viewModelScope.launch {
                try {
                    repository.markAsRead(managerId, alertItem.id)
                } catch (e: Exception) {
                    // Fail silently for read markers
                }
            }
        }

        // Toggle expansion if it's an actionable request
        if (alertItem.type == NotificationType.ACTIONABLE_REQUEST) {
            expandedItemIds.update { currentSet ->
                if (currentSet.contains(alertItem.id)) currentSet - alertItem.id else currentSet + alertItem.id
            }
            
            _uiState.update { state ->
                val updatedList = state.alertsList.map { item ->
                    item.copy(isExpanded = expandedItemIds.value.contains(item.id))
                }
                state.copy(alertsList = updatedList)
            }
        }
    }

    fun handleCardExpansionToggle(alertItem: StaffAlertItem) {
        handleCardClicked(alertItem)
    }

    fun updateAlertRequestStatus(alertId: String, nextStatus: RequestStatus) {
        val managerId = sessionManager.getUid()
        if (managerId.isEmpty()) return

        viewModelScope.launch {
            try {
                repository.updateNotificationStatus(managerId, alertId, nextStatus)
                if (nextStatus == RequestStatus.DONE) {
                    expandedItemIds.update { it - alertId }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Update failed: ${e.message}") }
            }
        }
    }

    class AlertsViewModelFactory(private val sessionManager: SessionManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlertsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AlertsViewModel(sessionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
