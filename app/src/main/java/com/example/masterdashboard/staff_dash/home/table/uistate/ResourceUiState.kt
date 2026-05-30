package com.example.masterdashboard.staff_dash.home.table.uistate

sealed class ResourceUiState<out T> {

    object  Loading: ResourceUiState<Nothing>()
    data class Success<T>(val data: T): ResourceUiState<T>()
    data class Error(val message: String) : ResourceUiState<Nothing>()

}