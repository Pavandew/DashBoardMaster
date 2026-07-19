package com.example.masterdashboard.staff_dash.waiter_screens.table.repo

import android.util.Log
import com.example.masterdashboard.login.utils.AppConstants
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuCategoryData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

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

                            singleCategoryFoodList.add(
                                FoodItemData(id = id, name = name, price = price, imageUrl = imageUrl, categoryId = categoryId)
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
}