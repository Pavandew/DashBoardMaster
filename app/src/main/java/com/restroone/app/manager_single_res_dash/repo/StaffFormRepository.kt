package com.restroone.app.manager_single_res_dash.repo

import com.restroone.app.manager_single_res_dash.models.StaffDataModel
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.RestaurantPathHelper
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class StaffFormRepository {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun saveStaffToFirestore(staff: StaffDataModel, ownerUid: String) : Result<Unit> {
        return try {
            if (ownerUid.isBlank()) {
                return Result.failure(Exception("Owner UID is missing"))
            }

            functions.getHttpsCallable("createStaff")
                .call(
                    mapOf(
                        "restaurantId" to ownerUid,
                        "staff" to staff.toMap(),
                        "password" to staff.password
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isMobileNumberRegistered(ownerUid: String, mobileNumber: String): Result<Boolean> = runCatching {
        val querySnapshot = RestaurantPathHelper.getOutletDocRef(ownerUid)
            .collection(AppConstants.COLLECTION_STAFF)
            .whereEqualTo(AppConstants.FIELD_MOBILE, mobileNumber)
            .get()
            .await()

        !querySnapshot.isEmpty
    }

    suspend fun updateStaffInFirestore(staff: StaffDataModel, ownerUid: String): Result<Unit> {
        return try {
            if (ownerUid.isBlank() || staff.id.isBlank()) {
                return Result.failure(Exception("Required IDs are missing"))
            }

            functions.getHttpsCallable("updateStaff")
                .call(
                    mapOf(
                        "restaurantId" to ownerUid,
                        "staffDocId" to staff.id,
                        "staff" to staff.toMap()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
