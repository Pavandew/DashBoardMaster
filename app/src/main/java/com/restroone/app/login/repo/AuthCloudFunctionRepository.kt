package com.restroone.app.login.repo

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class AuthCloudFunctionRepository {

    private val functions = FirebaseFunctions.getInstance()
    private val TAG = "AuthCloudRepo"

    /**
     * Calls the server-side Cloud Function to authenticate user via Mobile + Password
     * and returns a Firebase Custom Token.
     */
    suspend fun authenticateUser(mobile: String, password: String): Result<String> {
        return try {
            val formattedMobile = if (mobile.startsWith("+91")) mobile else "+91$mobile"
            val data = hashMapOf(
                "mobile" to formattedMobile,
                "password" to password
            )

            Log.d(TAG, "🚀 [Auth] Initiating authenticateUser Cloud Function call for mobile: $formattedMobile")

            val result = functions
                .getHttpsCallable("authenticateUser")
                .call(data)
                .await()

            val resultMap = result.data as? Map<*, *>
            val token = resultMap?.get("token") as? String
            val uid = resultMap?.get("uid") as? String ?: ""

            if (!token.isNullOrEmpty()) {
                Log.i(TAG, "🟢 [Auth] Successfully received Custom Token from Cloud Function for UID: $uid")
                Result.success(token)
            } else {
                Log.e(TAG, "❌ [Auth] Custom token response was empty from Cloud Function.")
                Result.failure(Exception("Custom token response was empty."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Auth] Exception during authenticateUser Cloud Function call: ${e.message}", e)
            val friendlyMsg = com.restroone.app.utils.NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * Calls the server-side Cloud Function to register secure credentials after Phone OTP verification.
     */
    suspend fun registerUserCredentials(mobile: String, password: String): Result<Unit> {
        return try {
            val formattedMobile = if (mobile.startsWith("+91")) mobile else "+91$mobile"
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
            Log.e(TAG, "❌ [Register] Error registering user credentials: ${e.message}", e)
            val friendlyMsg = com.restroone.app.utils.NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * Calls the server-side Cloud Function to find a user by mobile for password reset (bypassing Firestore client permissions).
     */
    suspend fun findUserForReset(mobile: String): Result<Unit> {
        return try {
            val formattedMobile = if (mobile.startsWith("+91")) mobile else "+91$mobile"
            val data = hashMapOf("mobile" to formattedMobile)

            Log.d(TAG, "🚀 [Reset] Calling findUserForReset Cloud Function for mobile: $formattedMobile")

            functions
                .getHttpsCallable("findUserForReset")
                .call(data)
                .await()

            Log.i(TAG, "🟢 [Reset] User lookup successful for mobile: $formattedMobile")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Reset] Error during findUserForReset Cloud Function call: ${e.message}", e)
            val friendlyMsg = com.restroone.app.utils.NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }
}
