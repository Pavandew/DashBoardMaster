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

    private const val TAG = "FcmNotificationSender"
    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"
    
    // TODO: Consider moving this to a more secure location or a backend server
    private const val SERVER_KEY = "AIzaSyBFYWttGUJFLavZx97zP2YBsFLxEMKTdwM"

    /**
     * Sends an FCM notification using the Legacy HTTP API.
     * Note: Migration to HTTP v1 is recommended for future-proofing.
     */
    suspend fun sendNotification(
        targetTokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>? = null
    ) {
        val tokens = targetTokens.filter { it.isNotBlank() }
        if (tokens.isEmpty()) {
            Log.w(TAG, "sendNotification: No valid target tokens provided.")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val url = URL(FCM_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Authorization", "key=$SERVER_KEY")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }

                val payload = JsonObject().apply {
                    if (tokens.size == 1) {
                        addProperty("to", tokens[0])
                    } else {
                        val tokensArray = com.google.gson.JsonArray()
                        tokens.forEach { tokensArray.add(it) }
                        add("registration_ids", tokensArray)
                    }

                    val notification = JsonObject().apply {
                        addProperty("title", title)
                        addProperty("body", body)
                        addProperty("sound", "default")
                        addProperty("click_action", "OPEN_ACTIVITY_1")
                    }
                    add("notification", notification)

                    data?.let {
                        val dataObj = JsonObject()
                        it.forEach { (key, value) -> dataObj.addProperty(key, value) }
                        add("data", dataObj)
                    }
                }

                conn.outputStream.use { os ->
                    OutputStreamWriter(os, "UTF-8").use { writer ->
                        writer.write(payload.toString())
                        writer.flush()
                    }
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.i(TAG, "Successfully sent notification to ${tokens.size} devices. Response: $response")
                } else {
                    val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                    Log.e(TAG, "Failed to send FCM. Response Code: $responseCode, Error: $errorResponse")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error sending FCM notification", e)
            }
        }
    }
}
