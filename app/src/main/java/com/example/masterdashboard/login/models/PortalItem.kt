package com.example.masterdashboard.login.models

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class PortalFeature(
    @DrawableRes val icon: Int,
    val text: String,
    @ColorRes val color: Int
)

data class PortalItem(
    val title: String,
    val description: String,
    @DrawableRes val mainIcon: Int,
    @ColorRes val themeColor: Int,
    val features: List<PortalFeature>,
    val onClick: () -> Unit
)