package com.example.masterdashboard.manager_single_res_dash.home.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.home.models.MenuFoodItemsData
import com.example.masterdashboard.manager_single_res_dash.home.repo.MenuManagementRepository
import com.example.masterdashboard.manager_single_res_dash.home.uistate.MenuItemUiState
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MenuItemViewModel: ViewModel() {
    companion object {
        private const val TAG = "MenuItemViewModel ----> "
    }

    private val repository = MenuManagementRepository()

    private val _foodItemsState = MutableStateFlow<MenuItemUiState>(MenuItemUiState.Loading)
    val foodItemsState: StateFlow<MenuItemUiState> = _foodItemsState.asStateFlow()


    fun observeFoodItems(ownerUid: String, categoryId: String) {
        Log.d(TAG, "Loading food items for Category ID: $categoryId")
        viewModelScope.launch {
            _foodItemsState.value = MenuItemUiState.Loading

            repository.getLiveFoodItems(ownerUid, categoryId)
                .catch { exception ->
                    Log.e(TAG, " Error pulling food items subcollection", exception)
                    _foodItemsState.value = MenuItemUiState.Error(exception.message ?: "Failed to load dishes")
                }
                .collect { list ->
                    Log.d(TAG, "Received dishes snapshot. Total size: ${list.size}")
                    if (list.isEmpty()) {
                        _foodItemsState.value = MenuItemUiState.Empty
                    } else {
                        _foodItemsState.value = MenuItemUiState.Success(list) // Type-safe mapping!
                    }
                }
        }
    }

    // Save a brand_new dish directly into the category's food_items sub-collection
    fun saveMenuFoodItem(
        ownerUid: String,
        categoryId: String,
        itemName: String,
        price: String,
        description: String,
        status: String,
        isVeg: Boolean
    ) {
        Log.i(TAG, "Attempting to save new dish: '$itemName' into Category ID: $categoryId ")
        viewModelScope.launch {
            val firestore = FirebaseFirestore.getInstance()

            // build the nested collection path matching your database scheme
            val categoryDocRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .collection(AppConstants.COLLECTION_MENU_CATEGORIES)
                .document(categoryId)

            val newMenuFoodItemDocRef = categoryDocRef
                .collection(AppConstants.COLLECTION_FOOD_ITEMS)
                .document()

            val newItem = MenuFoodItemsData(
                id = newMenuFoodItemDocRef.id,
                itemName = itemName,
                categoryName = "",
                price = price,
                description = description,
                status = status,
                isVeg = isVeg,
                imageUrl = ""   // Placeholder token for future firebase Storage images
            )

            Log.d(TAG, "Launching network transaction write  for document: ${newMenuFoodItemDocRef.id}")

            // 3. Execute an atomic cloud transaction
            firestore.runTransaction { transaction ->

                // Step 1: Read the current state of the category document
                val categorySnapShot = transaction.get(categoryDocRef)

                // Extract the current value of "itemCount" field safely(default to 0 if missing
                val currentCount = categorySnapShot.getLong("itemCount") ?: 0L
                Log.d(TAG, "Current itemCount value read from Firestore: $currentCount")
                val newCount = currentCount +1

                // Step 2: Write the new Food Item data map
                transaction.set(newMenuFoodItemDocRef, newItem.toMap())

                // step 3: Update the parent category's itemCount parameter field
                transaction.update(categoryDocRef, "itemCount", newCount)

                null // return null to signify transaction work block completion
        }.addOnSuccessListener {
                Log.i(TAG, "Transaction Complete! Saved '$itemName' and incremented parent itemCount successfully.")
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Transaction Failed! Item not saved and counter not modified.", exception)
            }
        }
    }

    fun deleteMenuFoodItem(
        ownerUid: String,
        categoryId: String,
        foodItemId: String,
        itemName: String
    ) {
        Log.i(TAG, "Initiating deletion request for item: '$itemName'")

        viewModelScope.launch {
            try {
                // Delegate transactional database data mutations safely to the repository layer
                repository.removeFoodItemTransactional(ownerUid, categoryId, foodItemId)
                Log.i(TAG, "Successfully completed deletion sequence for item: '$itemName'")
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to complete deletion sequence for item: '$itemName'. Error: ${exception.message}", exception)
            }
        }
    }
}