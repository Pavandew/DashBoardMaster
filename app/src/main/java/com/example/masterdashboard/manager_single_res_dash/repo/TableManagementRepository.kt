package com.example.masterdashboard.manager_single_res_dash.repo

import android.util.Log
import com.example.masterdashboard.manager_single_res_dash.models.FloorDataModel
import com.example.masterdashboard.manager_single_res_dash.models.TableData
import com.example.masterdashboard.login.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TableManagementRepository {

    companion object {
        private const val TAG = "TableManagementRepository ----> "
    }

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Streams top-level restaurant floors in real-time.
     */
    fun getFloors(ownerUid: String): Flow<List<FloorDataModel>> = callbackFlow {
        Log.d(TAG, "getFloors stream initiated for Owner: $ownerUid")

        val query = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_RES_FLOORS)
            .orderBy("displayFloor", Query.Direction.ASCENDING)

        val listeners = query.addSnapshotListener { snapshots, exception ->
            if (exception != null) {
                Log.e(TAG, "Firestore error encountered inside getFloors snapshot block: ${exception.message}", exception)
                close(exception)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val floors = snapshots.toObjects(FloorDataModel::class.java)
                Log.i(TAG, "getFloors snapshot processed. Deserialized ${floors.size} Floor entries.")
                trySend(floors)
            }
        }

        awaitClose {
            Log.d(TAG, "getFloors real-time flow listener detached safely.")
            listeners.remove()
        }
    }

    /**
     * Store a brand new Floor document in the database collection cleanly.
     */
    suspend fun storeNewFloor(ownerUid: String, floorName: String, nextDisplayOrder: Int) {
        Log.i(TAG, "storeNewFloor task triggered for Floor: '$floorName'")

        val floorCollectionRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_RES_FLOORS)

        val newDocRef = floorCollectionRef.document()

        val newFloorData = FloorDataModel(
            floorId = newDocRef.id,
            floorName = floorName,
            displayFloor = nextDisplayOrder,
            tableCount = 0
        )

        val writeMap = newFloorData.toMap()
        newDocRef.set(writeMap).await()
        Log.i(TAG, "Successfully saved Floor document to Firestore. Assigned ID: ${newDocRef.id}")
    }

    /**
     * Executes a cascading batch deletion to completely erase a floor document
     * along with all its nested tables from the subcollection.
     */
    suspend fun removeFloorCascading(ownerUid: String, floorId: String) {
        Log.i(TAG, "removeFloorCascading transaction initiated for Floor ID: $floorId")

        val floorDocRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_RES_FLOORS)
            .document(floorId)

        val tablesCollectionRef = floorDocRef.collection(AppConstants.COLLECTION_TABLES)
        val tablesSnapshot = tablesCollectionRef.get().await()

        val batch = firestore.batch()

        if (!tablesSnapshot.isEmpty) {
            Log.d(TAG, "Found ${tablesSnapshot.size()} nested tables to delete inside Floor ID: $floorId")
            for (tableDoc in tablesSnapshot.documents) {
                batch.delete(tableDoc.reference)
            }
        }

        batch.delete(floorDocRef)
        batch.commit().await()
        Log.i(TAG, "✅ Cascading write batch committed. Floor and nested tables deleted completely.")
    }

    /**
     * Listens to real-time additions/removals of individual tables mapped inside a specific floor.
     */
    fun getLiveTables(ownerUid: String, floorId: String): Flow<List<TableData>> = callbackFlow {
        Log.d(TAG, "getLiveTables stream initiated for Floor ID: $floorId")

        val query = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_RES_FLOORS)
            .document(floorId)
            .collection(AppConstants.COLLECTION_TABLES)
            .orderBy("tableName", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshots, exception ->
            if (exception != null) {
                Log.e(TAG, "Firestore error encountered inside getLiveTables snapshot block: ${exception.message}", exception)
                close(exception)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val tables = snapshots.toObjects(TableData::class.java)
                Log.i(TAG, "getLiveTables snapshot processed. Deserialized ${tables.size} Table entries.")
                trySend(tables)
            }
        }

        awaitClose {
            Log.d(TAG, "getLiveTables real-time flow listener detached safely.")
            listener.remove()
        }
    }

    /**
     * Commits a new table document layout definition to a floor subcollection.
     */
    suspend fun storeNewTable(ownerUid: String, floorId: String, tableName: String, capacity: Int, tableStatus: String) {
        Log.i(TAG, "storeNewTable transaction initiated for Table: '$tableName' [Status: $tableStatus]")

        val floorDocRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_RES_FLOORS)
            .document(floorId)

        val tableCollectionRef = floorDocRef.collection(AppConstants.COLLECTION_TABLES)
        val newTableDocRef = tableCollectionRef.document()

        val newTableData = TableData(
            tableId = newTableDocRef.id,
            tableName = tableName,
            capacity = capacity,
            status = tableStatus,
            assignedStaffId = null
        )

        val compiledTableMap = newTableData.toMap()

        firestore.runTransaction { transaction ->
            val floorSnapshot = transaction.get(floorDocRef)
            val currentCount = floorSnapshot.getLong("tableCount") ?: 0

            transaction.set(newTableDocRef, compiledTableMap)
            transaction.update(floorDocRef, "tableCount", currentCount + 1)
            null
        }.await()

        Log.i(TAG, "✅ storeNewTable transaction completed successfully. Committed ID: ${newTableDocRef.id}")
    }

    /**
     * Deletes a single table document from a floor's nested subcollection.
     * Uses a Firestore Transaction to safely decrement the parent floor's tableCount.
     */
    suspend fun removeTableTransactional(ownerUid: String, floorId: String, tableId: String) {
        Log.i(TAG, "removeTableTransactional initiated for Table ID: $tableId under Floor ID: $floorId")

        val floorDocRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_RES_FLOORS)
            .document(floorId)

        val tableDocRef = floorDocRef.collection(AppConstants.COLLECTION_TABLES)
            .document(tableId)

        firestore.runTransaction { transaction ->
            // Reads first
            val floorSnapshot = transaction.get(floorDocRef)
            val currentCount = floorSnapshot.getLong("tableCount") ?: 0

            // Writes next
            transaction.delete(tableDocRef)

            val newCount = if (currentCount > 0) currentCount - 1 else 0
            transaction.update(floorDocRef, "tableCount", newCount)
            null
        }.await()

        Log.i(TAG, "✅ removeTableTransactional completed successfully. Table deleted: $tableId")
    }
}

