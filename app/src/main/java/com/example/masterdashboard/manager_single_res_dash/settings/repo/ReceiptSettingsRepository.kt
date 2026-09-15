package com.example.masterdashboard.manager_single_res_dash.settings.repo

import android.util.Log
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.BillingPrinterSettings
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.example.masterdashboard.utils.RestaurantPathHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ReceiptSettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "ReceiptSettingsRepo"
    }

    /**
     * Fetches Receipt/Billing Printer Settings from Firestore for a given owner/restaurant ID.
     */
    suspend fun getReceiptSettings(ownerUid: String): Result<BillingPrinterSettings> {
        return try {
            Log.d(TAG, "Fetching receipt settings for restaurant/user: $ownerUid")
            val docRef = RestaurantPathHelper.getRestaurantDocRef(ownerUid)
            val doc = docRef.get().await()

            val regModel = doc.toObject(RegistrationDataModel::class.java)
            val settings = regModel?.getEffectiveBillingPrinterSettings() ?: BillingPrinterSettings()
            
            Log.i(TAG, "Receipt settings loaded: prefix=${settings.invoicePrefix}, size=${settings.printSize}")
            Result.success(settings)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching receipt settings from Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Saves Receipt/Billing Printer Settings to Firestore as a nested map and syncs flat fields.
     */
    suspend fun saveReceiptSettings(ownerUid: String, settings: BillingPrinterSettings): Result<Unit> {
        return try {
            Log.d(TAG, "Saving billingPrinterSettings map to Firestore for restaurant/user: $ownerUid")
            val updates = mapOf(
                "billingPrinterSettings" to settings.toMap(),
                "invoicePrefix" to settings.invoicePrefix,
                "startingInvoiceNumber" to settings.startingInvoiceNumber,
                "printSize" to settings.printSize,
                "currency" to settings.currency,
                "currencySymbol" to settings.currencySymbol,
                "showLogoOnReceipts" to settings.showLogoOnReceipts
            )

            val docRef = RestaurantPathHelper.getRestaurantDocRef(ownerUid)
            docRef.set(updates, SetOptions.merge()).await()

            Log.i(TAG, "Receipt settings map saved successfully to ${docRef.path}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating billingPrinterSettings map in Firestore", e)
            Result.failure(e)
        }
    }
}
