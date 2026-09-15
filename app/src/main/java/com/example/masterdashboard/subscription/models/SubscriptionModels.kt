package com.example.masterdashboard.subscription.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SubscriptionStatus {
    TRIAL,
    ACTIVE_MONTHLY,
    ACTIVE_YEARLY,
    EXPIRED,
    FREE_BASE
}

enum class BillingCycle {
    TRIAL,
    MONTHLY,
    YEARLY
}

data class SubscriptionPlan(
    val id: String,
    val title: String,
    val priceText: String,
    val numericPrice: Double,
    val billingCycle: BillingCycle,
    val discountTag: String? = null,
    val originalPriceText: String? = null,
    val features: List<String>,
    val isRecommended: Boolean = false
)

data class UserSubscriptionInfo(
    val status: SubscriptionStatus = SubscriptionStatus.TRIAL,
    val planId: String? = null,
    val trialDaysRemaining: Int = 30,
    val startDateTimestamp: Long = System.currentTimeMillis(),
    val expiryDateTimestamp: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val autoRenew: Boolean = true
) {
    fun getFormattedExpiryDate(): String {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(expiryDateTimestamp))
        } catch (e: Exception) {
            ""
        }
    }

    fun isProActive(): Boolean {
        return (status == SubscriptionStatus.ACTIVE_MONTHLY || status == SubscriptionStatus.ACTIVE_YEARLY) &&
                expiryDateTimestamp > System.currentTimeMillis()
    }
}
