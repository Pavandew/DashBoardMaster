package com.example.masterdashboard.staff_dash.home.alert

enum class NotificationType { ACTIONABLE_REQUEST, INFORMATIONAL }
enum class RequestStatus { PENDING, ACCEPTED, DONE }

data class StaffAlertItem(
    val id: String,
    val tableId: String,
    val title: String,
    val message: String,
    val timeStamp: String,
    val type: NotificationType,
    val status: RequestStatus,
    val isRead: Boolean,
    val isExpanded: Boolean = false
)

data class AlertsUiState(
    val isLoading: Boolean = false,
    val alertsList: List<StaffAlertItem> = emptyList(),
    val errorMessage: String? = null
)