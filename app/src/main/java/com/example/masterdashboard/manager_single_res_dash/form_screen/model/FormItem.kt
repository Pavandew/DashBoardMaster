package com.example.masterdashboard.manager_single_res_dash.form_screen.model

sealed class FormItem {
    data class StepProgress(
        val step: String,
        val title: String,
        val subTitle: String
    ) : FormItem()

    data class InfoCard(
        val message: String
    ) : FormItem()

    data class SectionHeader(
        val title: String,
        val subTitle: String = "",
        val isOptional: Boolean = false,
        val sectionNumber: String = "!"
    ) : FormItem()

    data class SwitchField(
        val key: String,
        val title: String,
        val subTitle: String = "",
        var isChecked: Boolean = false
    ) : FormItem()

    data class DatePickerField(
        val key: String,
        val label: String,
        val hint: String,
        var value: String = "",
        val helperText: String? = null,
        var error: String? = null
    ) : FormItem()

    data class DropdownField(
        val key: String,
        val label: String,
        val hint: String,
        val options: List<String>,
        var selectedValue: String = "",
        var error: String? = null
    ) : FormItem()

    data class UploadField(
        val key: String,
        val title: String,
        val subTitle: String = "",
        var imageUri: String? = null
    ) : FormItem()

    data class ReviewHeader(
        val name: String,
        val type: String,
        val status: String = "Everything looks good. You're ready to launch."
    ) : FormItem()

    data class ReviewCard(
        val title: String,
        val details: List<Pair<String, String>>,
        val onEditClick: () -> Unit
    ) : FormItem()

    data class PhoneInputField(
        val key: String,
        val label: String,
        val hint: String,
        val codes: List<String>,
        var selectedCode: String = "+91",
        var phoneNumber: String = "",
        var error: String? = null
    ) : FormItem()

    data class InputField(
        val key: String,
        val label: String,
        val hint: String,
        val inputType: Int,
        val isPassword: Boolean = false,
        var value: String = "",
        val helperText: String? = null,
        var error: String? = null
    ) : FormItem()
}