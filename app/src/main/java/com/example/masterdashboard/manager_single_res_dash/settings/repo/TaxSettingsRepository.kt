package com.example.masterdashboard.manager_single_res_dash.settings.repo

import android.util.Log
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.TaxSettings
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TaxSettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "TaxSettingsRepo"
    }

    /**
     * Fetches Tax Settings from Firestore for a given owner.
     * Uses fallback to legacy flat fields if taxSettings map is not populated yet.
     */
    suspend fun getTaxSettings(ownerUid: String): Result<TaxSettings> {
        return try {
            Log.d(TAG, "Fetching tax settings for user: $ownerUid")
            val doc = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .get()
                .await()

            val regModel = doc.toObject(RegistrationDataModel::class.java)
            val settings = regModel?.getEffectiveTaxSettings() ?: TaxSettings()
            
            Log.i(TAG, "Tax settings loaded successfully: rate=${settings.defaultTaxRate}%, label=${settings.taxLabel}")
            Result.success(settings)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tax settings from Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Saves Tax Settings to Firestore as a nested map and syncs legacy flat fields for compatibility.
     */
    suspend fun saveTaxSettings(ownerUid: String, taxSettings: TaxSettings): Result<Unit> {
        return try {
            Log.d(TAG, "Saving taxSettings map to Firestore for user: $ownerUid")
            val updates = mapOf(
                "taxSettings" to taxSettings.toMap(),
                "chargeTaxOnBills" to taxSettings.chargeTaxOnBills,
                "defaultTaxRate" to taxSettings.defaultTaxRate.toString(),
                "gstNumber" to taxSettings.gstNumber,
                "taxLabel" to taxSettings.taxLabel,
                "priceIncludesTax" to taxSettings.priceIncludesTax
            )

            firestore.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .update(updates)
                .await()

            Log.i(TAG, "Tax settings map saved successfully to Firestore.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating taxSettings map in Firestore", e)
            Result.failure(e)
        }
    }
}
