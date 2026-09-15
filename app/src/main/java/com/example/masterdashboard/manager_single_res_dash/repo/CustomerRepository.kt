package com.example.masterdashboard.manager_single_res_dash.repo

import android.util.Log
import com.example.masterdashboard.manager_single_res_dash.models.CustomerModel
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.RestaurantPathHelper
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CustomerRepository {

    fun getCustomers(managerId: String): Flow<List<CustomerModel>> = callbackFlow {
        if (managerId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val customersRef = RestaurantPathHelper.getOutletDocRef(managerId)
            .collection(AppConstants.COLLECTION_CUSTOMERS)
            .orderBy(AppConstants.FIELD_LAST_VISIT, Query.Direction.DESCENDING)

        val listener = customersRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("CustomerRepo", "Error listening to customers", e)
                close(e)
                return@addSnapshotListener
            }

            val customers = snapshot?.toObjects(CustomerModel::class.java) ?: emptyList()
            trySend(customers)
        }

        awaitClose { listener.remove() }
    }
}
