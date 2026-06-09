package com.example.masterdashboard.manager_single_res_dash.home.models

import androidx.fragment.app.Fragment

data class DrawerMenuItem(
    val id: Int,
    val title: String,
    val iconRes: Int,
    val fragmentClass: Class<out Fragment>?, // Null indicates handling custom non-fragment execution flows like Logout
    val badgeCount: Int = 0,
    val isLogout: Boolean = false
)
