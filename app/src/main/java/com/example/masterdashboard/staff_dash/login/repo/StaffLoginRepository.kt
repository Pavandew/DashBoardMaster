package com.example.masterdashboard.staff_dash.login.repo

import com.example.masterdashboard.manager_single_res_dash.home.models.StaffDataModel
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
        return try {
            val searchKey = staffId.trim()
            Log.d("StaffLoginRepo", "🔍 Executing strict literal query matching [staffId == '$searchKey']")

            // ✅ CAMELCASE ALIGNMENT: Must match the "staffId" key used in StaffDataModel.toMap()
            val querySnapshot = firestore.collectionGroup("staff")
                .whereEqualTo("staffId", searchKey)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Log.w("StaffLoginRepo", "❌ No match: No document contains staffId matching exactly '$searchKey' in any 'staff' sub-collection")
                return Result.failure(Exception("No staff profile matching ID code '$searchKey' exists in the system database records."))
            }

            val document = querySnapshot.documents[0]
            Log.i("StaffLoginRepo", "🟢 Match Found! Parsing document ID: ${document.id}")

            // Extract the Restaurant Owner UID from the parent path (users/{ownerUid}/staff/{staffDocId})
            val ownerUid = document.reference.parent.parent?.id ?: ""
            Log.d("StaffLoginRepo", "🔗 Identified parent Restaurant Owner UID: $ownerUid")

            // Manual mapping to construct the model out of exact schema keys
            try {
                val staffModel = StaffDataModel(
                    id = document.getString("id") ?: document.id,
                    // ✅ CAMELCASE ALIGNMENT: Extracting from correct field 'staffId'
                    staffId = document.getString("staffId") ?: "",
                    password = document.getString("password") ?: "",
                    staffName = document.getString("staffName") ?: "",
                    mobile = document.getString("mobile") ?: "",
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
            Log.e("StaffLoginRepo", "Critical connection transaction error", e)
            Result.failure(e)
        }
    }
}