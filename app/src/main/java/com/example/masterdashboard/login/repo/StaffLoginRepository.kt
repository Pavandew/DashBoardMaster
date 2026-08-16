package com.example.masterdashboard.login.repo

import com.example.masterdashboard.manager_single_res_dash.models.StaffDataModel
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

class StaffLoginRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Searches for a staff member across all restaurant owner sub-collections using collectionGroup.
     * Returns a Pair containing the staff profile data and the parent Restaurant Owner UID.
     */
    suspend fun findStaffProfileById(staffId: String): Result<Pair<StaffDataModel, String>> {
        return findStaffProfileByField(AppConstants.FIELD_STAFF_ID, staffId)
    }

    suspend fun findStaffProfileByPhone(phone: String): Result<Pair<StaffDataModel, String>> {
        return findStaffProfileByField(AppConstants.FIELD_MOBILE, phone)
    }

    private suspend fun findStaffProfileByField(fieldName: String, value: String): Result<Pair<StaffDataModel, String>> {
        return try {
            val searchKey = value.trim()
            Log.d("StaffLoginRepo", "🔍 Executing query matching [$fieldName == '$searchKey']")

            var querySnapshot = firestore.collectionGroup(AppConstants.COLLECTION_STAFF)
                .whereEqualTo(fieldName, searchKey)
                .get()
                .await()

            // Fallback for mobile/phone field mismatch
            if (querySnapshot.isEmpty && fieldName == AppConstants.FIELD_MOBILE) {
                Log.d("StaffLoginRepo", "No staff found with '$fieldName', trying fallback field 'phone'")
                querySnapshot = firestore.collectionGroup(AppConstants.COLLECTION_STAFF)
                    .whereEqualTo("phone", searchKey)
                    .get()
                    .await()
            }

            if (querySnapshot.isEmpty) {
                Log.w("StaffLoginRepo", "❌ No match: No document contains $fieldName matching exactly '$searchKey'")
                return Result.failure(Exception("No staff profile matching '$searchKey' exists in the system."))
            }

            val document = querySnapshot.documents[0]
            Log.i("StaffLoginRepo", "🟢 Match Found! Parsing document ID: ${document.id}")

            val ownerUid = document.reference.parent.parent?.id ?: ""
            Log.d("StaffLoginRepo", "🔗 Identified parent Restaurant Owner UID: $ownerUid")

            try {
                val staffModel = StaffDataModel(
                    id = document.getString("id") ?: document.id,
                    staffId = document.getString(AppConstants.FIELD_STAFF_ID) ?: "",
                    password = document.getString(AppConstants.FIELD_PASSWORD) ?: "",
                    staffName = document.getString("staffName") ?: "",
                    // Try getting from FIELD_MOBILE then fallback to "phone"
                    mobile = document.getString(AppConstants.FIELD_MOBILE) 
                        ?: document.getString("phone") 
                        ?: "",
                    email = document.getString("email") ?: "",
                    gender = document.getString("gender") ?: "",
                    role = document.getString("role") ?: "",
                    department = document.getString("department") ?: "",
                    joiningDate = document.getString("joiningDate") ?: "",
                    shift = document.getString("shift") ?: "",
                    salary = document.getString("salary") ?: "",
                    status = document.getString("status") ?: "Active",
                    permissions = (document.get("permissions") as? List<*>)?.map { it.toString() } ?: emptyList()
                )
                Result.success(Pair(staffModel, ownerUid))
            } catch (parseException: Exception) {
                Log.e("StaffLoginRepo", "Model deserialization parsing failure", parseException)
                Result.failure(Exception("Data structure parsing mismatch: ${parseException.localizedMessage}"))
            }

        } catch (e: Exception) {
            Log.e("StaffLoginRepo", "Critical connection error", e)
            Result.failure(e)
        }
    }
}
