package com.restroone.app.manager_single_res_dash.repo

import android.util.Log
import com.restroone.app.manager_single_res_dash.models.StaffDataModel
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.RestaurantPathHelper
import com.google.firebase.functions.FirebaseFunctions
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

            val staffData = documentSnapshot.toObject(StaffDataModel::class.java)
                ?: throw Exception("Failed to parse staff details.")

            staffData.copy(id = documentSnapshot.id)
        }

    /**
     * Deletes a specific staff member from the outlet staff roster and user login accounts.
     */
    suspend fun deleteStaffMember(ownerUid: String, staffId: String): Result<Unit> = runCatching {
        Log.i("StaffManagementRepo", "Deleting staff member '$staffId' for Outlet: $ownerUid")

        FirebaseFunctions.getInstance().getHttpsCallable("deleteStaff")
            .call(mapOf("restaurantId" to ownerUid, "staffDocId" to staffId))
            .await()
        Log.i("StaffManagementRepo", "Successfully deleted staff '$staffId' from outlet roster and users collection.")
    }
}
