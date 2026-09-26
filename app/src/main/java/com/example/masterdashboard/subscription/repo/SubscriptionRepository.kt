package com.example.masterdashboard.subscription.repo

import android.content.Context
import android.util.Log
import com.example.masterdashboard.subscription.models.BillingCycle
import com.example.masterdashboard.subscription.models.PaymentGateway
import com.example.masterdashboard.subscription.models.PaymentOrderDetails
import com.example.masterdashboard.subscription.models.PaymentResultPayload
import com.example.masterdashboard.subscription.models.SubscriptionDocument
import com.example.masterdashboard.subscription.models.SubscriptionPlan
import com.example.masterdashboard.subscription.models.SubscriptionStatus
import com.example.masterdashboard.subscription.models.UserSubscriptionInfo
import com.example.masterdashboard.utils.RestaurantPathHelper
import com.example.masterdashboard.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SubscriptionRepository(private val context: Context) {

    companion object {
        private const val TAG = "SubscriptionRepository"
        private const val COLLECTION_SUBSCRIPTIONS = "subscriptions"
        private const val COLLECTION_PAYMENTS = "payments"
        private const val COLLECTION_OWNERS = "owners"
    }

    private val sessionManager = SessionManager(context)
    private val firestore = FirebaseFirestore.getInstance()

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

    fun createPaymentOrder(plan: SubscriptionPlan, gateway: PaymentGateway): PaymentOrderDetails {
        val resId = sessionManager.getRestaurantId().ifEmpty { sessionManager.getUid() }
        val ownerUid = sessionManager.getUid().ifEmpty { resId }
        val orderId = "ORD_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6).uppercase()}"

        Log.i(TAG, "createPaymentOrder: Generated Order ID '$orderId' for Gateway ${gateway.name}, Plan '${plan.title}', ResID '$resId'")

        return PaymentOrderDetails(
            orderId = orderId,
            planId = plan.id,
            planTitle = plan.title,
            numericPrice = plan.numericPrice,
            currency = "INR",
            gateway = gateway,
            ownerUid = ownerUid,
            restaurantId = resId
        )
    }

    suspend fun verifyAndActivateSubscription(
        plan: SubscriptionPlan,
        payload: PaymentResultPayload
    ): Boolean {
        val resId = sessionManager.getRestaurantId().ifEmpty { sessionManager.getUid() }
        val ownerUid = sessionManager.getUid().ifEmpty { resId }

        if (resId.isEmpty()) {
            Log.e(TAG, "verifyAndActivateSubscription: ResID is empty!")
            return false
        }

        return try {
            val now = System.currentTimeMillis()
            val durationDays = when (plan.billingCycle) {
                BillingCycle.MONTHLY -> 30L
                BillingCycle.YEARLY -> 365L
                BillingCycle.TRIAL -> 30L
            }
            val expiry = now + (durationDays * 24 * 60 * 60 * 1000)
            val subStatusStr = when (plan.billingCycle) {
                BillingCycle.MONTHLY -> SubscriptionStatus.ACTIVE_MONTHLY.name
                BillingCycle.YEARLY -> SubscriptionStatus.ACTIVE_YEARLY.name
                BillingCycle.TRIAL -> SubscriptionStatus.TRIAL.name
            }

            val subId = "SUB_${resId.takeLast(6)}_${UUID.randomUUID().toString().take(6).uppercase()}"

            // 1. Write Payment Record to top-level `payments/{paymentId}`
            val paymentDoc = hashMapOf(
                "paymentId" to payload.paymentId,
                "orderId" to payload.orderId,
                "subscriptionId" to subId,
                "ownerUid" to ownerUid,
                "restaurantId" to resId,
                "amount" to plan.numericPrice,
                "gateway" to payload.gateway.name,
                "signature" to (payload.signature ?: ""),
                "status" to "SUCCESS",
                "createdAt" to now
            )
            firestore.collection(COLLECTION_PAYMENTS).document(payload.paymentId).set(paymentDoc).await()
            Log.d(TAG, "verifyAndActivateSubscription: Saved payment record '${payload.paymentId}'")

            // 2. Write Dedicated Top-Level `subscriptions/{subscriptionId}`
            val subDocument = SubscriptionDocument(
                subscriptionId = subId,
                ownerUid = ownerUid,
                restaurantIds = listOf(resId),
                planId = plan.id,
                status = subStatusStr,
                billingCycle = plan.billingCycle.name,
                numericPrice = plan.numericPrice,
                currency = "INR",
                startDateTimestamp = now,
                expiryDateTimestamp = expiry,
                autoRenew = true,
                latestPaymentId = payload.paymentId,
                updatedAt = now
            )
            firestore.collection(COLLECTION_SUBSCRIPTIONS).document(subId).set(subDocument).await()
            Log.d(TAG, "verifyAndActivateSubscription: Saved dedicated subscription document '$subId'")

            // 3. Update Restaurant Document with reference & status
            val resRef = RestaurantPathHelper.getRestaurantDocRef(resId, ownerUid)
            val resUpdateMap = hashMapOf(
                "subscriptionId" to subId,
                "subscriptionStatus" to subStatusStr,
                "subscriptionExpiry" to expiry,
                "subscription" to hashMapOf(
                    "status" to subStatusStr,
                    "planId" to plan.id,
                    "startDateTimestamp" to now,
                    "expiryDateTimestamp" to expiry,
                    "autoRenew" to true,
                    "lastPaymentId" to payload.paymentId
                )
            )
            resRef.set(resUpdateMap, SetOptions.merge()).await()
            Log.d(TAG, "verifyAndActivateSubscription: Updated restaurant doc '${resRef.path}'")

            // 4. Update Owner Summary Document
            if (ownerUid.isNotEmpty()) {
                val ownerRef = firestore.collection(COLLECTION_OWNERS).document(ownerUid)
                val ownerUpdateMap = hashMapOf(
                    "ownerUid" to ownerUid,
                    "activeSubscriptionIds" to com.google.firebase.firestore.FieldValue.arrayUnion(subId),
                    "ownedRestaurantIds" to com.google.firebase.firestore.FieldValue.arrayUnion(resId),
                    "maxAllowedOutlets" to plan.maxAllowedOutlets
                )
                ownerRef.set(ownerUpdateMap, SetOptions.merge()).await()
                Log.d(TAG, "verifyAndActivateSubscription: Updated owner doc '${ownerRef.path}'")
            }

            Log.i(TAG, "verifyAndActivateSubscription: SUCCESS! Subscription activated for '$resId' with plan '${plan.title}' until $expiry")
            true
        } catch (e: Exception) {
            Log.e(TAG, "verifyAndActivateSubscription: Failed to activate subscription in Firestore", e)
            false
        }
    }

    private fun parseSubscriptionStatus(statusStr: String, daysLeft: Int): SubscriptionStatus {
        return try {
            SubscriptionStatus.valueOf(statusStr)
        } catch (e: Exception) {
            if (daysLeft > 0) SubscriptionStatus.TRIAL else SubscriptionStatus.EXPIRED
        }
    }
}
