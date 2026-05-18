package com.example.masterdashboard.home.create_res.models

sealed class CreateRestaurantItem {

    data class SectionTitle(
        val sectionNumber: String,
        val title: String
    ) : CreateRestaurantItem()

    data class InputField(
        val label: String,
        val hint: String,
        val inputType: Int,
        val isPassword: Boolean = false
    ) : CreateRestaurantItem()

    data object LocationSection : CreateRestaurantItem()

    data object UploadSection : CreateRestaurantItem()

    data class PermissionSection(
        val permissions: List<String>
    ) : CreateRestaurantItem()

    data class ButtonSection(
        val saveText: String,
        val cancelText: String
    ) : CreateRestaurantItem()
}