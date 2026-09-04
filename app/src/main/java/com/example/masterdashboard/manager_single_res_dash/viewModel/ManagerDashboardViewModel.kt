package com.example.masterdashboard.manager_single_res_dash.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.models.DashboardSummary
import com.example.masterdashboard.manager_single_res_dash.models.StatMetric
import com.example.masterdashboard.manager_single_res_dash.repo.ManagerDashboardRepository
import com.example.masterdashboard.manager_single_res_dash.repo.ReportsRepository
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ManagerDashboardViewModel(
    private val repository: ManagerDashboardRepository = ManagerDashboardRepository(),
    private val reportsRepository: ReportsRepository = ReportsRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "ManagerDashVM"
    }

    private val _orderStatusSummary = MutableStateFlow(DashboardSummary())
    val orderStatusSummary: StateFlow<DashboardSummary> = _orderStatusSummary.asStateFlow()

    private val _todayMetrics = MutableStateFlow<List<StatMetric>>(getInitialEmptyMetrics())
    val todayMetrics: StateFlow<List<StatMetric>> = _todayMetrics.asStateFlow()

    private val _restaurantName = MutableStateFlow("")
    val restaurantName: StateFlow<String> = _restaurantName.asStateFlow()

    // Persistent UI state for Quick Actions expansion
    private val _isQuickActionsExpanded = MutableStateFlow(false)
    val isQuickActionsExpanded: StateFlow<Boolean> = _isQuickActionsExpanded.asStateFlow()

    private var isListeningToUpdates = false

    /**
     * Initiates real-time tracking of active order status counts AND today's financial metrics.
     */
    fun startRealTimeOrderStatusTracking(managerId: String) {
        if (managerId.isEmpty()) return

        if (!isListeningToUpdates) {
            isListeningToUpdates = true

            // 1. Listen to active orders
            viewModelScope.launch {
                repository.getActiveOrdersStream(managerId).collect { statuses ->
                    val counts = statuses.groupingBy { it.uppercase() }.eachCount()

                    val newSummary = DashboardSummary(
                        newCount = String.format(Locale.getDefault(), "%02d", counts["PENDING"] ?: 0),
                        kitchenCount = String.format(Locale.getDefault(), "%02d", counts["PREPARING"] ?: 0),
                        readyCount = String.format(Locale.getDefault(), "%02d", counts["READY"] ?: 0),
                        servedCount = String.format(Locale.getDefault(), "%02d", (counts["SERVED"] ?: 0) + (counts["PAID"] ?: 0)),
                        cancelledCount = String.format(Locale.getDefault(), "%02d", counts["REJECTED"] ?: 0)
                    )

                    _orderStatusSummary.value = newSummary
                    Log.d(TAG, "Order status summary recalculated: $newSummary")

                    // Re-calculate active order count for overview metrics
                    val activeCount = (counts["PENDING"] ?: 0) + (counts["PREPARING"] ?: 0) + (counts["READY"] ?: 0)
                    updateActiveOrdersInMetrics(activeCount)
                }
            }

            // 2. Listen to today's completed sales reports
            viewModelScope.launch {
                reportsRepository.getReportSummaryStream(managerId, ReportsRepository.TimeFilter.TODAY)
                    .collect { todaySummary ->
                        Log.d(TAG, "Today's report summary received for dashboard: Revenue=₹${todaySummary.totalRevenue}, Orders=${todaySummary.totalOrders}")

                        val activeCount = _orderStatusSummary.value.let {
                            (it.newCount.toIntOrNull() ?: 0) + (it.kitchenCount.toIntOrNull() ?: 0) + (it.readyCount.toIntOrNull() ?: 0)
                        }

                        val salesVal = if (todaySummary.totalRevenue > 0) formatCurrency(todaySummary.totalRevenue) else "₹ 0"
                        val salesSub = if (todaySummary.totalRevenue > 0) "Today's Collection" else "No sales today"

                        val ordersVal = if (todaySummary.totalOrders > 0) "${todaySummary.totalOrders}" else "0"
                        val ordersSub = if (todaySummary.totalOrders > 0) "Today's Completed" else "No orders today"

                        val activeVal = "$activeCount"
                        val activeSub = if (activeCount > 0) "In Progress" else "No active orders"

                        val avgVal = if (todaySummary.avgOrderValue > 0) formatCurrency(todaySummary.avgOrderValue) else "₹ 0"
                        val avgSub = "Per Transaction"

                        _todayMetrics.value = listOf(
                            StatMetric("Total Sales", salesVal, salesSub, todaySummary.totalRevenue > 0),
                            StatMetric("Total Orders", ordersVal, ordersSub, todaySummary.totalOrders > 0),
                            StatMetric("Active Orders", activeVal, activeSub, activeCount > 0),
                            StatMetric("Avg Order Value", avgVal, avgSub, todaySummary.avgOrderValue > 0)
                        )
                    }
            }
        }
    }

    private fun updateActiveOrdersInMetrics(activeCount: Int) {
        val currentList = _todayMetrics.value.toMutableList()
        if (currentList.size >= 3) {
            val activeVal = "$activeCount"
            val activeSub = if (activeCount > 0) "In Progress" else "No active orders"
            currentList[2] = StatMetric("Active Orders", activeVal, activeSub, activeCount > 0)
            _todayMetrics.value = currentList
        }
    }

    private fun getInitialEmptyMetrics(): List<StatMetric> {
        return listOf(
            StatMetric("Total Sales", "₹ 0", "No sales today", true),
            StatMetric("Total Orders", "0", "No orders today", true),
            StatMetric("Active Orders", "0", "No active orders", true),
            StatMetric("Avg Order Value", "₹ 0", "Per Transaction", true)
        )
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            val formatted = formatter.format(amount)
            if (!formatted.startsWith("₹")) "₹ $amount" else formatted.replace("₹", "₹ ")
        } catch (e: Exception) {
            "₹ $amount"
        }
    }

    /**
     * Loads the restaurant name, prioritizing local cache in SessionManager.
     */
    fun loadRestaurantDetails(ownerUid: String, sessionManager: SessionManager) {
        val cachedName = sessionManager.getRestaurantName()
        if (cachedName.isNotEmpty()) {
            Log.d(TAG, "Cache Hit: Using restaurant name from SessionManager: $cachedName")
            _restaurantName.value = cachedName
            return
        }

        if (ownerUid.isEmpty()) return

        viewModelScope.launch {
            Log.d(TAG, "Cache Miss: Fetching restaurant name from Firestore for UID: $ownerUid")
            val fetchedName = repository.getRestaurantName(ownerUid) ?: "My Restaurant"
            sessionManager.saveRestaurantName(fetchedName)
            _restaurantName.value = fetchedName
        }
    }

    fun toggleQuickActionsExpanded() {
        _isQuickActionsExpanded.value = !_isQuickActionsExpanded.value
    }
}
