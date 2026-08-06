package com.example.masterdashboard.manager_single_res_dash.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.models.MenuCategory
import com.example.masterdashboard.manager_single_res_dash.repo.MenuManagementRepository
import com.example.masterdashboard.manager_single_res_dash.uistate.MenuUiState
import com.example.masterdashboard.login.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MenuManagementViewModel : ViewModel(){
    companion object {
        private const val TAG = "MenuManagementViewModel ----> "
    }

    private val repository = MenuManagementRepository()

    private val _uiState = MutableStateFlow<MenuUiState>(MenuUiState.Loading)
    val uiState : StateFlow<MenuUiState> = _uiState.asStateFlow()

    // Observe Stored Categories
    fun observeCategories(ownerUid: String) {
        Log.d(TAG, "observeCategories called for Owner UID: $ownerUid")
        viewModelScope.launch {
            _uiState.value = MenuUiState.Loading

            repository.getLiveMenuCategories(ownerUid = ownerUid)
                .catch { exception ->
                    Log.e(TAG, "Error while observing menu categories: ${exception.message}", exception)
                    _uiState.value = MenuUiState.Error(exception.message ?: "Failed to synchronize menu records")
                }
                .collect { list ->
                    Log.d(TAG, "Received menu categories update: ${list.size} categories found")
                    if(list.isEmpty()) {
                        Log.w(TAG, "Real-time stream returned an empty category list.")
                        _uiState.value = MenuUiState.Empty
                    } else {
                        list.forEach { category ->
                            Log.v(TAG, "   └─ Category item: [ID: ${category.menuCategoryId} | Name: ${category.menuCategoryName} | Items: ${category.itemCount}]")
                        }
                        _uiState.value = MenuUiState.Success(list)
                    }
                }
        }
    }

    // Adding new Categories
    fun addNewCategory(
        ownerUid: String,
        categoryName: String
    ) {
        Log.i(TAG, "➕ Attempting to add new category: '$categoryName' for Owner: $ownerUid")
        viewModelScope.launch {
            // determine a matching drawable asset string key based on what they typed
            val lowCaseName = categoryName.lowercase()
            val mappedIconAsset = when{

                lowCaseName.contains("pizza") -> "person"
                lowCaseName.contains("pizza") -> "person"
                lowCaseName.contains("burger") -> "person"
                lowCaseName.contains("pasta") -> "person"
                lowCaseName.contains("salad") -> "person"
                lowCaseName.contains("drink") || lowCaseName.contains("beverage") -> "person"
                lowCaseName.contains("sweet") || lowCaseName.contains("desert") -> "person"
                else -> "app_logo"

            }

            // Generate a clean Firestore ID path entry reference pointer
            val firestore = FirebaseFirestore.getInstance()
            val newDocRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .collection(AppConstants.COLLECTION_MENU_CATEGORIES)
                .document()  // Generate unique document ID automatically


            val newCategory = MenuCategory(
                menuCategoryId = newDocRef.id,
                menuCategoryName = categoryName,
                itemCount = 0, // Starts fresh with zero linked food items
                imageResId = mappedIconAsset
            )

            // Save directly to the cloud
            runCatching {
                newDocRef.set(newCategory.toMap())
                    .addOnSuccessListener {
                        Log.i(TAG, "Firebase Write Success! Category successfully synchronized globally: '$categoryName'")
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Firebase Write Failed for category: '$categoryName'", exception)
                    }
            }.onFailure { throwable ->
                Log.e(TAG, "Critical exception thrown during runCatching block setup execution", throwable)
            }
        }
    }

    fun deleteCategoryItem(
        ownerUid: String,
        categoryId: String,
        categoryName: String
    ) {
        Log.i(TAG, "Initiating cascading deletion request for category: '$categoryName'")

        viewModelScope.launch {
            try {
                // Delegate cascading database data mutations safely to the repository layer
                repository.removeCategoryCascading(ownerUid = ownerUid, categoryId = categoryId)
                Log.i(TAG, "Successfully completed cascading deletion for category: '$categoryName'")
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to complete cascading deletion for category: '$categoryName'. Error: ${exception.message}", exception)
            }
        }
    }
}

