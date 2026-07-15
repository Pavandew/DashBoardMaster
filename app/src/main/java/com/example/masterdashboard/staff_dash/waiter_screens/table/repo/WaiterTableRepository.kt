package com.example.masterdashboard.staff_dash.waiter_screens.table.repo

import android.util.Log
import com.example.masterdashboard.login.utils.AppConstants
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableCardData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class WaiterTableRepository {

    companion object {
        private const val TAG = "Table_Flow_Debug"
    }
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getFloors(): Flow<List<TableFilterData>> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        Log.d(TAG, "📦 [REPO] getFloors() invoked. Current User UID: $currentUid")

        if(currentUid == null) {
            Log.w(TAG, "📦 [REPO] User UID is null. Emitting empty floor list.")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val floorsRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(currentUid)
            .collection(AppConstants.COLLECTION_RES_FLOORS)

        Log.d(TAG, "📦 [REPO] Attaching Firestore SnapshotListener to: users/$currentUid/res_floors")

        val listener = floorsRef.addSnapshotListener { snapshots, exception ->
            if(exception != null) {
                Log.e(TAG, "📦 [REPO] Firestore SnapshotListener encountered an error", exception)
                close(exception)
                return@addSnapshotListener
            }

            val floorList = mutableListOf<TableFilterData>()
            floorList.add(TableFilterData(id = "ALL_FLOORS", name = "All", isSelected = true))

            snapshots?.documents?.forEach { doc ->
                val id = doc.id
                val name = doc.getString("floorName") ?: "Unnamed Floor"

                if(name.lowercase() != "all") {
                    floorList.add(TableFilterData(id = id, name = name, isSelected = false))
                }
            }

            Log.i(TAG, "📦 [REPO] Successfully fetched floors from Firestore. Total count: ${floorList.size} (including 'All')")
            floorList.forEach { Log.v(TAG, "   └─ Floor ID: ${it.id}, Name: ${it.name}") }

            trySend(floorList)
        }
        awaitClose {
            Log.d(TAG, "📦 [REPO] Closing floors callbackFlow. Removing SnapshotListener.")
            listener.remove()
        }
    }

    fun getTables(): Flow<ResourceUiState<List<TableCardData>>> = flow {
        Log.d(TAG, "📦 [REPO] getTables() flow started. Emitting Loading State...")
        emit(ResourceUiState.Loading)

        try {
            delay(500) // Simulating network lag

            // Hardcoded dummy floor IDs matching your image data maps
            val dummyTables = listOf(
                TableCardData("T1", "aBSvOGkKGlbZA1vEgucc", 4, TableStatus.FREE, "20"),
                TableCardData("T2", "aBSvOGkKGlbZA1vEgucc", 2, TableStatus.OCCUPIED, "15"),
                TableCardData("T3", "F2", 6, TableStatus.RESERVED, null),
                TableCardData("T4", "F2", 4, TableStatus.BILLING, "50"),
                TableCardData("T1", "aBSvOGkKGlbZA1vEgucc", 4, TableStatus.FREE, "20"),
                TableCardData("T2", "aBSvOGkKGlbZA1vEgucc", 2, TableStatus.OCCUPIED, "15"),
            )

            Log.i(TAG, "📦 [REPO] Dummy tables generated successfully. Emitting Success State with ${dummyTables.size} items.")
            emit(ResourceUiState.Success(dummyTables))
        } catch (e: Exception) {
            Log.e(TAG, "📦 [REPO] Exception caught while fetching tables flow", e)
            emit(ResourceUiState.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)
}