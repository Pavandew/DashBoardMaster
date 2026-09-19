package com.example.masterdashboard.manager_single_res_dash.models

import android.app.Activity
import androidx.fragment.app.Fragment

data class DrawerMenuItem(
    val id: Int,
    val title: String,
    val iconRes: Int,
    val fragmentClass: Class<out Fragment>? = null,
    val activityClass: Class<out Activity>? = null,
    val badgeCount: Int = 0,
    val isLogout: Boolean = false,
    val isHeader: Boolean = false,
    val parentHeaderId: Int? = null,
    var isExpanded: Boolean = false,
    val children: List<DrawerMenuItem> = emptyList()
)
