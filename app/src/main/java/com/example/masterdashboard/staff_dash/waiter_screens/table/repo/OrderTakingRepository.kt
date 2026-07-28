package com.example.masterdashboard.staff_dash.waiter_screens.table.repo

import android.util.Log
import com.example.masterdashboard.login.utils.AppConstants
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuCategoryData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderDataModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

class OrderTakingRepository {

    companion object {
        private const val TAG = "Order_Flow_Debug"
    }

    private val firestore = FirebaseFirestore.getInstance()

    // 1. Live stream of categories from users/{managerId}/menu_categories
    fun getMenuCategories(managerId: String?): Flow<List<MenuCategoryData>> = callbackFlow {
        Log.d(TAG, "📦 [REPO] getMenuCategories() called for Manager ID: $managerId")

        if (managerId.isNullOrEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val categoriesRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_MENU_CATEGORIES)

        val listener = categoriesRef.addSnapshotListener { snapshots, exception ->
            if (exception != null) {
                Log.e(TAG, "📦 [REPO] Categories snapshot fetch error", exception)
                close(exception)
                return@addSnapshotListener
            }

            val categoryList = mutableListOf<MenuCategoryData>()
            categoryList.add(MenuCategoryData(id = "ALL_ITEMS", name = "All", isSelected = true))

            snapshots?.documents?.forEach { doc ->
                val id = doc.id
                val name = doc.getString("menuCategoryName") ?: "Unnamed Category"

                if (name.lowercase() != "all") {
                    categoryList.add(MenuCategoryData(id = id, name = name, isSelected = false))
                }
            }
            trySend(categoryList)
        }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    // 2. FIX: Deep-fetch menu items from the nested paths: menu_categories -> {categoryId} -> menu_items
    fun getFoodMenu(managerId: String?): Flow<List<FoodItemData>> = callbackFlow {
        Log.d(TAG, "📦 [REPO] getFoodMenu() nested snapshot loop started for Manager ID: $managerId")

        if (managerId.isNullOrEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val activeListeners = mutableListOf<ListenerRegistration>()
        val foodItemsMap = mutableMapOf<String, List<FoodItemData>>()

        // Step A: Target the parent menu_categories collection first
        val categoriesRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_MENU_CATEGORIES)

        val masterCategoriesListener = categoriesRef.addSnapshotListener { categorySnapshots, catException ->
            if (catException != null) {
                Log.e(TAG, "📦 [REPO] Error resolving parent categories for items", catException)
                return@addSnapshotListener
            }

            // Clear old child sub-listeners if categories structural layout changes
            activeListeners.forEach { it.remove() }
            activeListeners.clear()

            val categoryDocs = categorySnapshots?.documents ?: emptyList()
            if (categoryDocs.isEmpty()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            // Step B: Loop through every category to open up child menu_items listeners
            categoryDocs.forEach { catDoc ->
                val categoryId = catDoc.id
                val categoryName = catDoc.getString("menuCategoryName") ?: "Unknown"

                // Explicit nested target path: menu_categories/{categoryId}/menu_items
                val itemsRef = categoriesRef.document(categoryId).collection(AppConstants.COLLECTION_FOOD_ITEMS)

                val itemListener = itemsRef.addSnapshotListener { itemSnapshots, itemException ->
                    if (itemException != null) {
                        Log.e(TAG, "📦 [REPO] Error loading nested items for category: $categoryId")
                        return@addSnapshotListener
                    }

                    val singleCategoryFoodList = mutableListOf<FoodItemData>()

                    itemSnapshots?.documents?.forEach { doc ->
                        try {
                            val id = doc.id
                            val name = doc.getString("itemName") ?: "Unnamed Dish"

                            // Price may be stored as a Number or a String in Firestore. Handle both cases.
                            val rawPrice = doc.get("price")
                            val price = when (rawPrice) {
                                is Number -> rawPrice.toInt()
                                is String -> {
                                    // Strip currency symbols and non-numeric characters, then parse
                                    val cleaned = rawPrice.replace("[^0-9.]".toRegex(), "")
                                    cleaned.toDoubleOrNull()?.toInt() ?: 0
                                }
                                else -> 0
                            }

                            val imageUrl = doc.getString("imageUrl") ?: ""
                            val isVeg = doc.getBoolean("isVeg") ?: true

                            singleCategoryFoodList.add(
                                FoodItemData(
                                    id = id,
                                    name = name,
                                    price = price,
                                    imageUrl = imageUrl,
                                    categoryId = categoryId,
                                    categoryName = categoryName,
                                    isVeg = isVeg
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "📦 [REPO] Failure parsing nested item object doc: ${doc.id}", e)
                        }
                    }

                    // Save this category's updated list into our map tracking matrix
                    foodItemsMap[categoryId] = singleCategoryFoodList

                    // Step C: Flatten map into a single comprehensive menu list and send it to the UI
                    val combinedMasterMenuList = foodItemsMap.values.flatten()
                    Log.i(TAG, "📦 [REPO] Pushing updated nested master menu list size: ${combinedMasterMenuList.size} items to UI.")
                    trySend(combinedMasterMenuList)
                }

                activeListeners.add(itemListener)
            }
        }

        // Clean up everything when the user leaves the fragment screen
        awaitClose {
            Log.d(TAG, "📦 [REPO] Cleaning nested food item listeners.")
            masterCategoriesListener.remove()
            activeListeners.forEach { it.remove() }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Fetches a single order document to resume or add items to it.
     */
    suspend fun getExistingOrder(
        managerId: String,
        floorId: String,
        tableId: String,
        orderDocId: String
    ): OrderDataModel? {
        return try {
            firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_RES_FLOORS)
                .document(floorId)
                .collection(AppConstants.COLLECTION_TABLES)
                .document(tableId)
                .collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
                .document(orderDocId)
                .get()
                .await()
                .toObject(OrderDataModel::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "📦 [REPO] Error fetching existing order", e)
            null
        }
    }

    /**
     * Pushes the KOT payload directly inside the EXACT existing table's sub-collection
     * and updates its status to OCCUPIED atomically using a Write Batch.
     */
    fun sendOrderToFirebaseKitchen(
        managerId: String?,
        floorId: String?,
        tableId: String?,
        orderData: OrderDataModel,
        existingOrderDocId: String? = null
    ): Flow<ResourceUiState<Boolean>> = callbackFlow {
        trySend(ResourceUiState.Loading)

        if (managerId.isNullOrEmpty() || floorId.isNullOrEmpty() || tableId.isNullOrEmpty()) {
            trySend(ResourceUiState.Error("Missing routing credentials: Cannot place order."))
            close()
            return@callbackFlow
        }

        val batch = firestore.batch()

        // 1. Target the EXACT existing table document path
        val tableRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_RES_FLOORS)
            .document(floorId)
            .collection(AppConstants.COLLECTION_TABLES)
            .document(tableId)

        // 2. Determine if we are updating an existing order or creating a new one
        val orderRef = if (!existingOrderDocId.isNullOrEmpty()) {
            tableRef.collection(AppConstants.COLLECTION_ACTIVE_ORDERS).document(existingOrderDocId)
        } else {
            tableRef.collection(AppConstants.COLLECTION_ACTIVE_ORDERS).document()
        }

        // Stage the order creation/update
        if (!existingOrderDocId.isNullOrEmpty()) {
            // For updates, we use update to merge fields. 
            // Note: In a production app, you might want to use FieldValue.arrayUnion 
            // for items if you aren't sending the full merged list from the ViewModel.
            batch.set(orderRef, orderData, com.google.firebase.firestore.SetOptions.merge())
        } else {
            batch.set(orderRef, orderData)
        }

        // 3. Stage the status update on the existing table to lock it as occupied
        batch.update(tableRef, "status", "OCCUPIED")

        // Commit both operations together safely
        batch.commit()
            .addOnSuccessListener {
                Log.i("Order_Flow_Debug", "📦 [REPO] Order saved/updated successfully for Table $tableId.")
                trySend(ResourceUiState.Success(true))
                close()
            }
            .addOnFailureListener { exception ->
                Log.e("Order_Flow_Debug", "📦 [REPO] Failed to write order to path", exception)
                trySend(ResourceUiState.Error(exception.message ?: "Firestore batch execution failure"))
                close(exception)
            }

        awaitClose { }
    }.flowOn(Dispatchers.IO)
}