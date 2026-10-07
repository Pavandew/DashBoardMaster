package com.restroone.app.staff_dash.profile

import android.util.Log
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.RestaurantPathHelper
import com.restroone.app.utils.SessionManager
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
        val managerId = sessionManager.getRestaurantId().trim()
        val customStaffId = sessionManager.getStaffId().trim()
        val staffDocId = sessionManager.getStaffDocId().trim()

        Log.d(TAG, "📦 [REPO] Querying staff profile for RestaurantID='$managerId', StaffDocID='$staffDocId'")

        if (managerId.isEmpty() || customStaffId.isEmpty() || staffDocId.isEmpty()) {
            Log.e(TAG, "📦 [REPO] Error: UID or StaffID is empty in SessionManager.")
            trySend(Result.failure(Exception("Session expired or invalid credentials.")))
            close()
            return@callbackFlow
        }

        val documentRef = RestaurantPathHelper.getOutletDocRef(managerId)
            .collection(AppConstants.COLLECTION_STAFF)
            .document(staffDocId)

        val listenerRegistration = documentRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "📦 [REPO] Listener Error: ${error.message}", error)
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(StaffProfileModel::class.java)
                if (profile != null) {
                    trySend(Result.success(profile.copy(staffId = customStaffId)))
                } else {
                    trySend(Result.failure(Exception("Staff profile could not be parsed.")))
                }
            } else {
                trySend(Result.failure(Exception("Staff profile does not exist.")))
            }
        }

        awaitClose {
            Log.d(TAG, "📦 [REPO] Stream closed. Removing listener.")
            listenerRegistration.remove()
        }
    }
}
