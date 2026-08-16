package com.example.masterdashboard.staff_dash.utils

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.masterdashboard.R
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus

import com.google.android.material.card.MaterialCardView

object StatusUIUtils {

    /**
     * Applies the standard label, background, and color for a specific order status.
     * Centralizes the UI look for Waiter, Kitchen, and Cashier screens.
     */
    fun applyStatusUI(
        context: Context,
        textView: TextView,
        status: ActiveOrderStatus,
        cardView: MaterialCardView? = null
    ) {
        val label: String
        val bgColor: Int
        val textColor: Int

        when (status) {
            ActiveOrderStatus.PENDING -> {
                label = "• Pending"
                bgColor = R.color.light_golden      // Amber/Yellow Background
                textColor = R.color.brown             // Darker Golden/Amber text
            }
            ActiveOrderStatus.PREPARING -> {
                label = "• Preparing"
                bgColor = R.color.status_reserved_bg // Light Orange
                textColor = R.color.status_reserved    // Orange
            }
            ActiveOrderStatus.READY -> {
                label = "• Ready"
                bgColor = R.color.primary_green          // Light Purple
                textColor = R.color.status_free           // Purple
            }
            ActiveOrderStatus.SERVED -> {
                label = "• Served"
                bgColor = R.color.bg_light_purple           // Light Gray
                textColor = R.color.accent_purple       // Gray
            }
            ActiveOrderStatus.BILLING -> {
                label = "• Billing"
                bgColor = R.color.status_billing_bg       // Light Blue
                textColor = R.color.status_billing        // Dark Blue
            }
            ActiveOrderStatus.PAID -> {
                label = "• Paid"
                bgColor = R.color.dark_green             // Dark Green Background
                textColor = R.color.white                // White text for high contrast
            }
        }

        applyUI(context, textView, label, bgColor, textColor, cardView)
    }

    /**
     * Helper to apply raw values (useful for custom labels like in Cashier screen)
     */
    fun applyUI(
        context: Context,
        textView: TextView,
        label: String,
        bgColorRes: Int,
        textColorRes: Int,
        cardView: MaterialCardView? = null
    ) {
        textView.text = label
        val colorInt = ContextCompat.getColor(context, bgColorRes)
        val textColorInt = ContextCompat.getColor(context, textColorRes)

        if (cardView != null) {
            cardView.setCardBackgroundColor(colorInt)
            textView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        } else {
            textView.setBackgroundResource(R.drawable.bg_status_preparing)
            textView.backgroundTintList = ColorStateList.valueOf(colorInt)
        }
        textView.setTextColor(textColorInt)
    }

    /**
     * Overload for String status (handy for Kitchen/Cashier screens)
     */
    fun applyStatusUI(
        context: Context,
        textView: TextView,
        statusStr: String?,
        cardView: MaterialCardView? = null
    ) {
        val normalized = statusStr?.uppercase()?.trim() ?: ""
        
        // Handle custom cashier/kitchen statuses first
        when (normalized) {
            "COMPLETED", "SERVED", "HANDED OVER" -> {
                applyUI(context, textView, "• Handed Over", R.color.bg_light_purple, R.color.accent_purple, cardView)
                return
            }
            "BILLING" -> {
                applyUI(context, textView, "READY FOR BILL", R.color.status_reserved_bg, R.color.status_reserved, cardView)
                return
            }
        }

        val statusEnum = when (normalized) {
            "PENDING", "NEW" -> ActiveOrderStatus.PENDING
            "PREPARING" -> ActiveOrderStatus.PREPARING
            "READY" -> ActiveOrderStatus.READY
            "SERVED" -> ActiveOrderStatus.SERVED
            "PAID", "SUCCESS" -> ActiveOrderStatus.PAID
            else -> ActiveOrderStatus.PENDING
        }
        applyStatusUI(context, textView, statusEnum, cardView)
    }

    /**
     * Specifically for individual items in an order (no background bubble).
     */
    fun applyItemStatusUI(
        context: Context,
        textView: TextView,
        status: String,
        isNewAddition: Boolean = false,
        delta: Int = 0
    ) {
        val colorRes: Int
        val label: String

        when {
            status.equals("REJECTED", true) -> {
                label = "Rejected by Chef"
                colorRes = R.color.red_alert
            }
            isNewAddition -> {
                label = if (delta > 1) "• $delta New Items Added" else "• New Item Added"
                colorRes = R.color.status_occupied
            }
            status.equals("SERVED", true) -> {
                label = "• Served"
                colorRes = R.color.accent_purple
            }
            status.equals("READY", true) -> {
                label = "• Ready to Serve"
                colorRes = R.color.status_free
            }
            status.equals("PREPARING", true) -> {
                label = "• Preparing in Kitchen"
                colorRes = R.color.status_reserved
            }
            else -> {
                label = "• Sent to Kitchen"
                colorRes = R.color.search_bar_hint
            }
        }

        textView.text = label
        textView.setTextColor(ContextCompat.getColor(context, colorRes))
        textView.background = null // Remove any background bubble for items
    }

    /**
     * Centralized color provider for Table status UI.
     * Returns a Pair of (Background Color String, Text Color String)
     */
    fun getTableStatusColors(status: String): Pair<String, String> {
        return when (status.uppercase().trim()) {
            "AVAILABLE", "FREE" -> "#112B1B" to "#4CAF50"  // Dark Green BG, Light Green Text
            "OCCUPIED" -> "#3E2006" to "#FF9800"          // Dark Orange BG, Light Orange Text
            "RESERVED" -> "#2C123D" to "#BA68C8"          // Dark Purple BG, Light Purple Text
            "BILLING" -> "#0D2339" to "#2196F3"           // Dark Blue BG, Light Blue Text
            else -> "#321111" to "#E57373"                // Dark Red BG, Light Red Text
        }
    }
}
