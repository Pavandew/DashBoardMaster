package com.example.masterdashboard.login.repo

import android.app.Activity
import android.util.Log
import com.example.masterdashboard.login.models.UserModelData
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "AuthRepository"

    /**
     * Finds a user by their phone number, checking both 'mobile' and 'phone' fields.
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
            Result.failure(e)
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

    /**
     * Hashes the password using the Java hashCode algorithm.
     */
    fun hashPassword(password: String): String {
        return password.hashCode().toString()
    }
}
