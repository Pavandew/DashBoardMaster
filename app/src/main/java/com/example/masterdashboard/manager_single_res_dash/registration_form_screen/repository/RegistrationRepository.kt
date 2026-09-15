package com.example.masterdashboard.manager_single_res_dash.registration_form_screen.repository

import android.util.Log
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class RegistrationRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Final submission: Saves restaurant outlet profile into `restaurants/{restaurantId}`
     * and links `restaurantId` inside the owner's `users/{ownerUid}` document.
     */
    suspend fun saveFinalRegistration(data: RegistrationDataModel): Result<String> {
        return try {
            val ownerUid = data.ownerUid
            val restaurantId = if (data.restaurantId.isNotEmpty()) data.restaurantId else ownerUid
            Log.i("RegistrationRepo", "Saving restaurant outlet details to 'restaurants/$restaurantId' for Owner: $ownerUid")

            // 1. Save complete restaurant outlet details into restaurants/{restaurantId}
            val outletDocRef = firestore.collection(AppConstants.COLLECTION_RESTAURANTS).document(restaurantId)
            val outletData = data.toMap().toMutableMap()
            outletData[AppConstants.FIELD_RESTAURANT_ID] = restaurantId
            outletData["ownerUid"] = ownerUid
            val ownerFullName = data.getUnifiedFullName().ifEmpty { "Owner" }
            outletData["ownerName"] = ownerFullName

            outletDocRef.set(outletData, SetOptions.merge()).await()

            // 2. Save user profile link & initial subscription in users/{ownerUid}
            val userDocRef = firestore.collection(AppConstants.COLLECTION_USERS).document(ownerUid)
            val now = System.currentTimeMillis()
            val thirtyDaysExpiry = now + (30L * 24 * 60 * 60 * 1000)

            val initialSubscriptionMap = mapOf(
                "status" to "TRIAL",
                "planId" to "single_res_trial",
                "startDateTimestamp" to now,
                "expiryDateTimestamp" to thirtyDaysExpiry,
                "autoRenew" to true
            )

            val userData = mutableMapOf<String, Any>(
                AppConstants.FIELD_IS_SETUP_COMPLETE to true,
                AppConstants.FIELD_RESTAURANT_ID to restaurantId,
                AppConstants.FIELD_RESTAURANT_NAME to data.restaurantName,
                AppConstants.FIELD_ROLE to AppConstants.ROLE_OWNER_SINGLE,
                "subscription" to initialSubscriptionMap
            )

            userDocRef.set(userData, SetOptions.merge()).await()

            Log.i("RegistrationRepo", "Firebase Success: Outlet saved to 'restaurants/$restaurantId' & User linked.")
            Result.success(restaurantId)
        } catch (e: Exception) {
            Log.e("RegistrationRepo", "CRITICAL: Firebase registration save failed", e)
            Result.failure(e)
        }
    }
}
