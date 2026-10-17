package com.restroone.app.login.repo

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

class AuthCloudFunctionRepository {

    private val functions = FirebaseFunctions.getInstance()
    private val TAG = "AuthCloudRepo"

    /**
     * Authenticates user via server-side Cloud Function (auto-heals legacy account mappings).
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
            val friendlyMsg = com.restroone.app.utils.NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * Registers secure credentials server-side via Cloud Function.
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
            val friendlyMsg = com.restroone.app.utils.NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * Finds user by mobile for password reset.
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
            val friendlyMsg = com.restroone.app.utils.NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }
}
