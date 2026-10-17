package com.restroone.app.login.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.restroone.app.manager_single_res_dash.models.StaffDataModel
import com.restroone.app.utils.NetworkUtils
import kotlinx.coroutines.tasks.await

data class StaffAuthenticationResult(
    val staff: StaffDataModel,
    val restaurantId: String,
    val ownerUid: String
)

class StaffLoginRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun authenticateStaff(staffId: String, password: String): Result<StaffAuthenticationResult> =
        runCatching {
            val response = functions.getHttpsCallable("authenticateStaff")
                .call(mapOf("staffId" to staffId, "password" to password))
                .await()
                .data as? Map<*, *>
                ?: throw IllegalStateException("Invalid staff authentication response")

            val token = response["token"] as? String
                ?: throw IllegalStateException("Staff authentication token was missing")
            val uid = response["uid"] as? String
                ?: throw IllegalStateException("Staff authentication UID was missing")
            val signInResult = auth.signInWithCustomToken(token).await()
            if (signInResult.user?.uid != uid) {
                throw IllegalStateException("Authenticated staff identity did not match the server response")
            }

            StaffAuthenticationResult(
                staff = parseProfile(response["profile"] as? Map<*, *> ?: emptyMap<String, Any>()),
                restaurantId = response["restaurantId"] as? String
                    ?: throw IllegalStateException("Staff restaurant ID was missing"),
                ownerUid = response["ownerUid"] as? String
                    ?: throw IllegalStateException("Restaurant owner UID was missing")
            )
        }.onFailure { error ->
            Log.e("StaffLoginRepo", "Server-side staff authentication failed", error)
        }.mapFailure()

    private fun parseProfile(data: Map<*, *>): StaffDataModel {
        val permissions = (data["permissions"] as? List<*>)?.mapNotNull { it as? String }
            ?: emptyList()
        return StaffDataModel(
            id = data["id"] as? String ?: data["uid"] as? String ?: "",
            staffId = data["staffId"] as? String ?: "",
            staffName = data["staffName"] as? String ?: "",
            mobile = data["mobile"] as? String ?: "",
            email = data["email"] as? String ?: "",
            gender = data["gender"] as? String ?: "",
            role = data["role"] as? String ?: "",
            department = data["department"] as? String ?: "",
            joiningDate = data["joiningDate"] as? String ?: "",
            shift = data["shift"] as? String ?: "",
            salary = data["salary"] as? String ?: "",
            status = data["status"] as? String ?: "Active",
            permissions = permissions,
            documentType = data["documentType"] as? String ?: "",
            documentNumber = data["documentNumber"] as? String ?: ""
        )
    }

    private fun <T> Result<T>.mapFailure(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(Exception(NetworkUtils.formatErrorMessage(it))) }
        )
}
