package com.example.masterdashboard.manager_single_res_dash.uistate

import com.example.masterdashboard.manager_single_res_dash.models.DrawerMenuItem
import com.example.masterdashboard.subscription.models.SubscriptionStatus

data class DrawerUiState(
    val userName: String = "User",
    val displayRole: String = "Owner",
    val restaurantName: String = "",
    val trialDaysRemaining: Int = 30,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.TRIAL,
    val unreadNotificationsCount: Int = 0,
    val menuItems: List<DrawerMenuItem> = emptyList()
)
