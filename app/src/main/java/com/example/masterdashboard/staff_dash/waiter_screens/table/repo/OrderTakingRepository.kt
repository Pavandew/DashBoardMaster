package com.example.masterdashboard.staff_dash.waiter_screens.table.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.RestaurantPathHelper
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuCategoryData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderDataModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.manager_single_res_dash.models.ItemVariant
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

class OrderTakingRepository {

    companion object {
        private const val TAG = "Order_Flow_Debug"
    }

    private val firestore = FirebaseFirestore.getInstance()

    // 1. Live stream of categories from restaurants/{managerId}/menu_categories
    fun getMenuCategories(managerId: String?): Flow<List<MenuCategoryData>> = callbackFlow {
        Log.d(TAG, "📦 [REPO] getMenuCategories() called for Manager ID: $managerId")

        if (managerId.isNullOrEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val categoriesRef = RestaurantPathHelper.getOutletDocRef(managerId)
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

            val sortedList = mutableListOf<MenuCategoryData>()
            sortedList.add(categoryList.first())
            sortedList.addAll(categoryList.drop(1).sortedBy { it.name.lowercase() })

            trySend(sortedList)
        }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    // 2. Fetch menu items from nested paths: restaurants/{managerId}/menu_categories/{categoryId}/menu_food_items
    fun getFoodMenu(managerId: String?): Flow<List<FoodItemData>> = callbackFlow {
        Log.d(TAG, "📦 [REPO] getFoodMenu() nested snapshot loop started for Manager ID: $managerId")

        if (managerId.isNullOrEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val activeListeners = mutableListOf<ListenerRegistration>()
        val foodItemsMap = mutableMapOf<String, List<FoodItemData>>()

        val categoriesRef = RestaurantPathHelper.getOutletDocRef(managerId)
            .collection(AppConstants.COLLECTION_MENU_CATEGORIES)

        val masterCategoriesListener = categoriesRef.addSnapshotListener { categorySnapshots, catException ->
            if (catException != null) {
                Log.e(TAG, "📦 [REPO] Error resolving parent categories for items", catException)
                return@addSnapshotListener
            }

            activeListeners.forEach { it.remove() }
            activeListeners.clear()

            val categoryDocs = categorySnapshots?.documents ?: emptyList()
            if (categoryDocs.isEmpty()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            categoryDocs.forEach { catDoc ->
                val categoryId = catDoc.id
                val categoryName = catDoc.getString(AppConstants.FIELD_CATEGORY_NAME) ?: "Unknown"

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

                            val rawPrice = doc.get(AppConstants.FIELD_ITEM_PRICE)
                            val price = when (rawPrice) {
                                is Number -> rawPrice.toInt()
                                is String -> {
                                    val cleaned = rawPrice.replace("[^0-9.]".toRegex(), "")
                                    cleaned.toDoubleOrNull()?.toInt() ?: 0
                                }
                                else -> 0
                            }

                            val imageUrl = doc.getString(AppConstants.FIELD_ITEM_IMAGE) ?: ""
                            val isVeg = doc.getBoolean(AppConstants.FIELD_IS_VEG) ?: true
                            val hasVariants = doc.getBoolean(AppConstants.FIELD_HAS_VARIANTS) ?: false
                            
                            val rawVariants = doc.get(AppConstants.FIELD_VARIANTS) as? List<Map<String, Any>>
                            val variantsList = rawVariants?.map { vMap ->
                                ItemVariant(
                                    variantName = vMap[AppConstants.FIELD_VARIANT_NAME] as? String ?: "",
                                    price = (vMap[AppConstants.FIELD_ITEM_PRICE]?.toString()?.toDoubleOrNull() ?: 0.0)
                                )
                            } ?: emptyList()

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

                    foodItemsMap[categoryId] = singleCategoryFoodList

                    val combinedMasterMenuList = foodItemsMap.values.flatten()
                    Log.i(TAG, "📦 [REPO] Pushing updated nested master menu list size: ${combinedMasterMenuList.size} items to UI.")
                    trySend(combinedMasterMenuList)
                }

                activeListeners.add(itemListener)
            }
        }

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
            val snapshot = RestaurantPathHelper.getOutletDocRef(managerId)
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
            RestaurantPathHelper.getOutletDocRef(managerId)
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
            Log.e(TAG, "📦 [REPO] Error fetching existing order doc", e)
            null
        }
    }

    /**
     * Uploads/updates an order payload to the table's active_orders sub-collection in Firestore.
     */
    fun sendOrderToFirebaseKitchen(
        managerId: String?,
        floorId: String?,
        tableId: String?,
        orderData: OrderDataModel,
        existingOrderDocId: String?
    ): Flow<ResourceUiState<String>> = flow {
        emit(ResourceUiState.Loading)

        if (managerId.isNullOrEmpty() || floorId.isNullOrEmpty() || tableId.isNullOrEmpty()) {
            emit(ResourceUiState.Error("Missing required parameters for order placement"))
            return@flow
        }

        try {
            val tableRef = RestaurantPathHelper.getOutletDocRef(managerId)
                .collection(AppConstants.COLLECTION_RES_FLOORS)
                .document(floorId)
                .collection(AppConstants.COLLECTION_TABLES)
                .document(tableId)

            val activeOrdersRef = tableRef.collection(AppConstants.COLLECTION_ACTIVE_ORDERS)

            val orderDocRef = if (!existingOrderDocId.isNullOrEmpty()) {
                activeOrdersRef.document(existingOrderDocId)
            } else {
                activeOrdersRef.document()
            }

            val finalMap = mutableMapOf<String, Any?>(
                AppConstants.FIELD_ORDER_ID to orderData.orderId,
                AppConstants.FIELD_TABLE_ID to orderData.tableId,
                AppConstants.FIELD_FLOOR_ID to orderData.floorId,
                AppConstants.FIELD_TABLE_NAME to orderData.tableName,
                AppConstants.FIELD_CUSTOMER_NAME to orderData.customerName,
                AppConstants.FIELD_CUSTOMER_MOBILE to orderData.customerMobile,
                AppConstants.FIELD_ORDER_TYPE to orderData.orderType,
                AppConstants.FIELD_ORDER_ITEMS to orderData.items,
                AppConstants.FIELD_SPECIAL_NOTES to orderData.specialNotes,
                AppConstants.FIELD_SUBTOTAL to orderData.subtotal,
                AppConstants.FIELD_GST to orderData.gst,
                AppConstants.FIELD_GRAND_TOTAL to orderData.grandTotal,
                AppConstants.FIELD_ORDER_STATUS to orderData.orderStatus,
                AppConstants.FIELD_PAYMENT_METHOD to orderData.paymentMethod,
                AppConstants.FIELD_RESTAURANT_ID to managerId,
                AppConstants.FIELD_WAITER_ID to orderData.waiterId,
                AppConstants.FIELD_TIMESTAMP to orderData.timestamp,
                AppConstants.FIELD_ORDER_DOC_PATH to orderDocRef.path
            )

            // Save order document
            orderDocRef.set(finalMap, SetOptions.merge()).await()

            // Update parent table status to OCCUPIED and current bill amount
            val tableUpdates = mapOf<String, Any>(
                AppConstants.FIELD_STATUS to AppConstants.STATUS_OCCUPIED,
                AppConstants.FIELD_CURRENT_BILL to "₹${orderData.grandTotal.toInt()}",
                AppConstants.FIELD_CUSTOMER_NAME_TABLE to orderData.customerName
            )
            tableRef.update(tableUpdates).await()

            Log.i(TAG, "✅ sendOrderToFirebaseKitchen: Order saved at ${orderDocRef.path}")
            emit(ResourceUiState.Success(orderDocRef.path))
        } catch (e: Exception) {
            Log.e(TAG, "❌ sendOrderToFirebaseKitchen: Upload failed", e)
            emit(ResourceUiState.Error(e.localizedMessage ?: "Failed to save order"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Fetches FCM tokens of kitchen staff/chefs for a restaurant outlet to send notification.
     */
    suspend fun getChefTokens(managerId: String): List<String> {
        return try {
            val snapshot = RestaurantPathHelper.getOutletDocRef(managerId)
                .collection(AppConstants.COLLECTION_STAFF)
                .whereIn(AppConstants.FIELD_ROLE, listOf("chef", "kitchen", "chef_staff"))
                .get()
                .await()

            val tokens = snapshot.documents.mapNotNull { it.getString(AppConstants.FIELD_FCM_TOKEN) }
            Log.d(TAG, "Found ${tokens.size} chef FCM tokens for restaurant: $managerId")
            tokens
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chef tokens", e)
            emptyList()
        }
    }
}
