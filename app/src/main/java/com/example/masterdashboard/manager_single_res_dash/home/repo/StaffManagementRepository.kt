package com.example.masterdashboard.manager_single_res_dash.home.repo

import com.example.masterdashboard.manager_single_res_dash.home.models.StaffDataModel
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class StaffManagementRepository {
    private val firebase = FirebaseFirestore.getInstance()

    // Queries the complete subcollection list for a specific restaurant manager ID path
    suspend fun getStaffList(ownerUid: String) : Result<List<StaffDataModel>> =
        runCatching {

            val snapshot = firebase.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .collection(
                    AppConstants.COLLECTION_STAFF)
                .orderBy("staffName", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.toObjects(StaffDataModel::class.java)
        }
}