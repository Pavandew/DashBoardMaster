package com.restroone.app.subscription.repo

import android.content.Context
import android.util.Log
import com.restroone.app.subscription.models.BillingCycle
import com.restroone.app.subscription.models.PaymentGateway
import com.restroone.app.subscription.models.PaymentOrderDetails
import com.restroone.app.subscription.models.PaymentResultPayload
import com.restroone.app.subscription.models.SubscriptionPlan
import com.restroone.app.subscription.models.SubscriptionStatus
import com.restroone.app.subscription.models.UserSubscriptionInfo
import com.restroone.app.utils.RestaurantPathHelper
import com.restroone.app.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class SubscriptionRepository(private val context: Context) {

    companion object {
        private const val TAG = "SubscriptionRepository"
        private const val COLLECTION_SUBSCRIPTIONS = "subscriptions"
    }

    private val sessionManager = SessionManager(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    fun getAvailablePlans(): List<SubscriptionPlan> {
        val singleOwnerFeatures = listOf(
            "Full POS & Billing Access",
            "Real-time Inventory & Stock Alerts",
            "Advanced Daily Sales Reports & Analytics",
            "Table & KOT Management",
            "Custom Receipt Setup & Tax Configurations",
            "24/7 Priority Support & Cloud Backup"
        )

        val plans = listOf(
            SubscriptionPlan(
                id = "single_res_trial",
                title = "30-Day Free Trial",
                priceText = "₹0",
                numericPrice = 0.0,
                billingCycle = BillingCycle.TRIAL,
                discountTag = "100% FREE",
                originalPriceText = null,
                features = singleOwnerFeatures,
                isRecommended = false,
                maxAllowedOutlets = 1
            ),
            SubscriptionPlan(
                id = "single_res_monthly",
                title = "Monthly Owner Pass",
                priceText = "₹299",
                numericPrice = 299.0,
                billingCycle = BillingCycle.MONTHLY,
                discountTag = null,
                originalPriceText = null,
                features = singleOwnerFeatures,
                isRecommended = false,
                maxAllowedOutlets = 1
            ),
            SubscriptionPlan(
                id = "single_res_yearly",
                title = "Yearly Owner Pass",
                priceText = "₹2,999",
                numericPrice = 2999.0,
                billingCycle = BillingCycle.YEARLY,
                discountTag = "SAVE 16%",
                originalPriceText = "₹3,588",
                features = singleOwnerFeatures,
                isRecommended = true,
                maxAllowedOutlets = 1
            )
        )

        Log.d(TAG, "getAvailablePlans: Loaded ${plans.size} default plans")
        return plans
    }

    suspend fun getUserSubscriptionInfo(): UserSubscriptionInfo {
        val resId = sessionManager.getRestaurantId().ifEmpty { sessionManager.getUid() }
        val ownerUid = sessionManager.getUid().ifEmpty { resId }

        Log.d(TAG, "getUserSubscriptionInfo: Checking subscription details for Restaurant: '$resId', Owner: '$ownerUid'")

        if (resId.isEmpty()) {
            Log.w(TAG, "getUserSubscriptionInfo: UID is empty, returning default trial info")
            return UserSubscriptionInfo()
        }

        return try {
            val docRef = RestaurantPathHelper.getRestaurantDocRef(resId, ownerUid)
            val doc = docRef.get().await()

            if (doc.exists()) {
                val subId = doc.getString("subscriptionId")
                if (!subId.isNullOrEmpty()) {
                    Log.d(TAG, "getUserSubscriptionInfo: Found dedicated subscriptionId '$subId'. Fetching top-level document...")
                    val subDoc = firestore.collection(COLLECTION_SUBSCRIPTIONS).document(subId).get().await()
                    if (subDoc.exists()) {
                        val statusStr = subDoc.getString("status") ?: "TRIAL"
                        val expiry = subDoc.getLong("expiryDateTimestamp") ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                        val start = subDoc.getLong("startDateTimestamp") ?: System.currentTimeMillis()
                        val planId = subDoc.getString("planId")
                        val autoRenew = subDoc.getBoolean("autoRenew") ?: true

                        val now = System.currentTimeMillis()
                        val daysLeft = if (expiry > now) ((expiry - now) / (1000 * 60 * 60 * 24)).toInt() else 0
                        val status = parseSubscriptionStatus(statusStr, daysLeft)

                        return UserSubscriptionInfo(
                            status = status,
                            planId = planId,
                            subscriptionId = subId,
                            trialDaysRemaining = daysLeft,
                            startDateTimestamp = start,
                            expiryDateTimestamp = expiry,
                            autoRenew = autoRenew
                        )
                    }
                }

                val subMap = doc.get("subscription") as? Map<*, *>
                if (subMap != null) {
                    val statusStr = subMap["status"] as? String ?: "TRIAL"
                    val planId = subMap["planId"] as? String

                    val expiry = when (val exp = subMap["expiryDateTimestamp"]) {
                        is Number -> exp.toLong()
                        is com.google.firebase.Timestamp -> exp.toDate().time
                        is java.util.Date -> exp.time
                        is String -> exp.toLongOrNull() ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                        else -> (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                    }

                    val start = when (val st = subMap["startDateTimestamp"]) {
                        is Number -> st.toLong()
                        is com.google.firebase.Timestamp -> st.toDate().time
                        is java.util.Date -> st.time
                        is String -> st.toLongOrNull() ?: System.currentTimeMillis()
                        else -> System.currentTimeMillis()
                    }

                    val autoRenew = subMap["autoRenew"] as? Boolean ?: true
                    val now = System.currentTimeMillis()
                    val daysLeft = if (expiry > now) ((expiry - now) / (1000 * 60 * 60 * 24)).toInt() else 0
                    val status = parseSubscriptionStatus(statusStr, daysLeft)

                    return UserSubscriptionInfo(
                        status = status,
                        planId = planId,
                        subscriptionId = null,
                        trialDaysRemaining = daysLeft,
                        startDateTimestamp = start,
                        expiryDateTimestamp = expiry,
                        autoRenew = autoRenew
                    )
                }
            }
            UserSubscriptionInfo()
        } catch (e: Exception) {
            Log.e(TAG, "getUserSubscriptionInfo: Error fetching subscription", e)
            UserSubscriptionInfo()
        }
    }

    suspend fun createPaymentOrder(plan: SubscriptionPlan, gateway: PaymentGateway): PaymentOrderDetails {
        val resId = sessionManager.getRestaurantId().ifEmpty { sessionManager.getUid() }
        val ownerUid = sessionManager.getUid().ifEmpty { resId }
        require(gateway == PaymentGateway.RAZORPAY) {
            "PhonePe direct payment is disabled. Please pay via PhonePe inside Razorpay Checkout."
        }
        val result = functions.getHttpsCallable("createSubscriptionPaymentOrder").call(
            mapOf("planId" to plan.id, "restaurantId" to resId)
        ).await()
        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Payment service returned an invalid response")
        return PaymentOrderDetails(
            orderId = data["orderId"] as? String
                ?: throw IllegalStateException("Payment service did not return an order ID"),
            planId = data["planId"] as? String ?: plan.id,
            planTitle = plan.title,
            numericPrice = (data["numericPrice"] as? Number)?.toDouble()
                ?: throw IllegalStateException("Payment service did not return the trusted price"),
            currency = data["currency"] as? String ?: "INR",
            gateway = gateway,
            ownerUid = ownerUid,
            restaurantId = resId,
            gatewayKeyId = data["keyId"] as? String ?: ""
        )
    }

    suspend fun verifyAndActivateSubscription(
        plan: SubscriptionPlan,
        payload: PaymentResultPayload
    ): String? {
        val resId = sessionManager.getRestaurantId().ifEmpty { sessionManager.getUid() }
        if (resId.isEmpty()) {
            Log.e(TAG, "verifyAndActivateSubscription: ResID is empty!")
            return null
        }

        return try {
            val result = functions.getHttpsCallable("verifyAndActivateSubscription").call(
                mapOf(
                    "paymentId" to payload.paymentId,
                    "orderId" to payload.orderId,
                    "signature" to (payload.signature ?: ""),
                    "gateway" to payload.gateway.name,
                    "planId" to plan.id,
                    "restaurantId" to resId
                )
            ).await()
            val response = result.data as? Map<*, *>
            if (response?.get("success") == true) {
                (response["subscriptionId"] as? String) ?: "sub_$resId"
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "verifyAndActivateSubscription: Server verification failed", e)
            null
        }
    }

    private fun parseSubscriptionStatus(statusStr: String, daysLeft: Int): SubscriptionStatus {
        if (daysLeft <= 0) return SubscriptionStatus.EXPIRED

        return when {
            statusStr.equals("ACTIVE_YEARLY", ignoreCase = true) -> SubscriptionStatus.ACTIVE_YEARLY
            statusStr.equals("ACTIVE_MONTHLY", ignoreCase = true) -> SubscriptionStatus.ACTIVE_MONTHLY
            statusStr.equals("ACTIVE", ignoreCase = true) -> SubscriptionStatus.ACTIVE_YEARLY
            statusStr.equals("TRIAL", ignoreCase = true) -> SubscriptionStatus.TRIAL
            else -> try {
                SubscriptionStatus.valueOf(statusStr)
            } catch (_: Exception) {
                SubscriptionStatus.TRIAL
            }
        }
    }
}
