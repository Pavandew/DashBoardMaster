package com.example.masterdashboard.staff_dash.kitchen_screens.repo

import android.util.Log
import com.example.masterdashboard.staff_dash.kitchen_screens.model.InventoryItem
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class KitchenInventoryRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    companion object {
        private const val TAG = "KitchenInventoryRepo"
    }

    fun getInventoryItems(restaurantId: String): Flow<List<InventoryItem>> = callbackFlow {
        if (restaurantId.isEmpty()) {
            Log.e(TAG, "getInventoryItems: restaurantId is empty!")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        Log.d(TAG, "getInventoryItems: Fetching for $restaurantId")
        val listenerRegistration = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(restaurantId)
            .collection(AppConstants.COLLECTION_INVENTORY)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getInventoryItems: Error listening to inventory", error)
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(InventoryItem::class.java) ?: emptyList()
                Log.d(TAG, "getInventoryItems: Received ${items.size} items")
                trySend(items)
            }
        awaitClose { 
            Log.d(TAG, "getInventoryItems: Closing listener")
            listenerRegistration.remove() 
        }
    }

    suspend fun addInventoryItem(restaurantId: String, item: InventoryItem) {
        if (restaurantId.isEmpty()) {
            Log.e(TAG, "addInventoryItem: Cannot add item, restaurantId is empty")
            return
        }
        
        try {
            Log.d(TAG, "addInventoryItem: Adding ${item.itemName} to $restaurantId")
            firestore.collection(AppConstants.COLLECTION_USERS)
                .document(restaurantId)
                .collection(AppConstants.COLLECTION_INVENTORY)
                .add(item)
                .await()
            Log.d(TAG, "addInventoryItem: Successfully added ${item.itemName}")
        } catch (e: Exception) {
            Log.e(TAG, "addInventoryItem: Error adding item", e)
            throw e
        }
    }

    suspend fun updateInventoryItem(restaurantId: String, item: InventoryItem) {
        if (restaurantId.isEmpty() || item.inventoryId.isEmpty()) {
            Log.e(TAG, "updateInventoryItem: Missing ID(s). ResId: $restaurantId, ItemId: ${item.inventoryId}")
            return
        }

        try {
            Log.d(TAG, "updateInventoryItem: Updating ${item.itemName} ($restaurantId)")
            firestore.collection(AppConstants.COLLECTION_USERS)
                .document(restaurantId)
                .collection(AppConstants.COLLECTION_INVENTORY)
                .document(item.inventoryId)
                .set(item)
                .await()
            Log.d(TAG, "updateInventoryItem: Successfully updated ${item.itemName}")
        } catch (e: Exception) {
            Log.e(TAG, "updateInventoryItem: Error updating item", e)
            throw e
        }
    }

    suspend fun deleteInventoryItem(restaurantId: String, inventoryId: String) {
        if (restaurantId.isEmpty() || inventoryId.isEmpty()) {
            Log.e(TAG, "deleteInventoryItem: Missing ID(s)")
            return
        }

        try {
            Log.d(TAG, "deleteInventoryItem: Deleting $inventoryId from $restaurantId")
            firestore.collection(AppConstants.COLLECTION_USERS)
                .document(restaurantId)
                .collection(AppConstants.COLLECTION_INVENTORY)
                .document(inventoryId)
                .delete()
                .await()
            Log.d(TAG, "deleteInventoryItem: Successfully deleted item")
        } catch (e: Exception) {
            Log.e(TAG, "deleteInventoryItem: Error deleting item", e)
            throw e
        }
    }
}
