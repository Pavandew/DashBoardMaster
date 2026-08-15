package com.example.masterdashboard.utils

sealed class ValidationResult {

    object Success : ValidationResult()

    data class Error(
        val field: String,
        val message: String
    ) : ValidationResult()
}

object SignUpValidator {

    fun validate(
        fullName: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): ValidationResult {

        val name = fullName.trim()
        val mobile = phone.trim()

        return when {

            // Full name
            name.isBlank() ->
                ValidationResult.Error(
                    field = AppConstants.FIELD_FULL_NAME,
                    message = "Enter full name"
                )

            name.length < 3 ->
                ValidationResult.Error(
                    field = AppConstants.FIELD_FULL_NAME,
                    message = "Name must be at least 3 characters"
                )

            !name.matches(Regex("^[a-zA-Z ]+$")) ->
                ValidationResult.Error(
                    field = AppConstants.FIELD_FULL_NAME,
                    message = "Only letters allowed"
                )

            // Mobile
            mobile.isBlank() ->
                ValidationResult.Error(
                    field = AppConstants.FIELD_MOBILE,
                    message = "Enter mobile number"
                )

            !mobile.matches(Regex("^[0-9]{10}$")) ->
                ValidationResult.Error(
                    field = AppConstants.FIELD_MOBILE,
                    message = "Enter valid 10-digit mobile number"
                )

            // Password
            password.isBlank() ->
                ValidationResult.Error(
                    field = AppConstants.FIELD_PASSWORD,
                    message = "Enter password"
                )

            password.length < 8 ->
                ValidationResult.Error(
                    field = AppConstants.FIELD_PASSWORD,
                    message = "Password must be at least 8 characters"
                )

            !password.matches(Regex(".*[A-Za-z].*")) ->
                ValidationResult.Error(
                    field = AppConstants.FIELD_PASSWORD,
                    message = "Password must contain a letter"
                )

            !password.matches(Regex(".*\\d.*")) ->
                ValidationResult.Error(
                    field = AppConstants.FIELD_PASSWORD,
                    message = "Password must contain a number"
                )

            // Confirm password
            confirmPassword.isBlank() ->
                ValidationResult.Error(
                    field = "confirmPassword",
                    message = "Confirm your password"
                )

            password != confirmPassword ->
                ValidationResult.Error(
                    field = "confirmPassword",
                    message = "Passwords do not match"
                )

            else -> ValidationResult.Success
        }
    }
}
