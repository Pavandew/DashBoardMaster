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
     * Finds a user by their phone number, checking both 'mobile' and 'phone' fields in Firestore.
     */
    suspend fun findUserByPhone(phone: String): Result<UserModelData?> {
        return try {
            val formattedPhone = if (phone.startsWith("+91")) phone else "+91$phone"
            
            // Try querying by 'mobile' first
            var snapshot = db.collection(AppConstants.COLLECTION_USERS)
                .whereEqualTo(AppConstants.FIELD_MOBILE, formattedPhone)
                .get()
                .await()

            // Fallback: Try querying by 'phone'
            if (snapshot.isEmpty) {
                snapshot = db.collection(AppConstants.COLLECTION_USERS)
                    .whereEqualTo("phone", formattedPhone)
                    .get()
                    .await()
            }

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val user = snapshot.documents[0].toObject(UserModelData::class.java)
                Result.success(user)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding user by phone", e)
            val formattedMsg = NetworkUtils.formatErrorMessage(e)
            Result.failure(Exception(formattedMsg))
        }
    }

    /**
     * Checks if a phone number is registered by looking up an active user profile in Firestore.
     */
    suspend fun isPhoneRegistered(phone: String): Result<Boolean> {
        return try {
            val userResult = findUserByPhone(phone)
            if (userResult.isSuccess) {
                val user = userResult.getOrNull()
                Result.success(user != null && user.isVerified)
            } else {
                val formattedPhone = if (phone.startsWith("+91")) phone else "+91$phone"
                val response = FirebaseFunctions.getInstance()
                    .getHttpsCallable("isMobileRegistered")
                    .call(mapOf("mobile" to formattedPhone))
                    .await()
                    .data as? Map<*, *>
                Result.success(response?.get("registered") == true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "isPhoneRegistered exception: ${e.message}")
            Result.success(false)
        }
    }

    /**
     * Gets a user profile directly by their Firebase Auth UID.
     * Verifies that the underlying restaurant document exists to auto-heal setup status.
     */
    suspend fun getUserProfileByUid(uid: String): Result<UserModelData?> {
        return try {
            val doc = db.collection(AppConstants.COLLECTION_USERS)
                .document(uid)
                .get()
                .await()

            if (doc.exists()) {
                val user = doc.toObject(UserModelData::class.java)
                if (user != null && (user.isSetupComplete || user.restaurantId.isNotEmpty())) {
                    val targetResId = user.restaurantId.ifEmpty { uid }
                    val resDoc = db.collection(AppConstants.COLLECTION_RESTAURANTS)
                        .document(targetResId)
                        .get()
                        .await()

                    if (!resDoc.exists()) {
                        Log.w(TAG, "Restaurant document '$targetResId' missing for user '$uid'. Auto-healing setup status to false.")
                        try {
                            db.collection(AppConstants.COLLECTION_USERS)
                                .document(uid)
                                .update("isSetupComplete", false, "restaurantId", "")
                                .await()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to update Firestore user setup state: ${e.message}")
                        }
                        return Result.success(user.copy(isSetupComplete = false, restaurantId = ""))
                    }
                }
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
        
        auth.firebaseAuthSettings.forceRecaptchaFlowForTesting(false)

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Resends Firebase Phone Number verification with ForceResendingToken.
     */
    fun resendOtp(
        phone: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        val formattedPhone = if (phone.startsWith("+91")) phone else "+91$phone"

        auth.firebaseAuthSettings.forceRecaptchaFlowForTesting(false)

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (token != null) {
            builder.setForceResendingToken(token)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
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
