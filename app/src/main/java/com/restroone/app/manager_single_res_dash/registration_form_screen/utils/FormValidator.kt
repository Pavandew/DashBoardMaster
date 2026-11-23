package com.restroone.app.manager_single_res_dash.registration_form_screen.utils

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
        val cleanPhone = phone.replace("\\s+".toRegex(), "").replace("-", "")
        if (cleanPhone.isEmpty()) return "Mobile number is required"
        val phonePattern = "^[6-9]\\d{9}$"
        return if (!cleanPhone.matches(Regex(phonePattern))) "Invalid 10-digit mobile number" else null
    }

    fun validatePinCode(pin: String): String? {
        val cleanPin = pin.trim()
        if (cleanPin.isEmpty()) return "PIN code is required"
        return if (cleanPin.length != 6) "PIN code must be 6 digits" else null
    }

    fun validateGst(gst: String): String? {
        val cleanGst = gst.trim().uppercase()
        if (cleanGst.isEmpty()) return null // Optional field (turnover < 20L or un-registered)
        val gstPattern = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"
        return if (!cleanGst.matches(Regex(gstPattern))) "Invalid 15-digit GSTIN format" else null
    }

    fun validatePan(pan: String): String? {
        val cleanPan = pan.trim().uppercase()
        if (cleanPan.isEmpty()) return null // Optional field
        val panPattern = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$"
        return if (!cleanPan.matches(Regex(panPattern))) "Invalid 10-character PAN format" else null
    }

    fun validateFssai(fssai: String): String? {
        val cleanFssai = fssai.trim()
        if (cleanFssai.isEmpty()) return null // Optional field (can be added later in Settings)
        val fssaiPattern = "^[0-9]{14}$"
        return if (!cleanFssai.matches(Regex(fssaiPattern))) "FSSAI must be 14 numeric digits" else null
    }
}
