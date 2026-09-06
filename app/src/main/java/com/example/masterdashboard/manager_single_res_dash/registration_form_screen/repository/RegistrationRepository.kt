package com.example.masterdashboard.manager_single_res_dash.registration_form_screen.repository

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class RegistrationRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Final submission: Saves all registration data directly into the OWNER'S user document.
     * This keeps all restaurant data tied to the specific owner UID as shown in your screenshot.
     */
    suspend fun saveFinalRegistration(data: RegistrationDataModel): Result<String> {
        return try {
            Log.i("RegistrationRepo", "Merging restaurant details into User: ${data.ownerUid}")
            
            val userDocRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(data.ownerUid)

            val finalData = data.toMap().toMutableMap()
            finalData[AppConstants.FIELD_IS_SETUP_COMPLETE] = true
            finalData[AppConstants.FIELD_RESTAURANT_ID] = data.ownerUid // Unique ID for Single owner portal
            
            // Save everything directly into the users/{ownerUid} document
            userDocRef.set(finalData, SetOptions.merge()).await()
                
            Log.i("RegistrationRepo", "Firebase Success: Owner profile updated with restaurant details.")
            Result.success(data.ownerUid) // In this structure, OwnerUID is the unique key
        } catch (e: Exception) {
            Log.e("RegistrationRepo", "CRITICAL: Firebase save failed", e)
            Result.failure(e)
        }
    }
}
