package com.example.masterdashboard.manager_single_res_dash.repo

import com.example.masterdashboard.manager_single_res_dash.models.StaffDataModel
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
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

    suspend fun getStaffCompleteDetails(ownerUid: String, staffDocId: String): Result<StaffDataModel> =
        runCatching {
            Log.d("StaffDetailRepo", "Fetching detail matching layout context path: users/$ownerUid/staff/$staffDocId")

            // Points explicitly to your operational "staff" nested sub-collection
            val documentSnapshot = firebase.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .collection(AppConstants.COLLECTION_STAFF)
                .document(staffDocId)
                .get()
                .await()

            if (!documentSnapshot.exists()) {
                throw Exception("Target employee profile document record does not exist on servers.")
            }

            // Robust manual parsing matching your case-sensitive lowercase 'staffid' property format
            StaffDataModel(
                id = documentSnapshot.id,
                staffId = documentSnapshot.getString(AppConstants.FIELD_STAFF_ID) ?: documentSnapshot.getString("staffid") ?: "",
                password = documentSnapshot.getString(AppConstants.FIELD_PASSWORD) ?: "",
                staffName = documentSnapshot.getString(AppConstants.FIELD_STAFF_NAME) ?: "",
                mobile = documentSnapshot.getString(AppConstants.FIELD_MOBILE) ?: "",
                email = documentSnapshot.getString(AppConstants.FIELD_EMAIL) ?: "",
                gender = documentSnapshot.getString(AppConstants.FIELD_GENDER) ?: "",
                role = documentSnapshot.getString(AppConstants.FIELD_ROLE) ?: "",
                department = documentSnapshot.getString(AppConstants.FIELD_DEPARTMENT) ?: "",
                joiningDate = documentSnapshot.getString(AppConstants.FIELD_JOINING_DATE) ?: "",
                shift = documentSnapshot.getString(AppConstants.FIELD_SHIFT) ?: "",
                salary = documentSnapshot.getString(AppConstants.FIELD_SALARY) ?: "",
                status = documentSnapshot.getString(AppConstants.FIELD_STATUS) ?: "Active",
                permissions = (documentSnapshot.get(AppConstants.FIELD_PERMISSIONS) as? List<*>)?.map { it.toString() } ?: emptyList(),
                documentType = documentSnapshot.getString(AppConstants.FIELD_DOCUMENT_TYPE) ?: "",
                documentNumber = documentSnapshot.getString(AppConstants.FIELD_DOCUMENT_NUMBER) ?: ""
            )
        }
}
