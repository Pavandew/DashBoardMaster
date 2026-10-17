package com.restroone.app.login.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.restroone.app.utils.NetworkUtils
import kotlinx.coroutines.tasks.await

class ChangePasswordRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    private val TAG = "ChangePasswordRepo"

    suspend fun verifyOwnerCurrentPassword(uid: String, currentPassword: String): Result<Boolean> {
        return try {
            if (auth.currentUser?.uid != uid) {
                return Result.failure(Exception("Authenticated account does not match the owner account"))
            }

            val response = functions.getHttpsCallable("verifyCurrentPassword")
                .call(mapOf("currentPassword" to currentPassword))
                .await()
                .data as? Map<*, *>

            if (response?.get("verified") == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Current password verification failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "verifyOwnerCurrentPassword: Error verifying owner password", e)
            Result.failure(e)
        }
    }

    suspend fun verifyStaffCurrentPassword(ownerUid: String, staffDocId: String, currentPassword: String): Result<Boolean> {
        return try {
            val response = functions.getHttpsCallable("verifyStaffCurrentPassword")
                .call(
                    mapOf(
                        "staffDocId" to staffDocId,
                        "currentPassword" to currentPassword
                    )
                )
                .await()
                .data as? Map<*, *>

            if (response?.get("verified") == true) Result.success(true)
            else Result.failure(Exception("Current password verification failed"))
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
                Log.i(TAG, "signInWithCredential: Phone authentication succeeded")
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
            val data = hashMapOf("newPassword" to newPassword)
            functions.getHttpsCallable("updatePassword")
                .call(data)
                .await()
            Log.i(TAG, "updateOwnerPassword: Password updated successfully via Cloud Function")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateOwnerPassword: Error updating password", e)
            Result.failure(Exception(NetworkUtils.formatErrorMessage(e)))
        }
    }

    suspend fun resetPassword(newPassword: String): Result<Unit> {
        return try {
            functions.getHttpsCallable("resetPassword")
                .call(mapOf("newPassword" to newPassword))
                .await()
            Log.i(TAG, "resetPassword: Password reset successful via Cloud Function")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Phone-verified password reset failed: ${e.message}", e)
            Result.failure(Exception(NetworkUtils.formatErrorMessage(e)))
        }
    }

    suspend fun updateStaffPassword(ownerUid: String, staffDocId: String, newPassword: String): Result<Unit> {
        return try {
            Log.d(TAG, "updateStaffPassword: Updating password for StaffDocId: $staffDocId")
            val payload = mutableMapOf("newPassword" to newPassword)
            if (staffDocId.isNotBlank()) payload["staffDocId"] = staffDocId
            val response = functions.getHttpsCallable("updateStaffPassword")
                .call(payload)
                .await()
                .data as? Map<*, *>

            if (response?.get("authUserMatchesStaff") == false) {
                auth.signOut()
            }
            Log.i(TAG, "updateStaffPassword: Staff password updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateStaffPassword: Error updating staff password for $staffDocId", e)
            Result.failure(e)
        }
    }
}
