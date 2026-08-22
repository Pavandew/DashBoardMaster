package com.example.masterdashboard.notifications

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(applicationContext)
        sessionManager = SessionManager(applicationContext)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM_SERVICE", "From: ${remoteMessage.from}")

        // Handle Notification Payload
        remoteMessage.notification?.let {
            Log.d("FCM_SERVICE", "Message Notification Body: ${it.body}")
            notificationHelper.showNotification(it.title, it.body, remoteMessage.data)
        }

        // Handle Data Payload (If no notification object is present)
        if (remoteMessage.data.isNotEmpty() && remoteMessage.notification == null) {
            val title = remoteMessage.data["title"] ?: remoteMessage.data["heading"]
            val message = remoteMessage.data["message"] ?: remoteMessage.data["body"]
            notificationHelper.showNotification(title, message, remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_SERVICE", "Refreshed token: $token")
        
        val uid = sessionManager.getUid()
        val staffDocId = sessionManager.getStaffDocId()

        if (uid.isNotEmpty()) {
            if (staffDocId.isNotEmpty()) {
                // If staffDocId exists, it's a staff member (stored under users/{ownerUid}/staff/{staffDocId})
                updateStaffToken(uid, staffDocId, token)
            } else {
                // If only uid exists, it's a manager/owner (stored under users/{uid})
                updateUserToken(uid, token)
            }
        }
    }

    private fun updateUserToken(uid: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection(AppConstants.COLLECTION_USERS)
            .document(uid)
            .update(AppConstants.FIELD_FCM_TOKEN, token)
            .addOnSuccessListener { Log.d("FCM_SERVICE", "User token updated: $uid") }
            .addOnFailureListener { e -> Log.e("FCM_SERVICE", "Error updating user token", e) }
    }

    private fun updateStaffToken(ownerUid: String, staffDocId: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.COLLECTION_STAFF)
            .document(staffDocId)
            .update(AppConstants.FIELD_FCM_TOKEN, token)
            .addOnSuccessListener { Log.d("FCM_SERVICE", "Staff token updated: $staffDocId") }
            .addOnFailureListener { e -> Log.e("FCM_SERVICE", "Error updating staff token", e) }
    }
}
