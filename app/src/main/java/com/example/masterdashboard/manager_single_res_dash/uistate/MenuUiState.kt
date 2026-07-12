package com.example.masterdashboard.manager_single_res_dash.uistate

import com.example.masterdashboard.manager_single_res_dash.models.MenuCategory
import com.example.masterdashboard.manager_single_res_dash.models.MenuFoodItemsData

sealed interface MenuUiState {
    object Loading: MenuUiState
    data class Success(val menuItems: List<MenuCategory>) : MenuUiState
    object Empty: MenuUiState
    data class Error(val message: String) : MenuUiState
}

sealed interface MenuItemUiState {
    object Loading : MenuItemUiState
    data class Success(val foodList: List<MenuFoodItemsData>) : MenuItemUiState
    object Empty : MenuItemUiState
    data class Error(val message: String) : MenuItemUiState
}