package com.example.masterdashboard.home.create_res.viewmodel

import android.text.InputType
import androidx.lifecycle.ViewModel
import com.example.masterdashboard.home.create_res.models.CreateRestaurantItem

class CreateRestaurantViewModel : ViewModel() {

    fun getFormItems(): List<CreateRestaurantItem> {

        return listOf(
            CreateRestaurantItem.SectionTitle(
                sectionNumber = "1",
                title = "Basic Details"
            ),

            CreateRestaurantItem.InputField(
                label = "Restaurant Name *",
                hint = "Enter restaurant name",
                inputType = InputType.TYPE_CLASS_TEXT
            ),

            CreateRestaurantItem.InputField(
                label = "Owner Name *",
                hint = "Enter owner name",
                inputType = InputType.TYPE_CLASS_TEXT
            ),

            CreateRestaurantItem.InputField(
                label = "Email Address *",
                hint = "Enter email address",
                inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            ),

            CreateRestaurantItem.InputField(
                label = "Phone Number *",
                hint = "Enter phone number",
                inputType = InputType.TYPE_CLASS_PHONE
            ),

            CreateRestaurantItem.InputField(
                label = "Username *",
                hint = "Enter username",
                inputType = InputType.TYPE_CLASS_TEXT
            ),

            CreateRestaurantItem.InputField(
                label = "Password *",
                hint = "Enter password",
                inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD,
                isPassword = true
            ),

            CreateRestaurantItem.InputField(
                label = "Confirm Password *",
                hint = "Re-enter password",
                inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD,
                isPassword = true
            ),

            CreateRestaurantItem.SectionTitle(
                sectionNumber = "2",
                title = "Location"
            ),

            CreateRestaurantItem.LocationSection,

            CreateRestaurantItem.SectionTitle(
                sectionNumber = "3",
                title = "Upload Documents"
            ),

            CreateRestaurantItem.UploadSection,

            CreateRestaurantItem.SectionTitle(
                sectionNumber = "4",
                title = "Module Permissions"
            ),

            CreateRestaurantItem.PermissionSection(
                permissions = listOf(
                    "Orders",
                    "Menu",
                    "Staff",
                    "Reports",
                    "Billing",
                    "Settings"
                )
            ),

            CreateRestaurantItem.ButtonSection(
                saveText = "Create Restaurant",
                cancelText = "Cancel"
            )
        )
    }
}