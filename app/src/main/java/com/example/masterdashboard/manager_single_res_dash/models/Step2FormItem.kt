package com.example.masterdashboard.manager_single_res_dash.models

import android.net.Uri

sealed class Step2FormItem{

    object Header: Step2FormItem()

    data class SectionTitle(
        val title: String,
        val subtitle: String
    ): Step2FormItem()

    data class PermissionItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val iconRes: Int,
        var isChecked: Boolean = false
    ) : Step2FormItem()

    data class DocumentItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val iconRes: Int,
        var isUploaded: Boolean = false,
        var fileUri: Uri? = null // Added path caching tracking reference pointer
    ) : Step2FormItem()
}