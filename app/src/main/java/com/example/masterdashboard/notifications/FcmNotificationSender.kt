package com.example.masterdashboard.notifications

import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class to send FCM notifications directly from the app.
 * NOTE: In a production app, this should be handled by a backend server or Cloud Functions.
 */
object FcmNotificationSender {

    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"
    
    // TODO: Replace with your actual FCM Server Key from Firebase Console (Project Settings -> Cloud Messaging)
    private const val SERVER_KEY = "AIzaSyBFYWttGUJFLavZx97zP2YBsFLxEMKTdwM"

    suspend fun sendNotification(
        targetTokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>? = null
    ) {
        if (targetTokens.isEmpty()) return

        withContext(Dispatchers.IO) {
            try {
                val url = URL(FCM_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "key=$SERVER_KEY")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val payload = JsonObject().apply {
                    // For multiple tokens, use registration_ids
                    if (targetTokens.size == 1) {
                        addProperty("to", targetTokens[0])
                    } else {
                        val tokensArray = com.google.gson.JsonArray()
                        targetTokens.forEach { tokensArray.add(it) }
                        add("registration_ids", tokensArray)
                    }

                    // Notification payload (visible to user)
                    val notification = JsonObject()
                    notification.addProperty("title", title)
                    notification.addProperty("body", body)
                    notification.addProperty("click_action", "OPEN_ACTIVITY_1") // Optional
                    add("notification", notification)

                    // Data payload (passed to MyFirebaseMessagingService)
                    data?.let {
                        val dataObj = JsonObject()
                        it.forEach { (key, value) -> dataObj.addProperty(key, value) }
                        add("data", dataObj)
                    }
                }

                val wr = OutputStreamWriter(conn.outputStream)
                wr.write(payload.toString())
                wr.flush()
                wr.close()

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    Log.i("FCM_SENDER", "Successfully sent notification to ${targetTokens.size} devices.")
                } else {
                    Log.e("FCM_SENDER", "Failed to send FCM. Response Code: $responseCode")
                }

            } catch (e: Exception) {
                Log.e("FCM_SENDER", "Error sending FCM notification", e)
            }
        }
    }
}
