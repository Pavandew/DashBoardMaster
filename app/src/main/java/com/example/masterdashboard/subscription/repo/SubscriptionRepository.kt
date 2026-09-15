package com.example.masterdashboard.subscription.repo

import android.content.Context
import android.util.Log
import com.example.masterdashboard.subscription.models.BillingCycle
import com.example.masterdashboard.subscription.models.SubscriptionPlan
import com.example.masterdashboard.subscription.models.SubscriptionStatus
import com.example.masterdashboard.subscription.models.UserSubscriptionInfo
import com.example.masterdashboard.utils.RestaurantPathHelper
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.tasks.await

class SubscriptionRepository(private val context: Context) {

    companion object {
        private const val TAG = "SubscriptionRepository"
    }

    private val sessionManager = SessionManager(context)

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
                isRecommended = false
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
                isRecommended = false
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
                isRecommended = true
            )
        )

        Log.d(TAG, "getAvailablePlans: Loaded ${plans.size} default plans")
        return plans
    }

    suspend fun getUserSubscriptionInfo(): UserSubscriptionInfo {
        val uid = sessionManager.getRestaurantId().ifEmpty { sessionManager.getUid() }
        Log.d(TAG, "getUserSubscriptionInfo: Checking subscription details for Restaurant/UID: '$uid'")

        if (uid.isEmpty()) {
            Log.w(TAG, "getUserSubscriptionInfo: UID is empty, returning default trial info")
            return UserSubscriptionInfo()
        }

        return try {
            val docRef = RestaurantPathHelper.getRestaurantDocRef(uid)
            val doc = docRef.get().await()

            if (doc.exists()) {
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
                    val daysLeft = if (expiry > now) {
                        ((expiry - now) / (1000 * 60 * 60 * 24)).toInt()
                    } else 0

                    val status = try {
                        SubscriptionStatus.valueOf(statusStr)
                    } catch (e: Exception) {
                        Log.w(TAG, "getUserSubscriptionInfo: Unknown status string '$statusStr', evaluating by daysLeft=$daysLeft")
                        if (daysLeft > 0) SubscriptionStatus.TRIAL else SubscriptionStatus.EXPIRED
                    }

                    val info = UserSubscriptionInfo(
                        status = status,
                        planId = planId,
                        trialDaysRemaining = daysLeft,
                        startDateTimestamp = start,
                        expiryDateTimestamp = expiry,
                        autoRenew = autoRenew
                    )

                    Log.i(
                        TAG,
                        "getUserSubscriptionInfo: Success from ${docRef.path} -> Status=${info.status}, PlanID=${info.planId}, TrialDaysRemaining=${info.trialDaysRemaining}, ExpiryTimestamp=${info.expiryDateTimestamp}, AutoRenew=${info.autoRenew}"
                    )
                    info
                } else {
                    Log.w(TAG, "getUserSubscriptionInfo: No 'subscription' map found at ${docRef.path}, using default TRIAL status")
                    UserSubscriptionInfo()
                }
            } else {
                Log.w(TAG, "getUserSubscriptionInfo: Firestore document does not exist at ${docRef.path}")
                UserSubscriptionInfo()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserSubscriptionInfo: Error fetching subscription from Firestore", e)
            UserSubscriptionInfo()
        }
    }
}
