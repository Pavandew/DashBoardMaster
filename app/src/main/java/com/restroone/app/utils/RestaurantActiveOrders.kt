package com.restroone.app.utils

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object RestaurantActiveOrders {
    fun observe(firestore: FirebaseFirestore, restaurantId: String): Flow<List<DocumentSnapshot>> = callbackFlow {
        val restaurant = firestore.collection(AppConstants.COLLECTION_RESTAURANTS).document(restaurantId)
        val lock = Any()
        val registrations = mutableMapOf<String, ListenerRegistration>()
        val documents = mutableMapOf<String, DocumentSnapshot>()
        val tableIdsByFloor = mutableMapOf<String, Set<String>>()

        fun emitDocuments() {
            trySend(documents.values.toList())
        }

        fun removeOrdersUnder(prefix: String) {
            val normalizedPrefix = prefix.trimEnd('/')
            registrations.keys.filter {
                it == "orders:$normalizedPrefix" || it.startsWith("orders:$normalizedPrefix/")
            }.toList().forEach { key ->
                registrations.remove(key)?.remove()
            }
            documents.keys.filter { it.startsWith("$normalizedPrefix/") }.toList().forEach(documents::remove)
        }

        fun listenForOrders(collectionPath: String, query: Query) {
            val key = "orders:$collectionPath"
            if (registrations.containsKey(key)) return
            registrations[key] = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                synchronized(lock) {
                    documents.keys.filter { it.startsWith("$collectionPath/") }
                        .toList().forEach(documents::remove)
                    snapshot?.documents?.forEach { documents[it.reference.path] = it }
                    emitDocuments()
                }
            }
        }

        lateinit var listenForTables: (String) -> Unit
        listenForTables = { floorId ->
            val tables = restaurant.collection(AppConstants.COLLECTION_RES_FLOORS)
                .document(floorId).collection(AppConstants.COLLECTION_TABLES)
            val key = "tables:$floorId"
            if (!registrations.containsKey(key)) {
                registrations[key] = tables.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    synchronized(lock) {
                        val currentTableIds = snapshot?.documents?.map { it.id }?.toSet().orEmpty()
                        val previousTableIds = tableIdsByFloor[floorId].orEmpty()
                        (previousTableIds - currentTableIds).forEach { staleTableId ->
                            val path = "${
                                tables.document(staleTableId).path
                            }/${AppConstants.COLLECTION_ACTIVE_ORDERS}"
                            removeOrdersUnder(path)
                        }
                        tableIdsByFloor[floorId] = currentTableIds
                        snapshot?.documents?.forEach { table ->
                            val orders = table.reference.collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
                                .whereEqualTo(AppConstants.FIELD_RESTAURANT_ID, restaurantId)
                            val ordersPath = table.reference.collection(AppConstants.COLLECTION_ACTIVE_ORDERS).path
                            listenForOrders(ordersPath, orders)
                        }
                        emitDocuments()
                    }
                }
            }
        }

        registrations["root-floors"] = restaurant.collection(AppConstants.COLLECTION_RES_FLOORS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                synchronized(lock) {
                    val currentFloorIds = snapshot?.documents?.map { it.id }?.toSet().orEmpty()
                    registrations.keys.filter {
                        it.startsWith("tables:") && it.removePrefix("tables:") !in currentFloorIds
                    }.toList().forEach { staleKey ->
                        val staleFloorId = staleKey.removePrefix("tables:")
                        registrations.remove(staleKey)?.remove()
                        tableIdsByFloor.remove(staleFloorId)
                        val floorPath = restaurant.collection(AppConstants.COLLECTION_RES_FLOORS)
                            .document(staleFloorId).path
                        removeOrdersUnder("$floorPath/")
                    }
                    snapshot?.documents?.forEach { listenForTables(it.id) }
                    emitDocuments()
                }
            }

        val rootOrders = restaurant.collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
        listenForOrders(
            rootOrders.path,
            rootOrders.whereEqualTo(AppConstants.FIELD_RESTAURANT_ID, restaurantId)
        )
        awaitClose {
            synchronized(lock) {
                registrations.values.forEach(ListenerRegistration::remove)
                registrations.clear()
                documents.clear()
                tableIdsByFloor.clear()
            }
        }
    }

    suspend fun getAll(firestore: FirebaseFirestore, restaurantId: String): List<DocumentSnapshot> {
        val restaurant = firestore.collection(AppConstants.COLLECTION_RESTAURANTS).document(restaurantId)
        val orders = mutableListOf<DocumentSnapshot>()
        val rootOrders = restaurant.collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
            .whereEqualTo(AppConstants.FIELD_RESTAURANT_ID, restaurantId).get().await()
        orders.addAll(rootOrders.documents)

        val floors = restaurant.collection(AppConstants.COLLECTION_RES_FLOORS).get().await()
        for (floor in floors.documents) {
            val tables = floor.reference.collection(AppConstants.COLLECTION_TABLES).get().await()
            for (table in tables.documents) {
                val tableOrders = table.reference.collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
                    .whereEqualTo(AppConstants.FIELD_RESTAURANT_ID, restaurantId).get().await()
                orders.addAll(tableOrders.documents)
            }
        }
        return orders
    }
}
