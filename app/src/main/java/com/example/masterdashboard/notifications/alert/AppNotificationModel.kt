package com.example.masterdashboard.notifications.alert

enum class NotificationType { ACTIONABLE_REQUEST, INFORMATIONAL }
enum class RequestStatus { PENDING, ACCEPTED, DONE }

/**
 * Standardized data model for all in-app notifications and alerts.
 */
data class AppNotificationModel(
    val id: String,
    val tableId: String,
    val title: String,
    val message: String,
    val timeStamp: String,
    val type: NotificationType,
    val status: RequestStatus,
    val isRead: Boolean,
    val targetRole: String = "",
    val targetStaffId: String = "",
    val isExpanded: Boolean = false,
    val orderId: String = "",
    val orderDocPath: String = ""
)

/**
 * Represents the observable state for the Notification UI.
 */
data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<AppNotificationModel> = emptyList(),
    val errorMessage: String? = null
)
