package com.example.masterdashboard.utils

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Universal Path Helper for Restaurant Outlets in Firestore.
 * Points directly to `restaurants/{restaurantId}` with full debug logging.
 */
object RestaurantPathHelper {

    private const val TAG = "RestaurantPathHelper"
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Returns a DocumentReference for `restaurants/{restaurantId}` with logging.
     */
    fun getOutletDocRef(restaurantId: String): DocumentReference {
        val id = restaurantId.ifEmpty { "unknown" }
        val docRef = firestore.collection(AppConstants.COLLECTION_RESTAURANTS).document(id)
        Log.d(TAG, "getOutletDocRef: Resolved DocumentReference path -> '${docRef.path}'")
        return docRef
    }

    /**
     * Resolves DocumentReference for `restaurants/{restaurantId}` with logging.
     */
    fun getRestaurantDocRef(restaurantId: String, ownerUid: String = restaurantId): DocumentReference {
        val targetId = when {
            restaurantId.isNotEmpty() -> restaurantId
            ownerUid.isNotEmpty() -> ownerUid
            else -> "unknown"
        }
        val docRef = firestore.collection(AppConstants.COLLECTION_RESTAURANTS).document(targetId)
        Log.d(TAG, "getRestaurantDocRef: Resolved path -> '${docRef.path}' (input restaurantId='$restaurantId', ownerUid='$ownerUid')")
        return docRef
    }
}
