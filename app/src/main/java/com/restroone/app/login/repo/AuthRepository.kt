package com.restroone.app.login.repo

import android.app.Activity
import android.util.Log
import com.restroone.app.login.models.UserModelData
import com.restroone.app.utils.AppConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.restroone.app.utils.NetworkUtils
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "AuthRepository"

    /**
     * Finds a user by their phone number, checking both 'mobile' and 'phone' fields.
     */
    suspend fun isPhoneRegistered(phone: String): Result<Boolean> {
        return try {
            val formattedPhone = if (phone.startsWith("+91")) phone else "+91$phone"
            val response = FirebaseFunctions.getInstance()
                .getHttpsCallable("isMobileRegistered")
                .call(mapOf("mobile" to formattedPhone))
                .await()
                .data as? Map<*, *>
                ?: throw IllegalStateException("Invalid mobile registration response")
            Result.success(response["registered"] == true)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking phone registration", e)
            val formattedMsg = NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(formattedMsg))
        }
    }

    /**
     * Gets a user profile directly by their Firebase Auth UID.
     */
    suspend fun getUserProfileByUid(uid: String): Result<UserModelData?> {
        return try {
            val doc = db.collection(AppConstants.COLLECTION_USERS)
                .document(uid)
                .get()
                .await()

            if (doc.exists()) {
                val user = doc.toObject(UserModelData::class.java)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile by UID", e)
            Result.failure(Exception(NetworkUtils.formatErrorMessage(e)))
        }
    }

    /**
     * Initiates Firebase Phone Number verification.
     */
    fun sendOtp(
        phone: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val formattedPhone = if (phone.startsWith("+91")) phone else "+91$phone"
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Signs in with the given PhoneAuthCredential.
     */
    suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<String> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid ?: throw Exception("UID is null after sign-in")
            Result.success(uid)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with credential", e)
            Result.failure(e)
        }
    }

    /**
     * Saves or updates the user profile in Firestore.
     */
    suspend fun saveUserProfile(user: UserModelData): Result<Unit> {
        return try {
            db.collection(AppConstants.COLLECTION_USERS)
                .document(user.uid)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile", e)
            Result.failure(e)
        }
    }
}
