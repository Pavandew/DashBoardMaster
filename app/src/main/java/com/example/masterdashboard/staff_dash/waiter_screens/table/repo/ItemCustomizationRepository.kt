package com.example.masterdashboard.staff_dash.waiter_screens.table.repo

import android.util.Log
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.AddonItem
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ItemCustomizationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "ItemCustomRepo"

    /**
     * Fetches optional add-ons for a specific food item from its nested sub-collection.
     */
    suspend fun getAddonsForItem(managerId: String, categoryId: String, itemId: String): List<AddonItem> {
        return try {
            val snapshot = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_MENU_CATEGORIES)
                .document(categoryId)
                .collection(AppConstants.COLLECTION_FOOD_ITEMS)
                .document(itemId)
                .collection(AppConstants.COLLECTION_ADDONS)
                .get()
                .await()

            snapshot.documents.map { doc ->
                AddonItem(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    price = doc.getDouble("price") ?: 0.0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching addons for item: $itemId", e)
            emptyList()
        }
    }
}
