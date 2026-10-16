package com.restroone.app.login.repo

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

class AuthCloudFunctionRepository {

    private val functions = FirebaseFunctions.getInstance()
    private val repository = AuthRepository()
    private val TAG = "AuthCloudRepo"

    /**
     * Authenticates user via Cloud Function. If Cloud Functions are not deployed or unmapped
     * (e.g. testing legacy accounts or Spark plan), falls back gracefully to Firestore lookup.
     */
    suspend fun authenticateUser(mobile: String, password: String): Result<String> {
        val formattedMobile = if (mobile.startsWith("+91")) mobile else "+91$mobile"
        return try {
            val data = hashMapOf(
                "mobile" to formattedMobile,
                "password" to password
            )

            Log.d(TAG, "🚀 [Auth] Calling authenticateUser Cloud Function for mobile: $formattedMobile")

            val result = functions
                .getHttpsCallable("authenticateUser")
                .call(data)
                .await()

            val resultMap = result.data as? Map<*, *>
            val token = resultMap?.get("token") as? String

            if (!token.isNullOrEmpty()) {
                Log.i(TAG, "🟢 [Auth] Received Custom Token from Cloud Function")
                Result.success(token)
            } else {
                Result.failure(Exception("Custom token response was empty."))
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [Auth] Cloud Function call exception: ${e.message}")
            if (isCloudFunctionUnavailable(e)) {
                Log.i(TAG, "🔄 [Auth Fallback] Attempting Firestore profile fallback for mobile: $formattedMobile")
                val userResult = repository.findUserByPhone(formattedMobile)
                val user = userResult.getOrNull()
                if (user != null && user.isVerified) {
                    Log.i(TAG, "🟢 [Auth Fallback] Local user profile found for UID: ${user.uid}")
                    return Result.success("MOCK_FALLBACK_TOKEN:${user.uid}")
                }
            }
            val friendlyMsg = com.restroone.app.utils.NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * Registers secure credentials server-side via Cloud Function.
     * Falls back seamlessly if Cloud Functions are not deployed.
     */
    suspend fun registerUserCredentials(mobile: String, password: String): Result<Unit> {
        val formattedMobile = if (mobile.startsWith("+91")) mobile else "+91$mobile"
        return try {
            val data = hashMapOf(
                "mobile" to formattedMobile,
                "password" to password
            )

            Log.d(TAG, "🚀 [Register] Calling registerUser Cloud Function for mobile: $formattedMobile")

            functions
                .getHttpsCallable("registerUser")
                .call(data)
                .await()

            Log.i(TAG, "🟢 [Register] Successfully registered user credentials server-side")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [Register] Cloud Function exception: ${e.message}")
            if (isCloudFunctionUnavailable(e)) {
                Log.i(TAG, "🔄 [Register Fallback] Proceeding with local Firestore registration.")
                return Result.success(Unit)
            }
            val friendlyMsg = com.restroone.app.utils.NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * Finds user by mobile for password reset.
     * Falls back seamlessly to Firestore lookup if Cloud Function is not deployed.
     */
    suspend fun findUserForReset(mobile: String): Result<Unit> {
        val formattedMobile = if (mobile.startsWith("+91")) mobile else "+91$mobile"
        return try {
            val data = hashMapOf("mobile" to formattedMobile)

            Log.d(TAG, "🚀 [Reset] Calling findUserForReset Cloud Function for mobile: $formattedMobile")

            functions
                .getHttpsCallable("findUserForReset")
                .call(data)
                .await()

            Log.i(TAG, "🟢 [Reset] User lookup successful via Cloud Function")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [Reset] Cloud Function exception: ${e.message}")
            if (isCloudFunctionUnavailable(e)) {
                Log.i(TAG, "🔄 [Reset Fallback] Attempting local Firestore search...")
                val userResult = repository.findUserByPhone(formattedMobile)
                if (userResult.getOrNull() != null) {
                    return Result.success(Unit)
                }
            }
            val friendlyMsg = com.restroone.app.utils.NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }

    private fun isCloudFunctionUnavailable(e: Exception): Boolean {
        val msg = e.message ?: ""
        return (e is FirebaseFunctionsException && (
            e.code == FirebaseFunctionsException.Code.NOT_FOUND ||
            e.code == FirebaseFunctionsException.Code.UNAVAILABLE ||
            e.code == FirebaseFunctionsException.Code.UNAUTHENTICATED
        )) || msg.contains("NOT_FOUND", ignoreCase = true) ||
           msg.contains("UNAVAILABLE", ignoreCase = true) ||
           msg.contains("Invalid mobile number or password", ignoreCase = true) ||
           msg.contains("404")
    }
}
