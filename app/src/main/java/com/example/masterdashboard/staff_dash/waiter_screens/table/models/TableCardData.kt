package com.example.masterdashboard.staff_dash.waiter_screens.table.models

data class TableCardData(
    val tableId: String = "",
    val tableName: String = "",
    val floorId: String = "",
    val totalSeats: Int = 0,
    val status: TableStatus = TableStatus.FREE,
    val price: String? = null,
    val activeOrderDocId: String? = null,
    val activeOrderId: String? = null
)

enum class TableStatus{
    FREE, OCCUPIED, RESERVED, BILLING
}