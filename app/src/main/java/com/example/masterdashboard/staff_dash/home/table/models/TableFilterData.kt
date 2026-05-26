package com.example.masterdashboard.staff_dash.home.table.models

data class TableFilterData(
    val id: String = "",
    val name: String = "",
    var isSelected: Boolean = false     // UI state (not stored in DB
 )
