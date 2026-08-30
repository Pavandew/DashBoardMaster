package com.example.masterdashboard.staff_dash.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimeUtils {
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    /**
     * Converts a Firebase Timestamp into a localized relative string:
     * - "Just now", "45 minutes ago", "2 hours ago" (for today)
     * - "Yesterday"
     * - "5 days ago"
     * - "Aug 25" (for older dates)
     */
    fun getRelativeTime(timestamp: Timestamp?): String {
        if (timestamp == null) return "Just now"
        
        val time = timestamp.toDate().time
        val now = System.currentTimeMillis()
        
        // Use Android's standard utility for robust, localized relative time strings
        return android.text.format.DateUtils.getRelativeTimeSpanString(
            time,
            now,
            android.text.format.DateUtils.MINUTE_IN_MILLIS
        ).toString()
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