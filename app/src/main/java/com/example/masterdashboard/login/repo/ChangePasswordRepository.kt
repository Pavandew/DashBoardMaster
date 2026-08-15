package com.example.masterdashboard.login.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChangePasswordRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "ChangePasswordRepo"

    suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<Boolean> {
        return try {
            Log.d(TAG, "signInWithCredential: Attempting sign-in with OTP credential")
            val task = auth.signInWithCredential(credential).await()
            if (task.user != null) {
                Log.i(TAG, "signInWithCredential: Success! User UID: ${task.user?.uid}")
                Result.success(true)
            } else {
                Log.w(TAG, "signInWithCredential: Task successful but user is null")
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "signInWithCredential: Error during Firebase sign-in", e)
            Result.failure(e)
        }
    }

    suspend fun updateOwnerPassword(uid: String, newPassword: String): Result<Unit> {
        return try {
            Log.d(TAG, "updateOwnerPassword: Updating password hash for Owner UID: $uid")
            val passwordHash = newPassword.hashCode().toString()
            db.collection(AppConstants.COLLECTION_USERS)
                .document(uid)
                .update(AppConstants.FIELD_PASSWORD_HASH, passwordHash)
                .await()
            Log.i(TAG, "updateOwnerPassword: Password updated successfully in Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateOwnerPassword: Error updating password for UID: $uid", e)
            Result.failure(e)
        }
    }

    suspend fun updateStaffPassword(ownerUid: String, staffDocId: String, newPassword: String): Result<Unit> {
        return try {
            Log.d(TAG, "updateStaffPassword: Updating password for StaffDocId: $staffDocId under Owner: $ownerUid")
            db.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .collection(AppConstants.COLLECTION_STAFF)
                .document(staffDocId)
                .update(AppConstants.FIELD_PASSWORD, newPassword)
                .await()
            Log.i(TAG, "updateStaffPassword: Staff password updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateStaffPassword: Error updating staff password for $staffDocId", e)
            Result.failure(e)
        }
    }
}