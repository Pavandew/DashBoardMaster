package com.example.masterdashboard.master_dash.dashboard.utils

import com.example.masterdashboard.R
import com.example.masterdashboard.master_dash.dashboard.model.DashboardCardModel
import com.example.masterdashboard.master_dash.dashboard.model.QuickActionModel

object DashboardUiData {

    val statsCards = listOf(

        DashboardCardModel(
            title = "Total Restaurants",
            subtitle = "All Registered",
            icon = R.drawable.ic_restaurant_24dp,
            iconColor = R.color.green,
            bgColor = R.color.light_green
        ),

        DashboardCardModel(
            title = "Active Restaurants",
            subtitle = "Currently Running",
            icon = R.drawable.ic_add_circle_24dp,
            iconColor = R.color.green,
            bgColor = R.color.light_blue
        ),

        DashboardCardModel(
            title = "Disabled Restaurants",
            subtitle = "Temporarily Closed",
            icon = R.drawable.ic_disabled_24dp,
            iconColor = R.color.red,
            bgColor = R.color.light_orange
        ),

        DashboardCardModel(
            title = "Total Admins",
            subtitle = "System Managers",
            icon = R.drawable.ic_person_24dp,
            iconColor = R.color.red,
            bgColor = R.color.light_red
        )
    )

    val quickActions = listOf(

        QuickActionModel(
            title = "Create\nRestaurant",
            icon = R.drawable.ic_add_24dp,
            bgColor = R.color.light_blue
        ),

        QuickActionModel(
            title = "All\nRestaurants",
            icon = R.drawable.ic_restaurant_24dp,
            bgColor = R.color.green
        ),

        QuickActionModel(
            title = "Recent\nLogs",
            icon = R.drawable.ic_logs_24dp,
            bgColor = R.color.red
        ),

        QuickActionModel(
            title = "Open\nSettings",
            icon = R.drawable.ic_settings_24dp,
            bgColor = R.color.red
        )
    )
}