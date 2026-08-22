package com.example.masterdashboard.notifications.alert

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Standardized ViewModel for managing the Notification Feed state and interactions.
 */
class NotificationViewModel(
    private val sessionManager: SessionManager,
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "NotificationViewModel"
    }

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val expandedItemIds = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Starts the real-time observation of filtered notifications.
     */
    fun observeNotificationStream() {
        val managerId = sessionManager.getUid()
        val userRole = sessionManager.getRole()
        val staffId = sessionManager.getStaffDocId()

        if (managerId.isEmpty()) {
            Log.w(TAG, "Skipping notification stream because managerId is empty for role=$userRole")
            return
        }

        Log.d(TAG, "Starting notification stream: managerId=$managerId role=$userRole staffId=$staffId")
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            repository.getNotificationsStream(managerId, userRole, staffId).collect { filteredList ->
                // Secondary filter for Manager Dashboard specific highlights
                val cleanRole = userRole.lowercase().trim()
                val isManagerScope = cleanRole == "manager" || cleanRole == "owner_single" || cleanRole == "owner_multi"

                val finalDisplayList = if (isManagerScope) {
                    filteredList.filter { alert ->
                        val target = alert.targetRole.lowercase().trim()
                        val title = alert.title.lowercase()
                        val msg = alert.message.lowercase()

                        // Managers see:
                        // 1. Items specifically targeted to them
                        // 2. Broadcasts (all)
                        // 3. Business critical keywords (Stock, Payment, Rejections)
                        target == "manager" ||
                        target == "all" ||
                        title.contains("stock") ||
                        title.contains("payment") ||
                        title.contains("received") && title.contains("payment") || // Specifically "Payment Received", not "Order Received"
                        msg.contains("paid") ||
                        msg.contains("rejected")
                    }
                } else {
                    filteredList
                }

                Log.d(TAG, "Notification stream update received: raw=${filteredList.size} final=${finalDisplayList.size} managerScope=$isManagerScope")

                _uiState.update { state ->
                    val updatedList = finalDisplayList.map { item ->
                        item.copy(isExpanded = expandedItemIds.value.contains(item.id))
                    }
                    state.copy(isLoading = false, notifications = updatedList)
                }
            }
        }
    }

    /**
     * Handles card click for reading and expanding notifications.
     */
    fun handleNotificationClicked(notification: AppNotificationModel) {
        Log.d(TAG, "Notification clicked: id=${notification.id} type=${notification.type} isRead=${notification.isRead}")

        // 1. Mark as read in backend
        val managerId = sessionManager.getUid()
        if (managerId.isNotEmpty() && !notification.isRead) {
            viewModelScope.launch {
                try {
                    repository.markAsRead(managerId, notification.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mark notification as read: id=${notification.id}", e)
                }
            }
        }

        // 2. Handle expansion for actionable requests
        if (notification.type == NotificationType.ACTIONABLE_REQUEST) {
            expandedItemIds.update { currentSet ->
                if (currentSet.contains(notification.id)) currentSet - notification.id else currentSet + notification.id
            }

            Log.d(TAG, "Toggled actionable notification expansion: id=${notification.id} expanded=${expandedItemIds.value.contains(notification.id)}")

            _uiState.update { state ->
                val updatedList = state.notifications.map { item ->
                    item.copy(isExpanded = expandedItemIds.value.contains(item.id))
                }
                state.copy(notifications = updatedList)
            }
        }
    }

    /**
     * Updates the status of an actionable request (e.g., Accepting a Bill Request).
     */
    fun updateRequestStatus(notificationId: String, nextStatus: RequestStatus) {
        val managerId = sessionManager.getUid()
        if (managerId.isEmpty()) {
            Log.w(TAG, "Cannot update request status because managerId is empty: notificationId=$notificationId nextStatus=$nextStatus")
            return
        }

        Log.d(TAG, "Updating notification request status: managerId=$managerId notificationId=$notificationId nextStatus=$nextStatus")
        viewModelScope.launch {
            try {
                repository.updateNotificationStatus(managerId, notificationId, nextStatus)
                if (nextStatus == RequestStatus.DONE) {
                    expandedItemIds.update { it - notificationId }
                    Log.d(TAG, "Closed expanded state after completing notification: $notificationId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Status update failed for notificationId=$notificationId", e)
                _uiState.update { it.copy(errorMessage = "Status update failed: ${e.message}") }
            }
        }
    }

    /**
     * Standard Factory to provide SessionManager dependency.
     */
    class NotificationViewModelFactory(private val sessionManager: SessionManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NotificationViewModel(sessionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
