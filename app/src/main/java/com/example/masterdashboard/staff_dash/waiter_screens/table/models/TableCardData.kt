package com.example.masterdashboard.staff_dash.waiter_screens.table.models

data class TableCardData(

    val tableId: String = "",
    val floorId: String = "",
    val totalSeats: Int = 0,
    val status: TableStatus = TableStatus.FREE,
    val price: String? = null
)

enum class TableStatus{
    FREE, OCCUPIED, RESERVED, BILLING
}