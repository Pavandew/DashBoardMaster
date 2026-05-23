package com.example.masterdashboard.master_dash.home.dashboard.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.masterdashboard.master_dash.home.dashboard.model.ActivityLogsModel
import com.example.masterdashboard.master_dash.home.dashboard.repo.DashboardRepository

class DashboardViewModel : ViewModel() {

    private val repo = DashboardRepository()
    private val TAG = "DashboardViewModel"

    val total = MutableLiveData<Int>()
    val active = MutableLiveData<Int>()
    val disabled = MutableLiveData<Int>()
    val admins = MutableLiveData<Int>()

    val recentLogs =
        MutableLiveData<List<ActivityLogsModel>>()

    fun loadData() {
        Log.i(TAG, "loadData: Initiating dashboard data fetch from Repository")

        repo.loadDashboardData { t, a, d, ad ->
            Log.d(TAG, "Received counts: total=$t, active=$a, disabled=$d, admins=$ad")
            total.value = t
            active.value = a
            disabled.value = d
            admins.value = ad
        }

        repo.loadRecentLogs {
            Log.d(TAG, "Received recent logs, count = ${it.size}")
            recentLogs.value = it
        }
    }
}
