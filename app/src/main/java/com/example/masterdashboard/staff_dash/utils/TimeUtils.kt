package com.example.masterdashboard.staff_dash.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimeUtils {
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    /**
     * Converts a Firebase Timestamp into a relative string:
     * - Today (< 1 min): "Just now"
     * - Today (< 60 min): "X min ago"
     * - Today (> 60 min): "X hr ago"
     * - Older: "dd MMM yyyy"
     */
    fun getRelativeTime(timestamp: Timestamp?): String {
        if (timestamp == null) return "Just now"
        
        val orderDate = timestamp.toDate()
        val currentTime = System.currentTimeMillis()
        val orderTimeMillis = orderDate.time
        val diffMillis = currentTime - orderTimeMillis

        val calendarOrder = Calendar.getInstance().apply { time = orderDate }
        val calendarToday = Calendar.getInstance()

        val isToday = calendarOrder.get(Calendar.YEAR) == calendarToday.get(Calendar.YEAR) &&
                     calendarOrder.get(Calendar.DAY_OF_YEAR) == calendarToday.get(Calendar.DAY_OF_YEAR)

        return try {
            if (isToday) {
                val diffMinutes = diffMillis / (60 * 1000)
                val diffHours = diffMillis / (60 * 60 * 1000)

                when {
                    diffMinutes < 1 -> "Just now"
                    diffMinutes < 60 -> "$diffMinutes min ago"
                    else -> "$diffHours hr ago"
                }
            } else {
                dateFormatter.format(orderDate)
            }
        } catch (e: Exception) {
            "Just now"
        }
    }

    /**
     * Returns the exact time in "hh:mm a" format.
     */
    fun formatToTime(timestamp: Timestamp?): String {
        return if (timestamp != null) timeFormatter.format(timestamp.toDate()) else "N/A"
    }

    /**
     * Returns the exact date in "dd MMM yyyy" format.
     */
    fun formatToDate(timestamp: Timestamp?): String {
        return if (timestamp != null) dateFormatter.format(timestamp.toDate()) else "N/A"
    }
}