package com.example.masterdashboard.master_dash.home.dashboard.repo

import android.util.Log
import com.example.masterdashboard.master_dash.home.dashboard.model.ActivityLogsModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DashboardRepository {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "DashboardRepository"

    // Dashboard counts
    fun loadDashboardData(
        onSuccess: (
            total: Int,
            active: Int,
            disabled: Int,
            admin: Int
        ) -> Unit
    ) {
        Log.i(TAG, "loadDashboardData: Fetching restaurant and admin counts")

        db.collection("restaurants")
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Restaurants fetch success: ${result.size()} documents found")

                var total = 0
                var active = 0
                var disabled = 0

                for (doc in result) {

                    total++

                    if (doc.getString("status") == "active") {
                        active++
                    } else {
                        disabled++
                    }
                }

                db.collection("admins")
                    .get()
                    .addOnSuccessListener { adminResult ->
                        Log.d(TAG, "Admins fetch success: ${adminResult.size()} documents found")

                        onSuccess(
                            total,
                            active,
                            disabled,
                            adminResult.size()
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Admins fetch failure", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Restaurants fetch failure", e)
            }
    }

    // Latest logs
    fun loadRecentLogs(
        callback: (List<ActivityLogsModel>) -> Unit
    ) {
        Log.i(TAG, "loadRecentLogs: Fetching latest activity logs")

        db.collection("logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(4)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Logs fetch success: ${result.size()} documents found")

                val list = arrayListOf<ActivityLogsModel>()

                for (doc in result) {

                    list.add(
                        ActivityLogsModel(
                            id = doc.id,
                            type = doc.getString("type") ?: "",
                            title = doc.getString("title") ?: "",
                            subtitle = doc.getString("subtitle") ?: "",
                            time = doc.getLong("timestamp") ?: 0L
                        )
                    )
                }

                callback(list)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Logs fetch failure", e)
            }
    }
}
