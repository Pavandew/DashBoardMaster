package com.example.masterdashboard.manager_single_res_dash.models

import android.app.Activity
import androidx.fragment.app.Fragment

data class DrawerMenuItem(
    val id: Int,
    val title: String,
    val iconRes: Int,
    val fragmentClass: Class<out Fragment>? = null,
    val activityClass: Class<out Activity>? = null, // New field for activity navigation
    val badgeCount: Int = 0,
    val isLogout: Boolean = false
)
