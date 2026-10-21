package com.restroone.app.manager_single_res_dash.repo

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.restroone.app.manager_single_res_dash.models.StaffDataModel
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.ImageUtils
import com.restroone.app.utils.RestaurantPathHelper
import kotlinx.coroutines.tasks.await

class StaffFormRepository {
    private val functions = FirebaseFunctions.getInstance()
    private val storage = FirebaseStorage.getInstance()

    companion object {
        private const val TAG = "StaffFormRepository"
    }

    suspend fun saveStaffToFirestore(context: Context, staff: StaffDataModel, ownerUid: String) : Result<Unit> {
        return try {
            if (ownerUid.isBlank()) {
                return Result.failure(Exception("Owner UID is missing"))
            }

            val preparedStaff = uploadStaffMediaIfLocal(context, ownerUid, staff)

            functions.getHttpsCallable("createStaff")
                .call(
                    mapOf(
                        "restaurantId" to ownerUid,
                        "staff" to preparedStaff.toMap(),
                        "password" to preparedStaff.password
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveStaffToFirestore error", e)
            Result.failure(e)
        }
    }

    suspend fun isMobileNumberRegistered(ownerUid: String, mobileNumber: String): Result<Boolean> = runCatching {
        val querySnapshot = RestaurantPathHelper.getOutletDocRef(ownerUid)
            .collection(AppConstants.COLLECTION_STAFF)
            .whereEqualTo(AppConstants.FIELD_MOBILE, mobileNumber)
            .get()
            .await()

        !querySnapshot.isEmpty
    }

    suspend fun updateStaffInFirestore(context: Context, staff: StaffDataModel, ownerUid: String): Result<Unit> {
        return try {
            if (ownerUid.isBlank() || staff.id.isBlank()) {
                return Result.failure(Exception("Required IDs are missing"))
            }

            val preparedStaff = uploadStaffMediaIfLocal(context, ownerUid, staff)

            functions.getHttpsCallable("updateStaff")
                .call(
                    mapOf(
                        "restaurantId" to ownerUid,
                        "staffDocId" to preparedStaff.id,
                        "staff" to preparedStaff.toMap()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateStaffInFirestore error", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadStaffMediaIfLocal(context: Context, ownerUid: String, staff: StaffDataModel): StaffDataModel {
        var updatedStaff = staff

        val targetStaffId = if (staff.staffId.isNotEmpty()) staff.staffId else staff.id.ifEmpty { "temp_${System.currentTimeMillis()}" }

        // 1. Upload Profile Photo if local URI
        if (staff.profilePhotoUrl.isNotEmpty() && (staff.profilePhotoUrl.startsWith("content:") || staff.profilePhotoUrl.startsWith("file:"))) {
            try {
                Log.i(TAG, "Uploading staff photo to Firebase Storage for ID: $targetStaffId...")
                val uri = Uri.parse(staff.profilePhotoUrl)
                val compressedBytes = ImageUtils.compressImage(context, uri)
                val storageRef = storage.reference.child("restaurants/$ownerUid/staff/$targetStaffId/photo.jpg")

                if (compressedBytes != null) {
                    storageRef.putBytes(compressedBytes).await()
                } else {
                    storageRef.putFile(uri).await()
                }

                val downloadUrl = storageRef.downloadUrl.await().toString()
                Log.i(TAG, "Staff photo uploaded successfully: $downloadUrl")
                updatedStaff = updatedStaff.copy(profilePhotoUrl = downloadUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload staff photo to Storage", e)
            }
        }

        // 2. Upload ID Document Image if local URI
        if (staff.documentImageUrl.isNotEmpty() && (staff.documentImageUrl.startsWith("content:") || staff.documentImageUrl.startsWith("file:"))) {
            try {
                Log.i(TAG, "Uploading staff ID document image to Firebase Storage for ID: $targetStaffId...")
                val uri = Uri.parse(staff.documentImageUrl)
                val compressedBytes = ImageUtils.compressImage(context, uri)
                val storageRef = storage.reference.child("restaurants/$ownerUid/staff/$targetStaffId/document.jpg")

                if (compressedBytes != null) {
                    storageRef.putBytes(compressedBytes).await()
                } else {
                    storageRef.putFile(uri).await()
                }

                val downloadUrl = storageRef.downloadUrl.await().toString()
                Log.i(TAG, "Staff document image uploaded successfully: $downloadUrl")
                updatedStaff = updatedStaff.copy(documentImageUrl = downloadUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload staff document image to Storage", e)
            }
        }

        return updatedStaff
    }
}