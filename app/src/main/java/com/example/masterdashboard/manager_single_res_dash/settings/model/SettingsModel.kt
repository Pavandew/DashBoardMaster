package com.example.masterdashboard.manager_single_res_dash.settings.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class SettingsOption(
    val id: String,
    val title: String,
    var subtitle: String,
    @DrawableRes val iconRes: Int,
    val isDestructive: Boolean = false
)

data class SettingsCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
    @ColorRes val iconTintRes: Int,
    val options: List<SettingsOption>
)
