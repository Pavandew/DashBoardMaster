package com.example.masterdashboard.manager_single_res_dash.viewModel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.models.MenuFoodItemsData
import com.example.masterdashboard.manager_single_res_dash.models.ItemVariant
import com.example.masterdashboard.manager_single_res_dash.repo.MenuManagementRepository
import com.example.masterdashboard.manager_single_res_dash.uistate.MenuItemUiState
import com.example.masterdashboard.manager_single_res_dash.form_screen.uiState.RegistrationUiState
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MenuItemViewModel(application: Application): AndroidViewModel(application) {
    companion object {
        private const val TAG = "MenuItemViewModel ----> "
    }

    private val repository = MenuManagementRepository()

    private val _foodItemsState = MutableStateFlow<MenuItemUiState>(MenuItemUiState.Loading)
    val foodItemsState: StateFlow<MenuItemUiState> = _foodItemsState.asStateFlow()

    private val _saveState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val saveState: StateFlow<RegistrationUiState> = _saveState.asStateFlow()


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
                        _foodItemsState.value = MenuItemUiState.Success(list)
                    }
                }
        }
    }

    fun uploadAndSaveMenuItem(
        ownerUid: String,
        categoryId: String,
        itemName: String,
        price: String,
        description: String,
        status: String,
        isVeg: Boolean,
        imageUri: Uri?,
        hasVariants: Boolean = false,
        variants: List<ItemVariant> = emptyList(),
        itemId: String = "",
        existingImageUrl: String = ""
    ) {
        viewModelScope.launch {
            try {
                _saveState.value = RegistrationUiState.Loading
                
                // 1. Prepare/Generate Item ID first so we can use it for the filename
                val firestore = FirebaseFirestore.getInstance()
                val isNewItem = itemId.isEmpty()
                val finalId = if (isNewItem) {
                    firestore.collection(AppConstants.COLLECTION_USERS).document().id
                } else {
                    itemId
                }

                var finalImageUrl = existingImageUrl
                
                // 2. Handle Image Upload with Compression using the unique finalId
                if (imageUri != null) {
                    Log.d(TAG, "New image detected. Starting compressed upload for $finalId...")
                    val uploadResult = repository.uploadMenuImage(getApplication(), ownerUid, imageUri, finalId)
                    if (uploadResult.isSuccess) {
                        finalImageUrl = uploadResult.getOrNull() ?: existingImageUrl
                    } else {
                        _saveState.value = RegistrationUiState.Error("Image upload failed")
                        return@launch
                    }
                }

                // 3. Prepare the item model with the link to the image
                val itemToSave = MenuFoodItemsData(
                    id = finalId,
                    itemName = itemName,
                    categoryName = "",
                    price = price,
                    description = description,
                    status = status,
                    isVeg = isVeg,
                    imageUrl = finalImageUrl,
                    hasVariants = hasVariants,
                    variants = variants
                )

                // 4. Save everything to Firestore
                repository.saveMenuFoodItemTransactional(ownerUid, categoryId, itemToSave, isNewItem)
                
                Log.i(TAG, "Save operation completed successfully for item: $itemName")
                _saveState.value = RegistrationUiState.Success("Item saved successfully", finalId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save item", e)
                _saveState.value = RegistrationUiState.Error("Failed to save item")
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
                repository.removeFoodItemTransactional(ownerUid, categoryId, foodItemId)
                Log.i(TAG, "Successfully completed deletion sequence for item: '$itemName'")
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to complete deletion sequence for item: '$itemName'. Error: ${exception.message}", exception)
            }
        }
    }
}
