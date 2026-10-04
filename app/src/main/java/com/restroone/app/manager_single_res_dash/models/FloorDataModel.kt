package com.restroone.app.manager_single_res_dash.models

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FloorDataModel(
    val floorId: String = "",
    val floorName: String = "",
    val displayFloor: Int = 0,
    val tableCount: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null
) {
    /**
     * Converts data models safely into Map blocks for Firestore writes.
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "floorName" to floorName,
            "displayFloor" to displayFloor,
            "tableCount" to tableCount,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
        )
    }
}

data class TableData(
    val tableId: String = "",
    val tableName: String = "",
    val capacity: Int = 2,
    val status: String = "AVAILABLE",
    val gridX: Int = 0,
    val gridY: Int = 0,
    val assignedStaffId: String? = null
) {
    /**
     * Helper to convert table configurations into map blocks safely.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "tableName" to tableName,
            "capacity" to capacity,
            "status" to status,
            "gridX" to gridX,
            "gridY" to gridY,
            "assignedStaffId" to assignedStaffId
        )
    }
}
