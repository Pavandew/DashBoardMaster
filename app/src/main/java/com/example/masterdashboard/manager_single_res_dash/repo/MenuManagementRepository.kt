package com.example.masterdashboard.manager_single_res_dash.repo

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.masterdashboard.manager_single_res_dash.models.MenuCategory
import com.example.masterdashboard.manager_single_res_dash.models.MenuFoodItemsData
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MenuManagementRepository {

    companion object {
        private const val TAG = "MenuManagementRepository ----> "
    }

    private val firestore = FirebaseFirestore.getInstance()
    // Explicitly initialize with bucket name to avoid 404 mismatch
    private val storage = FirebaseStorage.getInstance("gs://masterdashboard-836dd.firebasestorage.app")

    /**
     * Uploads a menu item image to Firebase Storage after compression.
     * Uses the itemId as the filename to handle "Replace" logic cleanly.
     */
    suspend fun uploadMenuImage(context: Context, ownerUid: String, imageUri: Uri, itemId: String): Result<String> {
        if (ownerUid.isEmpty() || itemId.isEmpty()) {
            return Result.failure(Exception("OwnerUID or ItemID is empty. Cannot generate path."))
        }

        return try {
            val fullPath = "users/$ownerUid/menu_images/item_${itemId}.jpg"
            Log.i(TAG, "Initiating compressed upload to path: $fullPath")
            
            val compressedBytes = ImageUtils.compressImage(context, imageUri)
            if (compressedBytes == null) {
                Log.e(TAG, "Compression failed for image: $imageUri")
                return Result.failure(Exception("Failed to process image"))
            }

            val storageRef = storage.reference.child(fullPath)

            Log.d(TAG, "Starting byte upload... (${compressedBytes.size / 1024} KB)")
            storageRef.putBytes(compressedBytes).await()
            
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.i(TAG, "Upload Successful! URL: $downloadUrl")
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Storage Upload Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // listens to real-time additions/removals of menu categories from firebase
    fun getLiveMenuCategories(ownerUid: String) : Flow<List<MenuCategory>> = callbackFlow {

        val query = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_MENU_CATEGORIES)
            .orderBy("menuCategoryName", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshots, exception ->
            if(exception != null) {
                close(exception)
                return@addSnapshotListener
            }

            if( snapshots != null) {
                val categories = snapshots.toObjects(MenuCategory::class.java)
                trySend(categories)
            }
        }

        // keep the channel open until the view's coroutine lifecycle scope is destroyed
        awaitClose { listener.remove() }
    }

    fun getLiveFoodItems(ownerUid: String, categoryId: String): Flow<List<MenuFoodItemsData>> = callbackFlow {

        val query = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_MENU_CATEGORIES)
            .document(categoryId)
            .collection(AppConstants.COLLECTION_FOOD_ITEMS)
            .orderBy("itemName", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshots, exception ->
            if(exception != null) {
                close(exception)
                return@addSnapshotListener
            }

            if(snapshots != null) {
                val items = snapshots.toObjects(MenuFoodItemsData::class.java)
                trySend(items)
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * Executes a cascading batch deletion to completely erase a menu category document
     * along with all its nested food items from the subcollection.
     */
    suspend fun removeCategoryCascading(ownerUid: String, categoryId: String) {
        Log.i(TAG, "removeCategoryCascading transaction initiated for Category ID: $categoryId")

        val categoryDocRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_MENU_CATEGORIES)
            .document(categoryId)

        val foodItemsCollectionRef = categoryDocRef.collection(AppConstants.COLLECTION_FOOD_ITEMS)
        val foodItemsSnapshot = foodItemsCollectionRef.get().await()

        val batch = firestore.batch()

        if (!foodItemsSnapshot.isEmpty) {
            Log.d(TAG, "Found ${foodItemsSnapshot.size()} nested food items to delete inside Category ID: $categoryId")
            for (itemDoc in foodItemsSnapshot.documents) {
                batch.delete(itemDoc.reference)
            }
        }

        batch.delete(categoryDocRef)
        batch.commit().await()
        Log.i(TAG, "✅ Cascading write batch committed. Category and nested food items deleted completely.")
    }

    /**
     * Deletes a single food item document from a category's nested subcollection.
     * Uses a Firestore Transaction to safely decrement the parent category's itemCount.
     */
    suspend fun removeFoodItemTransactional(ownerUid: String, categoryId: String, foodItemId: String) {
        Log.i(TAG, "removeFoodItemTransactional initiated for Item ID: $foodItemId under Category ID: $categoryId")

        val categoryDocRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_MENU_CATEGORIES)
            .document(categoryId)

        val foodItemDocRef = categoryDocRef.collection(AppConstants.COLLECTION_FOOD_ITEMS)
            .document(foodItemId)

        firestore.runTransaction { transaction ->
            val categorySnapshot = transaction.get(categoryDocRef)
            val currentCount = categorySnapshot.getLong("itemCount") ?: 0

            transaction.delete(foodItemDocRef)

            val newCount = if (currentCount > 0) currentCount - 1 else 0
            transaction.update(categoryDocRef, "itemCount", newCount)
            null
        }.await()

        Log.i(TAG, "✅ removeFoodItemTransactional completed successfully. Item deleted: $foodItemId")
    }

    /**
     * Saves or updates a food item document. 
     * Uses a transaction to increment itemCount ONLY if it's a brand new item (itemId is empty).
     */
    suspend fun saveMenuFoodItemTransactional(
        ownerUid: String,
        categoryId: String,
        item: MenuFoodItemsData,
        isNewItem: Boolean
    ) {
        Log.i(TAG, "saveMenuFoodItemTransactional initiated for item: ${item.itemName} (New: $isNewItem)")

        val categoryDocRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_MENU_CATEGORIES)
            .document(categoryId)

        val foodItemDocRef = categoryDocRef.collection(AppConstants.COLLECTION_FOOD_ITEMS)
            .document(item.id)

        firestore.runTransaction { transaction ->
            if (isNewItem) {
                val categorySnapshot = transaction.get(categoryDocRef)
                val currentCount = categorySnapshot.getLong("itemCount") ?: 0
                transaction.update(categoryDocRef, "itemCount", currentCount + 1)
            }

            transaction.set(foodItemDocRef, item.toMap())
            null
        }.await()
        
        Log.i(TAG, "✅ saveMenuFoodItemTransactional completed successfully.")
    }
}

