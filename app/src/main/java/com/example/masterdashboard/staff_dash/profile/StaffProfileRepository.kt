package com.example.masterdashboard.staff_dash.profile

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class StaffProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val sessionManager: SessionManager
) {

    companion object {
        private const val TAG = "StaffProfile_Debug"
    }

    fun getStaffProfileStream(): Flow<Result<StaffProfileModel>> = callbackFlow {
        val managerId = sessionManager.getUid().trim()
        val customStaffId = sessionManager.getStaffId().trim()

        Log.d(TAG, "📦 [REPO] Querying subcollection for ManagerUID='$managerId', searching StaffID='$customStaffId'")

        if (managerId.isEmpty() || customStaffId.isEmpty()) {
            Log.e(TAG, "📦 [REPO] Error: UID or StaffID is empty in SessionManager.")
            trySend(Result.failure(Exception("Session expired or invalid credentials.")))
            close()
            return@callbackFlow
        }

        // Reference to subcollection: users/{managerId}/staff
        val collectionRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_STAFF)

        val listenerRegistration = collectionRef.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                Log.e(TAG, "📦 [REPO] Listener Error: ${error.message}", error)
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            if (querySnapshot != null && !querySnapshot.isEmpty) {
                Log.d(TAG, "📦 [REPO] Found ${querySnapshot.size()} total staff documents under manager.")

                var matchedProfile: StaffProfileModel? = null

                for (doc in querySnapshot.documents) {
                    val dataMap = doc.data
                    Log.d(TAG, "📄 [DOC] ID='${doc.id}' -> Fields=$dataMap")

                    // Flexible match checking across common field keys
                    val fieldId = dataMap?.get("id")?.toString()?.trim()
                    val fieldStaffId = dataMap?.get("staffId")?.toString()?.trim()
                    val fieldEmpId = dataMap?.get("empId")?.toString()?.trim()

                    if (doc.id.equals(customStaffId, ignoreCase = true) ||
                        fieldId.equals(customStaffId, ignoreCase = true) ||
                        fieldStaffId.equals(customStaffId, ignoreCase = true) ||
                        fieldEmpId.equals(customStaffId, ignoreCase = true)
                    ) {
                        matchedProfile = doc.toObject(StaffProfileModel::class.java)?.copy(
                            staffId = doc.id
                        )
                        Log.i(TAG, "✅ [MATCH FOUND] Doc ID='${doc.id}', Name='${matchedProfile?.staffName}'")
                        break
                    }
                }

                if (matchedProfile != null) {
                    trySend(Result.success(matchedProfile))
                } else {
                    Log.w(TAG, "⚠️ [REPO] No document matched StaffID '$customStaffId' across document IDs or fields.")
                    trySend(Result.failure(Exception("Staff member '$customStaffId' not found.")))
                }
            } else {
                Log.w(TAG, "📦 [REPO] Subcollection 'staff' under manager '$managerId' is completely empty.")
                trySend(Result.failure(Exception("No staff records exist under manager.")))
            }
        }

        awaitClose {
            Log.d(TAG, "📦 [REPO] Stream closed. Removing listener.")
            listenerRegistration.remove()
        }
    }
}