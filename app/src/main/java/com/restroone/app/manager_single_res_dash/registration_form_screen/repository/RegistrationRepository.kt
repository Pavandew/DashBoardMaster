package com.restroone.app.manager_single_res_dash.registration_form_screen.repository

import android.util.Log
import com.restroone.app.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.restroone.app.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
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

            // The callable verifies ownership and provisions the initial trial server-side.
            FirebaseFunctions.getInstance().getHttpsCallable("completeRestaurantSetup")
                .call(
                    mapOf(
                        AppConstants.FIELD_RESTAURANT_ID to restaurantId,
                        AppConstants.FIELD_RESTAURANT_NAME to data.restaurantName
                    )
                )
                .await()

            Log.i("RegistrationRepo", "Firebase Success: Outlet saved to 'restaurants/$restaurantId' & User linked.")
            Result.success(restaurantId)
        } catch (e: Exception) {
            Log.e("RegistrationRepo", "CRITICAL: Firebase registration save failed", e)
            Result.failure(e)
        }
    }
}
