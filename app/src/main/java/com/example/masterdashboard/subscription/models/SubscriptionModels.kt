package com.example.masterdashboard.subscription.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-level status of a subscription for a restaurant/owner.
 */
enum class SubscriptionStatus {
    TRIAL,
    ACTIVE_MONTHLY,
    ACTIVE_YEARLY,
    EXPIRED,
    FREE_BASE
}

/**
 * Billing cycle duration options.
 */
enum class BillingCycle {
    TRIAL,
    MONTHLY,
    YEARLY
}

/**
 * Supported payment gateways for subscription purchases.
 */
enum class PaymentGateway {
    RAZORPAY,
    PHONEPE
}

/**
 * Lifecycle state of a payment processing request.
 */
sealed class PaymentState {
    object Idle : PaymentState()
    object InitiatingOrder : PaymentState()
    data class AwaitingSdk(val orderDetails: PaymentOrderDetails) : PaymentState()
    object VerifyingPayment : PaymentState()
    data class Success(val message: String, val subscriptionId: String) : PaymentState()
    data class Error(val errorMessage: String) : PaymentState()
}

/**
 * Represents a selectable subscription plan in the UI.
 */
data class SubscriptionPlan(
    val id: String,
    val title: String,
    val priceText: String,
    val numericPrice: Double,
    val billingCycle: BillingCycle,
    val discountTag: String? = null,
    val originalPriceText: String? = null,
    val features: List<String>,
    val isRecommended: Boolean = false,
    val maxAllowedOutlets: Int = 1
)

/**
 * Summary of active subscription status for a specific restaurant outlet.
 */
data class UserSubscriptionInfo(
    val status: SubscriptionStatus = SubscriptionStatus.TRIAL,
    val planId: String? = null,
    val subscriptionId: String? = null,
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

/**
 * Order details passed to Razorpay / PhonePe SDK for launching checkout.
 */
data class PaymentOrderDetails(
    val orderId: String,
    val planId: String,
    val planTitle: String,
    val numericPrice: Double,
    val currency: String = "INR",
    val gateway: PaymentGateway,
    val ownerUid: String,
    val restaurantId: String
)

/**
 * Payload returned after payment SDK checkout finishes.
 */
data class PaymentResultPayload(
    val paymentId: String,
    val orderId: String,
    val signature: String? = null,
    val gateway: PaymentGateway,
    val rawResponse: String? = null
)

/**
 * Document representation for the dedicated `subscriptions/{subscriptionId}` collection in Firestore.
 */
data class SubscriptionDocument(
    val subscriptionId: String = "",
    val ownerUid: String = "",
    val restaurantIds: List<String> = emptyList(),
    val planId: String = "",
    val status: String = "TRIAL",
    val billingCycle: String = "TRIAL",
    val numericPrice: Double = 0.0,
    val currency: String = "INR",
    val startDateTimestamp: Long = System.currentTimeMillis(),
    val expiryDateTimestamp: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val autoRenew: Boolean = true,
    val latestPaymentId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Owner billing summary for managing multi-restaurant outlet limits.
 */
data class OwnerBillingSummary(
    val ownerUid: String = "",
    val maxAllowedOutlets: Int = 1,
    val ownedRestaurantIds: List<String> = emptyList(),
    val activeSubscriptionIds: List<String> = emptyList()
)
