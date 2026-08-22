package com.example.masterdashboard.manager_single_res_dash.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.RegistrationDataModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RestaurantDetailsRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Fetches restaurant details directly from the Owner's user document.
     */
    suspend fun getRestaurantDetails(ownerUid: String): Result<RegistrationDataModel?> {
        return try {
            Log.d("ResDetailsRepo", "Fetching details from User document: $ownerUid")
            val doc = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .get()
                .await()
            
            val details = doc.toObject(RegistrationDataModel::class.java)
            if (details != null) {
                Log.i("ResDetailsRepo", "Data successfully fetched from User document.")
            } else {
                Log.w("ResDetailsRepo", "No restaurant data found inside User document: $ownerUid")
            }
            Result.success(details)
        } catch (e: Exception) {
            Log.e("ResDetailsRepo", "Error fetching details from User document", e)
            Result.failure(e)
        }
    }
}
