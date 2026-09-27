package com.restroone.app.manager_single_res_dash.models

import com.restroone.app.manager_single_res_dash.adapter.ManagerDashboardAdapter.QuickActionType

data class QuickActionModel(
    val type: QuickActionType,
    val title: String,
    val iconRes: Int,
    val iconColor: Int,
    val strokeColor: Int,
    val bgColor: Int
)
