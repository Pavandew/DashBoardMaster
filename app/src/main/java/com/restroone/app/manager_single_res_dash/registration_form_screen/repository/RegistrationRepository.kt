package com.restroone.app.manager_single_res_dash.registration_form_screen.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.restroone.app.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.ImageUtils
import kotlinx.coroutines.tasks.await

class RegistrationRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Final submission: Saves restaurant outlet profile into `restaurants/{restaurantId}`
     * and links `restaurantId` inside the owner's `users/{ownerUid}` document.
     */
    suspend fun saveFinalRegistration(context: Context, data: RegistrationDataModel): Result<String> {
        return try {
            val ownerUid = data.ownerUid
            val restaurantId = if (data.restaurantId.isNotEmpty()) data.restaurantId else ownerUid
            Log.i("RegistrationRepo", "Saving restaurant outlet details to 'restaurants/$restaurantId' for Owner: $ownerUid")

            // Upload logo to Firebase Storage if local URI is present
            uploadLogoIfLocalUri(context, restaurantId, data)

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

    private suspend fun uploadLogoIfLocalUri(context: Context, restaurantId: String, data: RegistrationDataModel) {
        val rawLogoUri = data.restaurantLogoUri ?: data.billingPrinterSettings.restaurantLogoUri
        if (!rawLogoUri.isNullOrEmpty() && (rawLogoUri.startsWith("content:") || rawLogoUri.startsWith("file:"))) {
            try {
                // Ensure Firebase Auth session exists for Storage rule evaluation
                if (FirebaseAuth.getInstance().currentUser == null) {
                    try {
                        Log.i("RegistrationRepo", "Signing in anonymously for Firebase Storage access...")
                        FirebaseAuth.getInstance().signInAnonymously().await()
                    } catch (authException: Exception) {
                        Log.w("RegistrationRepo", "Anonymous auth for Storage failed: ${authException.message}")
                    }
                }

                Log.i("RegistrationRepo", "Uploading local restaurant logo to Firebase Storage for ID: $restaurantId...")
                val uri = Uri.parse(rawLogoUri)
                val compressedBytes = ImageUtils.compressImage(context, uri)

                val storage = FirebaseStorage.getInstance()
                val storageRef = storage.reference.child("restaurants/$restaurantId/logo.jpg")

                if (compressedBytes != null) {
                    storageRef.putBytes(compressedBytes).await()
                } else {
                    storageRef.putFile(uri).await()
                }

                val downloadUrl = storageRef.downloadUrl.await().toString()

                Log.i("RegistrationRepo", "Logo uploaded successfully to Firebase Storage. Download URL: $downloadUrl")
                data.restaurantLogoUri = downloadUrl
                data.billingPrinterSettings.restaurantLogoUri = downloadUrl
            } catch (e: Exception) {
                Log.e("RegistrationRepo", "Failed to upload logo to Firebase Storage. Note: Check Firebase Console -> Storage -> Rules to allow write access.", e)
                data.restaurantLogoUri = null
                data.billingPrinterSettings.restaurantLogoUri = null
            }
        }
    }
}
