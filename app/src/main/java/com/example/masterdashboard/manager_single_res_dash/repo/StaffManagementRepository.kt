package com.example.masterdashboard.manager_single_res_dash.repo

import android.util.Log
import com.example.masterdashboard.manager_single_res_dash.models.StaffDataModel
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.RestaurantPathHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StaffManagementRepository {

    suspend fun getStaffList(ownerUid: String) : Result<List<StaffDataModel>> =
        runCatching {
            Log.d("StaffManagementRepo", "Fetching staff roster for Outlet: $ownerUid")

            val snapshot = RestaurantPathHelper.getOutletDocRef(ownerUid)
                .collection(AppConstants.COLLECTION_STAFF)
                .get()
                .await()

            val rawList = snapshot.toObjects(StaffDataModel::class.java)

            val naturalSortedList = rawList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) {
                it.staffName
            })

            Log.d("StaffManagementRepo", "Successfully sorted ${naturalSortedList.size} staff records alphabetically.")
            naturalSortedList
        }

    suspend fun getStaffCompleteDetails(ownerUid: String, staffDocId: String): Result<StaffDataModel> =
        runCatching {
            Log.d("StaffDetailRepo", "Fetching detail matching layout context path: restaurants/$ownerUid/staff/$staffDocId")

            val documentSnapshot = RestaurantPathHelper.getOutletDocRef(ownerUid)
                .collection(AppConstants.COLLECTION_STAFF)
                .document(staffDocId)
                .get()
                .await()

            if (!documentSnapshot.exists()) {
                throw Exception("Target employee profile document record does not exist on servers.")
            }

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

    /**
     * Deletes a specific staff member from the outlet staff roster and user login accounts.
     */
    suspend fun deleteStaffMember(ownerUid: String, staffId: String): Result<Unit> = runCatching {
        Log.i("StaffManagementRepo", "Deleting staff member '$staffId' for Outlet: $ownerUid")

        val staffDocRef = RestaurantPathHelper.getOutletDocRef(ownerUid)
            .collection(AppConstants.COLLECTION_STAFF)
            .document(staffId)

        val userStaffRef = FirebaseFirestore.getInstance()
            .collection(AppConstants.COLLECTION_USERS)
            .document(staffId)

        staffDocRef.delete().await()
        userStaffRef.delete().await()
        Log.i("StaffManagementRepo", "Successfully deleted staff '$staffId' from outlet roster and users collection.")
    }
}
