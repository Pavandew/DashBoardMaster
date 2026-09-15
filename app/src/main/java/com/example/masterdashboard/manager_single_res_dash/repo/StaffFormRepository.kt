package com.example.masterdashboard.manager_single_res_dash.repo

import com.example.masterdashboard.manager_single_res_dash.models.StaffDataModel
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.RestaurantPathHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class StaffFormRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun saveStaffToFirestore(staff: StaffDataModel, ownerUid: String) : Result<Unit> {
        return try {
            if (ownerUid.isBlank()) {
                return Result.failure(Exception("Owner UID is missing"))
            }

            // Reference to outlet staff roster: restaurants -> restaurantId -> staff
            val staffCollection = RestaurantPathHelper.getOutletDocRef(ownerUid)
                .collection(AppConstants.COLLECTION_STAFF)

            val newDocRef = staffCollection.document()
            val finalStaffData = staff.copy(id = newDocRef.id)

            // 1. Save in restaurant outlet staff roster
            newDocRef.set(finalStaffData.toMap()).await()

            // 2. Also save/link staff user account in users/{staffDocId} for login
            val userStaffRef = firestore.collection(AppConstants.COLLECTION_USERS).document(newDocRef.id)
            val staffAccountData = mapOf(
                AppConstants.FIELD_UID to newDocRef.id,
                AppConstants.FIELD_STAFF_ID to finalStaffData.staffId,
                AppConstants.FIELD_FULL_NAME to finalStaffData.staffName,
                AppConstants.FIELD_MOBILE to finalStaffData.mobile,
                AppConstants.FIELD_EMAIL to finalStaffData.email,
                AppConstants.FIELD_PASSWORD to finalStaffData.password,
                AppConstants.FIELD_ROLE to finalStaffData.role.ifEmpty { AppConstants.ROLE_STAFF },
                AppConstants.FIELD_RESTAURANT_ID to ownerUid,
                "ownerUid" to ownerUid,
                AppConstants.FIELD_STATUS to finalStaffData.status
            )
            userStaffRef.set(staffAccountData, SetOptions.merge()).await()

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

            RestaurantPathHelper.getOutletDocRef(ownerUid)
                .collection(AppConstants.COLLECTION_STAFF)
                .document(staff.id)
                .set(staff.toMap())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
