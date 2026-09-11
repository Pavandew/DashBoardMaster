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

    suspend fun verifyOwnerCurrentPassword(uid: String, currentPassword: String): Result<Boolean> {
        return try {
            Log.d(TAG, "verifyOwnerCurrentPassword: Verifying password for Owner UID: $uid")
            val doc = db.collection(AppConstants.COLLECTION_USERS)
                .document(uid)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.failure(Exception("User account not found"))
            }

            val storedHash = doc.getString(AppConstants.FIELD_PASSWORD_HASH) ?: ""
            val inputHash = currentPassword.hashCode().toString()

            if (storedHash == inputHash) {
                Log.i(TAG, "verifyOwnerCurrentPassword: Password match verified")
                Result.success(true)
            } else {
                Log.w(TAG, "verifyOwnerCurrentPassword: Password mismatch")
                Result.failure(Exception("Current password is incorrect"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "verifyOwnerCurrentPassword: Error verifying owner password", e)
            Result.failure(e)
        }
    }

    suspend fun verifyStaffCurrentPassword(ownerUid: String, staffDocId: String, currentPassword: String): Result<Boolean> {
        return try {
            Log.d(TAG, "verifyStaffCurrentPassword: Verifying password for StaffDocId: $staffDocId under Owner: $ownerUid")
            val doc = db.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .collection(AppConstants.COLLECTION_STAFF)
                .document(staffDocId)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.failure(Exception("Staff account not found"))
            }

            val storedPassword = doc.getString(AppConstants.FIELD_PASSWORD) ?: ""

            if (storedPassword == currentPassword) {
                Log.i(TAG, "verifyStaffCurrentPassword: Password match verified")
                Result.success(true)
            } else {
                Log.w(TAG, "verifyStaffCurrentPassword: Password mismatch")
                Result.failure(Exception("Current password is incorrect"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "verifyStaffCurrentPassword: Error verifying staff password", e)
            Result.failure(e)
        }
    }

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
