package com.example.masterdashboard.manager_single_res_dash.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FloorDataModel(
    @DocumentId
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
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()) // Computed server-side cleanly [cite: 118]
        )
    }
}

data class TableData(
    @DocumentId
    val tableId: String = "",
    val tableName: String = "",
    val capacity: Int = 2,       // FIXED: Added default fallback initialization value to provide a No-Arg Constructor
    val status: String = "AVAILABLE", // FIXED: Provide a default fallback to match cloud properties [cite: 88]
    val gridX: Int = 0,
    val gridY: Int = 0,
    val assignedStaffId: String? = null
) {
    /**
     * Helper to convert table configurations into map blocks safely.
     * Prevents document parsing compilation crashes during updates[cite: 112].
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