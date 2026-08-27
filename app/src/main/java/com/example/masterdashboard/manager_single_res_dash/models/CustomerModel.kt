package com.example.masterdashboard.manager_single_res_dash.models

import com.google.firebase.Timestamp
import java.io.Serializable

data class CustomerModel(
    val customerId: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val lastVisit: Timestamp? = null,
    val visitCount: Int = 0,
    val totalSpent: Double = 0.0
) : Serializable