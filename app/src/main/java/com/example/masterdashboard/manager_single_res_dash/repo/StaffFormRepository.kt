package com.example.masterdashboard.manager_single_res_dash.repo

import com.example.masterdashboard.manager_single_res_dash.models.StaffDataModel
import com.example.masterdashboard.login.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StaffFormRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun saveStaffToFirestore(staff: StaffDataModel, ownerUid: String) : Result<Unit> {
        return try {
            if (ownerUid.isBlank()) {
                return Result.failure(Exception("Owner UID is missing"))
            }

            // Reference to: users -> ownerUid -> staff
            val staffCollection = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .collection(AppConstants.COLLECTION_STAFF)

            // Generate a unique document ID for this new staff
            val newDocRef = staffCollection.document()
            val finalStaffData = staff.copy(id = newDocRef.id)

            // Save map to Firestore
            newDocRef.set(finalStaffData.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add this function inside your StaffFormRepository class
    suspend fun isMobileNumberRegistered(ownerUid: String, mobileNumber: String): Result<Boolean> = runCatching {
        val firestore = FirebaseFirestore.getInstance()

        val querySnapshot = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_STAFF)
            .whereEqualTo("mobile", mobileNumber)
            .get()
            .await()

        // If the snapshot is not empty, it means a staff member with this number exists
        !querySnapshot.isEmpty
    }
}