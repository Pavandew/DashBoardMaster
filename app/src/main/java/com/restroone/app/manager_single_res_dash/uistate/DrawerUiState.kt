package com.restroone.app.manager_single_res_dash.uistate

import com.restroone.app.manager_single_res_dash.models.DrawerMenuItem
import com.restroone.app.subscription.models.SubscriptionStatus

data class DrawerUiState(
    val userName: String = "User",
    val displayRole: String = "Owner",
    val restaurantName: String = "",
    val trialDaysRemaining: Int = 30,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.TRIAL,
    val unreadNotificationsCount: Int = 0,
    val menuItems: List<DrawerMenuItem> = emptyList()
)
