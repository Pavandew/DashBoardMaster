package com.example.masterdashboard.staff_dash.waiter_screens.table.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuCategoryData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderDataModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.manager_single_res_dash.models.ItemVariant
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
                val name = doc.getString(AppConstants.FIELD_CATEGORY_NAME) ?: "Unnamed Category"

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
                val categoryName = catDoc.getString(AppConstants.FIELD_CATEGORY_NAME) ?: "Unknown"

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
                            val name = doc.getString(AppConstants.FIELD_ITEM_NAME) ?: "Unnamed Dish"

                            // Price may be stored as a Number or a String in Firestore. Handle both cases.
                            val rawPrice = doc.get(AppConstants.FIELD_ITEM_PRICE)
                            val price = when (rawPrice) {
                                is Number -> rawPrice.toInt()
                                is String -> {
                                    // Strip currency symbols and non-numeric characters, then parse
                                    val cleaned = rawPrice.replace("[^0-9.]".toRegex(), "")
                                    cleaned.toDoubleOrNull()?.toInt() ?: 0
                                }
                                else -> 0
                            }

                            val imageUrl = doc.getString(AppConstants.FIELD_ITEM_IMAGE) ?: ""
                            val isVeg = doc.getBoolean(AppConstants.FIELD_IS_VEG) ?: true
                            val hasVariants = doc.getBoolean(AppConstants.FIELD_HAS_VARIANTS) ?: false
                            
                            // Map variants if they exist
                            val rawVariants = doc.get(AppConstants.FIELD_VARIANTS) as? List<Map<String, Any>>
                            val variantsList = rawVariants?.map { vMap ->
                                ItemVariant(
                                    variantName = vMap[AppConstants.FIELD_VARIANT_NAME] as? String ?: "",
                                    price = (vMap[AppConstants.FIELD_ITEM_PRICE]?.toString()?.toDoubleOrNull() ?: 0.0)
                                )
                            } ?: emptyList()

                            // FIX: If the item has variants but the base price is 0, 
                            // use the minimum variant price as the default display price.
                            val finalPrice = if (hasVariants && price == 0 && variantsList.isNotEmpty()) {
                                variantsList.minOf { it.price }.toInt()
                            } else {
                                price
                            }

                            singleCategoryFoodList.add(
                                FoodItemData(
                                    id = id,
                                    name = name,
                                    price = finalPrice,
                                    imageUrl = imageUrl,
                                    categoryId = categoryId,
                                    categoryName = categoryName,
                                    isVeg = isVeg,
                                    hasVariants = hasVariants,
                                    variantsList = variantsList
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
     * Finds the first active order for a specific table.
     * Returns Pair(DocumentID, OrderModel)
     */
    suspend fun getActiveOrderForTable(
        managerId: String,
        floorId: String,
        tableId: String
    ): Pair<String, OrderDataModel>? {
        return try {
            val snapshot = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_RES_FLOORS)
                .document(floorId)
                .collection(AppConstants.COLLECTION_TABLES)
                .document(tableId)
                .collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
                .limit(1)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
            val model = doc?.toObject(OrderDataModel::class.java)
            if (doc != null && model != null) {
                doc.id to model
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "📦 [REPO] Error finding active order for table", e)
            null
        }
    }

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
     * Fetches FCM tokens for all staff members with 'chef' or 'kitchen' roles
     * for a specific restaurant.
     */
    suspend fun getChefTokens(managerId: String): List<String> {
        return try {
            val snapshot = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_STAFF)
                .whereIn(AppConstants.FIELD_ROLE, listOf("chef", "kitchen", "Chef", "Kitchen"))
                .get()
                .await()

            snapshot.documents.mapNotNull { it.getString(AppConstants.FIELD_FCM_TOKEN) }
        } catch (e: Exception) {
            Log.e(TAG, "📦 [REPO] Error fetching chef tokens", e)
            emptyList()
        }
    }

    /**
     * Pushes the KOT payload directly inside the table's sub-collection or a central
     * active_orders collection for counter/cashier orders.
     */
    fun sendOrderToFirebaseKitchen(
        managerId: String?,
        floorId: String?,
        tableId: String?,
        orderData: OrderDataModel,
        existingOrderDocId: String? = null
    ): Flow<ResourceUiState<String>> = callbackFlow {
        trySend(ResourceUiState.Loading)

        if (managerId.isNullOrEmpty()) {
            trySend(ResourceUiState.Error("Missing Manager ID: Cannot place order."))
            close()
            return@callbackFlow
        }

        val isCounterOrder = tableId == "COUNTER_ORDER" || tableId == "N/A" || floorId == "N/A"
        
        val batch = firestore.batch()

        // 1. Resolve the target path based on order type
        val orderRef = if (isCounterOrder) {
            // Cashier/Counter orders go to a central active_orders collection for that manager
            firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
                .let { if (existingOrderDocId.isNullOrEmpty()) it.document() else it.document(existingOrderDocId) }
        } else {
            // Table orders go deep into the floor/table hierarchy
            if (floorId.isNullOrEmpty() || tableId.isNullOrEmpty()) {
                trySend(ResourceUiState.Error("Missing Table/Floor routing: Cannot place table order."))
                close()
                return@callbackFlow
            }

            val tableRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_RES_FLOORS)
                .document(floorId)
                .collection(AppConstants.COLLECTION_TABLES)
                .document(tableId)

            // Stage table status update only for real table orders
            batch.update(tableRef, AppConstants.FIELD_STATUS, AppConstants.STATUS_OCCUPIED)

            tableRef.collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
                .let { if (existingOrderDocId.isNullOrEmpty()) it.document() else it.document(existingOrderDocId) }
        }

        // 2. Stage the order data
        if (!existingOrderDocId.isNullOrEmpty()) {
            batch.set(orderRef, orderData, com.google.firebase.firestore.SetOptions.merge())
        } else {
            // Ensure we never have a blank orderId field in Firestore
            val finalOrder = if (orderData.orderId.isBlank() || orderData.orderId == orderRef.id) {
                // Generate a fallback if ViewModel didn't provide one
                orderData.copy(orderId = "#ORD-${(1000..9999).random()}")
            } else {
                orderData
            }
            batch.set(orderRef, finalOrder)
        }

        // 3. Commit the operation
        batch.commit()
            .addOnSuccessListener {
                Log.i("Order_Flow_Debug", "📦 [REPO] Order successfully saved/updated at: ${orderRef.path}")
                trySend(ResourceUiState.Success(orderRef.path))
                close()
            }
            .addOnFailureListener { exception ->
                Log.e("Order_Flow_Debug", "📦 [REPO] Failed to write order", exception)
                trySend(ResourceUiState.Error(exception.message ?: "Firestore write failure"))
                close(exception)
            }

        awaitClose { }
    }.flowOn(Dispatchers.IO)
}
