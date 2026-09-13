package com.example.masterdashboard.manager_single_res_dash.settings.repo

import android.util.Log
import com.example.masterdashboard.R
import com.example.masterdashboard.manager_single_res_dash.models.ReportSummaryModel
import com.example.masterdashboard.manager_single_res_dash.models.ShiftSales
import com.example.masterdashboard.manager_single_res_dash.models.TopSellingFoodItem
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsRepository {

    companion object {
        private const val TAG = "ReportsRepository"
    }

    private val db = FirebaseFirestore.getInstance()

    enum class TimeFilter {
        TODAY, WEEK, MONTH
    }

    /**
     * Streams completed orders in real-time for the specified time filter.
     * Returns zero values if no completed orders exist.
     */
    fun getReportSummaryStream(managerId: String, filter: TimeFilter): Flow<ReportSummaryModel> = callbackFlow {
        if (managerId.isEmpty()) {
            Log.w(TAG, "getReportSummaryStream: managerId is empty. Emitting 0 summary.")
            trySend(ReportSummaryModel())
            close()
            return@callbackFlow
        }

        Log.i(TAG, "Starting real-time listener for managerId: $managerId, filter: $filter")

        val registration = db.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_COMPLETED_ORDERS)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore Snapshot Error in completed_orders listener", error)
                    trySend(ReportSummaryModel())
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    Log.d(TAG, "No completed_orders found in Firestore for manager $managerId. Emitting 0 summary.")
                    trySend(ReportSummaryModel())
                    return@addSnapshotListener
                }

                Log.d(TAG, "Firestore Snapshot Received: ${snapshots.size()} total completed order documents found in collection.")

                var totalRevenue = 0.0
                var totalOrders = 0
                var totalDiscounts = 0.0
                var cashAmount = 0.0
                var upiAmount = 0.0
                var cardAmount = 0.0
                var dineInSales = 0.0
                var dineInOrders = 0
                var takeawaySales = 0.0
                var takeawayOrders = 0
                var grossSubtotal = 0.0
                var totalGst = 0.0

                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())

                val now = Date()
                val todayStr = sdfDate.format(now)
                val currentMonthStr = sdfMonth.format(now)

                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val sevenDaysAgo = cal.time

                for (doc in snapshots.documents) {
                    val billingDate = doc.getString(AppConstants.FIELD_BILLING_DATE) ?: ""
                    val billingMonth = doc.getString(AppConstants.FIELD_BILLING_MONTH) ?: ""
                    val timestamp = doc.getTimestamp(AppConstants.FIELD_PAID_AT)?.toDate()
                        ?: doc.getTimestamp(AppConstants.FIELD_TIMESTAMP)?.toDate()

                    val docDate = when {
                        billingDate.isNotEmpty() -> billingDate
                        timestamp != null -> sdfDate.format(timestamp)
                        else -> ""
                    }

                    val docMonth = when {
                        billingMonth.isNotEmpty() -> billingMonth
                        timestamp != null -> sdfMonth.format(timestamp)
                        else -> ""
                    }

                    val matchesFilter = when (filter) {
                        TimeFilter.TODAY -> docDate == todayStr
                        TimeFilter.MONTH -> docMonth == currentMonthStr
                        TimeFilter.WEEK -> timestamp != null && timestamp.after(sevenDaysAgo)
                    }

                    if (!matchesFilter) continue

                    totalOrders++
                    val grandTotal = doc.getDouble(AppConstants.FIELD_GRAND_TOTAL) ?: 0.0
                    val subtotal = doc.getDouble(AppConstants.FIELD_SUBTOTAL) ?: 0.0
                    val gst = doc.getDouble(AppConstants.FIELD_GST) ?: 0.0
                    val discount = doc.getDouble(AppConstants.FIELD_DISCOUNT_AMOUNT) ?: 0.0
                    val payMethod = doc.getString(AppConstants.FIELD_PAYMENT_METHOD)?.uppercase() ?: "CASH"
                    val orderType = doc.getString(AppConstants.FIELD_ORDER_TYPE) ?: "Dine-In"

                    totalRevenue += grandTotal
                    grossSubtotal += subtotal
                    totalGst += gst
                    totalDiscounts += discount

                    when {
                        payMethod.contains("CASH") -> cashAmount += grandTotal
                        payMethod.contains("UPI") || payMethod.contains("ONLINE") || payMethod.contains("QR") -> upiAmount += grandTotal
                        payMethod.contains("CARD") -> cardAmount += grandTotal
                        else -> cashAmount += grandTotal
                    }

                    if (orderType.contains("Takeaway", ignoreCase = true) || orderType.contains("Parcel", ignoreCase = true)) {
                        takeawaySales += grandTotal
                        takeawayOrders++
                    } else {
                        dineInSales += grandTotal
                        dineInOrders++
                    }
                }

                val avgOrderVal = if (totalOrders > 0) totalRevenue / totalOrders else 0.0

                val summary = ReportSummaryModel(
                    totalRevenue = totalRevenue,
                    totalOrders = totalOrders,
                    avgOrderValue = avgOrderVal,
                    totalDiscounts = totalDiscounts,
                    cashAmount = cashAmount,
                    upiAmount = upiAmount,
                    cardAmount = cardAmount,
                    dineInSales = dineInSales,
                    dineInOrders = dineInOrders,
                    takeawaySales = takeawaySales,
                    takeawayOrders = takeawayOrders,
                    grossSubtotal = grossSubtotal,
                    totalGst = totalGst
                )

                Log.i(TAG, "Report Data Aggregated for filter $filter: Revenue=₹$totalRevenue, Orders=$totalOrders, Cash=₹$cashAmount, UPI=₹$upiAmount, Card=₹$cardAmount")
                trySend(summary)
            }

        awaitClose { 
            Log.d(TAG, "Closing real-time listener for managerId: $managerId")
            registration.remove() 
        }
    }

    /**
     * Streams sales distribution across today's meal shifts (Morning, Lunch, Evening, Dinner).
     */
    fun getTodaySalesTrendStream(managerId: String): Flow<ShiftSales> = callbackFlow {
        if (managerId.isEmpty()) {
            trySend(ShiftSales())
            close()
            return@callbackFlow
        }

        val registration = db.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_COMPLETED_ORDERS)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || snapshots.isEmpty) {
                    trySend(ShiftSales())
                    return@addSnapshotListener
                }

                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = sdfDate.format(Date())

                var morningSales = 0.0
                var morningOrders = 0

                var lunchSales = 0.0
                var lunchOrders = 0

                var eveningSales = 0.0
                var eveningOrders = 0

                var dinnerSales = 0.0
                var dinnerOrders = 0

                val calendar = Calendar.getInstance()

                for (doc in snapshots.documents) {
                    val billingDate = doc.getString(AppConstants.FIELD_BILLING_DATE) ?: ""
                    val timestamp = doc.getTimestamp(AppConstants.FIELD_PAID_AT)?.toDate()
                        ?: doc.getTimestamp(AppConstants.FIELD_TIMESTAMP)?.toDate()

                    val docDate = when {
                        billingDate.isNotEmpty() -> billingDate
                        timestamp != null -> sdfDate.format(timestamp)
                        else -> ""
                    }

                    if (docDate != todayStr) continue

                    val grandTotal = doc.getDouble(AppConstants.FIELD_GRAND_TOTAL) ?: 0.0
                    val hour = if (timestamp != null) {
                        calendar.time = timestamp
                        calendar.get(Calendar.HOUR_OF_DAY)
                    } else 12

                    when (hour) {
                        in 8..11 -> {
                            morningSales += grandTotal
                            morningOrders++
                        }
                        in 12..15 -> {
                            lunchSales += grandTotal
                            lunchOrders++
                        }
                        in 16..19 -> {
                            eveningSales += grandTotal
                            eveningOrders++
                        }
                        else -> {
                            dinnerSales += grandTotal
                            dinnerOrders++
                        }
                    }
                }

                // Determine Peak Shift
                val shifts = listOf(
                    "Peak: Morning" to morningSales,
                    "Peak: Lunch" to lunchSales,
                    "Peak: Evening" to eveningSales,
                    "Peak: Dinner" to dinnerSales
                )
                val maxShift = shifts.maxByOrNull { it.second }
                val peakTag = if (maxShift != null && maxShift.second > 0) maxShift.first else "No Rush"

                val trend = ShiftSales(
                    morningSales = morningSales,
                    morningOrders = morningOrders,
                    lunchSales = lunchSales,
                    lunchOrders = lunchOrders,
                    eveningSales = eveningSales,
                    eveningOrders = eveningOrders,
                    dinnerSales = dinnerSales,
                    dinnerOrders = dinnerOrders,
                    peakShiftName = peakTag
                )

                Log.d(TAG, "Today's Shift Sales Trend: $trend")
                trySend(trend)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Streams the top 3 best-selling food items dynamically aggregated from completed orders.
     * If Today has no completed orders, falls back to This Week's (Last 7 Days) top selling items.
     */
    fun getTopSellingItemsStream(managerId: String): Flow<List<TopSellingFoodItem>> = callbackFlow {
        if (managerId.isEmpty()) {
            trySend(getEmptyTopSelling())
            close()
            return@callbackFlow
        }

        Log.d(TAG, "Starting Top Selling Items stream for managerId: $managerId")

        val registration = db.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_COMPLETED_ORDERS)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || snapshots.isEmpty) {
                    Log.d(TAG, "No completed_orders found. Emitting empty top selling list.")
                    trySend(getEmptyTopSelling())
                    return@addSnapshotListener
                }

                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = sdfDate.format(Date())

                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val sevenDaysAgo = cal.time

                val todayQtyMap = mutableMapOf<String, Int>()
                val todayRevenueMap = mutableMapOf<String, Double>()

                val weekQtyMap = mutableMapOf<String, Int>()
                val weekRevenueMap = mutableMapOf<String, Double>()

                for (doc in snapshots.documents) {
                    val billingDate = doc.getString(AppConstants.FIELD_BILLING_DATE) ?: ""
                    val timestamp = doc.getTimestamp(AppConstants.FIELD_PAID_AT)?.toDate()
                        ?: doc.getTimestamp(AppConstants.FIELD_TIMESTAMP)?.toDate()

                    val docDate = when {
                        billingDate.isNotEmpty() -> billingDate
                        timestamp != null -> sdfDate.format(timestamp)
                        else -> ""
                    }

                    val isToday = docDate == todayStr
                    val isThisWeek = timestamp != null && timestamp.after(sevenDaysAgo)

                    @Suppress("UNCHECKED_CAST")
                    val rawItems = doc.get(AppConstants.FIELD_ORDER_ITEMS) as? List<Map<String, Any>> ?: continue
                    for (itemMap in rawItems) {
                        val name = itemMap[AppConstants.FIELD_ITEM_NAME] as? String ?: continue
                        val qty = (itemMap[AppConstants.FIELD_QUANTITY] as? Number)?.toInt() ?: 1
                        val price = (itemMap[AppConstants.FIELD_ITEM_PRICE] as? Number)?.toDouble() ?: 0.0
                        val rowTotal = (itemMap[AppConstants.FIELD_ROW_TOTAL] as? Number)?.toDouble() ?: (price * qty)

                        if (isToday) {
                            todayQtyMap[name] = (todayQtyMap[name] ?: 0) + qty
                            todayRevenueMap[name] = (todayRevenueMap[name] ?: 0.0) + rowTotal
                        }

                        if (isThisWeek || isToday) {
                            weekQtyMap[name] = (weekQtyMap[name] ?: 0) + qty
                            weekRevenueMap[name] = (weekRevenueMap[name] ?: 0.0) + rowTotal
                        }
                    }
                }

                // If Today has sales, show Today's top items. Otherwise fallback to This Week's top items.
                val targetQtyMap = if (todayQtyMap.isNotEmpty()) todayQtyMap else weekQtyMap
                val targetRevMap = if (todayQtyMap.isNotEmpty()) todayRevenueMap else weekRevenueMap
                val periodTag = if (todayQtyMap.isNotEmpty()) "Today" else "This Week"

                if (targetQtyMap.isEmpty()) {
                    Log.d(TAG, "No completed order items found for today or this week. Emitting empty state.")
                    trySend(getEmptyTopSelling())
                    return@addSnapshotListener
                }

                val top3 = targetQtyMap.entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .mapIndexed { index, entry ->
                        val name = entry.key
                        val qty = entry.value
                        val rev = targetRevMap[name] ?: 0.0
                        TopSellingFoodItem(
                            id = (index + 1).toString(),
                            name = "$name ($periodTag)",
                            orderCount = qty,
                            totalPriceText = "₹ ${String.format(Locale.US, "%.0f", rev)}",
                            imageResId = R.drawable.ic_shield_24dp
                        )
                    }

                Log.i(TAG, "Top Selling Items aggregated ($periodTag): ${top3.size} items found.")
                trySend(top3)
            }

        awaitClose { registration.remove() }
    }

    private fun getEmptyTopSelling(): List<TopSellingFoodItem> {
        return listOf(
            TopSellingFoodItem("1", "No Sales Recorded Yet", 0, "₹ 0", R.drawable.ic_shield_24dp)
        )
    }
}
