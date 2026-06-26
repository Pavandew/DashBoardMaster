package com.example.masterdashboard.manager_single_res_dash.home.repo

import com.example.masterdashboard.manager_single_res_dash.home.models.StaffDataModel
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import android.util.Log

class StaffManagementRepository {
    private val firebase = FirebaseFirestore.getInstance()

    // Queries the complete subcollection list for a specific restaurant manager ID path
    suspend fun getStaffList(ownerUid: String) : Result<List<StaffDataModel>> =
        runCatching {
            Log.d("StaffManagementRepo", "Fetching staff subcollection list for Owner: $ownerUid")

            // 1. Fetch data snapshot directly from the nested subcollection path
            val snapshot = firebase.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .collection(AppConstants.COLLECTION_STAFF)
                .get() // We remove the remote orderBy since character casing breaks ASCII indexes
                .await()

            val rawList = snapshot.toObjects(StaffDataModel::class.java)

            // 2. FIXED: Perform case-insensitive alphabetical sorting in local memory.
            // This guarantees that "pavan" and "Pavan" sort together naturally in A-Z order,
            // bypassing Firestore's strict, case-sensitive ASCII indexing rules.
            val naturalSortedList = rawList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) {
                it.staffName
            })

            Log.d("StaffManagementRepo", "Successfully sorted ${naturalSortedList.size} staff records alphabetically.")
            naturalSortedList
        }
}