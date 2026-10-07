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

            val result = functions
                .getHttpsCallable("authenticateUser")
                .call(data)
                .await()

            val resultMap = result.data as? Map<*, *>
            val token = resultMap?.get("token") as? String

            if (!token.isNullOrEmpty()) {
                Log.i(TAG, "Successfully received Custom Token from Cloud Function")
                Result.success(token)
            } else {
                Result.failure(Exception("Custom token response was empty."))
            }
        } catch (e: Exception) {
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

            functions
                .getHttpsCallable("registerUser")
                .call(data)
                .await()

            Log.i(TAG, "Successfully registered user credentials server-side")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user credentials", e)
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

            functions
                .getHttpsCallable("findUserForReset")
                .call(data)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            val friendlyMsg = com.restroone.app.utils.NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(friendlyMsg))
        }
    }
}
