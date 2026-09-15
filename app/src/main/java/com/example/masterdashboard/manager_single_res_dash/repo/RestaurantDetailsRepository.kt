package com.example.masterdashboard.manager_single_res_dash.repo

import android.util.Log
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.example.masterdashboard.utils.RestaurantPathHelper
import kotlinx.coroutines.tasks.await

class RestaurantDetailsRepository {

    /**
     * Fetches restaurant profile details using RestaurantPathHelper (`restaurants/{id}`).
     */
    suspend fun getRestaurantDetails(ownerUid: String): Result<RegistrationDataModel?> {
        return try {
            Log.d("ResDetailsRepo", "Fetching details for ownerUid/restaurantId: $ownerUid")
            val docRef = RestaurantPathHelper.getRestaurantDocRef(ownerUid)
            val doc = docRef.get().await()

            val details = doc.toObject(RegistrationDataModel::class.java)
            if (details != null) {
                Log.i("ResDetailsRepo", "Data successfully fetched from path: ${docRef.path}")
            } else {
                Log.w("ResDetailsRepo", "No restaurant data found at path: ${docRef.path}")
            }
            Result.success(details)
        } catch (e: Exception) {
            Log.e("ResDetailsRepo", "Error fetching restaurant details for: $ownerUid", e)
            Result.failure(e)
        }
    }
}
