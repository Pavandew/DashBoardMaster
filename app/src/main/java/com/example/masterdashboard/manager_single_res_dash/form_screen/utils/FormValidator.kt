package com.example.masterdashboard.manager_single_res_dash.form_screen.utils

import android.util.Patterns

object FormValidator {

    fun validateNotEmpty(value: String, fieldName: String): String? {
        return if (value.trim().isEmpty()) "$fieldName is required" else null
    }

    fun validateEmail(email: String): String? {
        if (email.trim().isEmpty()) return "Email is required"
        return if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Invalid email format" else null
    }

    fun validatePhone(phone: String): String? {
        if (phone.trim().isEmpty()) return "Mobile number is required"
        val phonePattern = "^[6-9]\\d{9}$"
        return if (!phone.matches(Regex(phonePattern))) "Invalid 10-digit mobile number" else null
    }

    fun validatePinCode(pin: String): String? {
        if (pin.trim().isEmpty()) return "PIN code is required"
        return if (pin.length != 6) "PIN code must be 6 digits" else null
    }

    fun validateGst(gst: String): String? {
        if (gst.isEmpty()) return null // Optional field
        val gstPattern = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"
        return if (!gst.matches(Regex(gstPattern))) "Invalid GST format" else null
    }

    fun validatePan(pan: String): String? {
        if (pan.isEmpty()) return null // Optional field
        val panPattern = "[A-Z]{5}[0-9]{4}[A-Z]{1}"
        return if (!pan.matches(Regex(panPattern))) "Invalid PAN format" else null
    }

    fun validateFssai(fssai: String): String? {
        if (fssai.isEmpty()) return null // Optional field
        return if (fssai.length != 14) "FSSAI must be 14 digits" else null
    }
}
