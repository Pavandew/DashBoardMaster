package com.restroone.app.staff_dash.waiter_screens.table.models

data class TableFilterData(
    val id: String = "",
    val name: String = "",
    var isSelected: Boolean = false     // UI state (not stored in DB
 )
