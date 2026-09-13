package com.example.masterdashboard.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.login.repo.AuthRepository
import com.example.masterdashboard.login.uistate.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel: ViewModel() {

    private val repository = AuthRepository()

    private val _loginState =
        MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState : StateFlow<LoginUiState> = _loginState

    fun loginUser(phone: String, password: String) {

        if (phone.isBlank() && password.isBlank()) {
            _loginState.value = LoginUiState.Error(
                field = "BOTH",
                message = "Phone number and password are required"
            )
            return
        }

        if (phone.isBlank()) {
            _loginState.value = LoginUiState.Error(
                field = "PHONE",
                message = "Phone number is required"
            )
            return
        }

        if (password.isBlank()) {
            _loginState.value = LoginUiState.Error(
                field = "PASSWORD",
                message = "Password is required"
            )
            return
        }

        viewModelScope.launch {

            _loginState.value = LoginUiState.Loading

            repository.findUserByPhone(phone).fold(
                onSuccess = { user ->
                    if (user == null) {
                        Log.w("LoginVM", "User not found for: $phone")
                        _loginState.value = LoginUiState.Error(
                            field = "PHONE",
                            message = "Mobile number not registered"
                        )
                        return@launch
                    }

                    // Log all document fields for debugging
                    Log.d("LoginVM", "User data: $user")

                    // password check
                    if (user.passwordHash != repository.hashPassword(password)) {
                        _loginState.value = LoginUiState.Error(
                            field = "PASSWORD",
                            message = "Incorrect password"
                        )
                        return@launch
                    }

                    // OTP Verification check
                    if (!user.isVerified) {
                        _loginState.value = LoginUiState.Error(
                            field = "PHONE",
                            message = "Account not verified"
                        )
                        return@launch
                    }

                    _loginState.value = LoginUiState.Success(
                        message = "Login successful",
                        uid = user.uid,
                        role = user.role,
                        portalType = user.portalType,
                        isRestaurantSetup = user.isActuallySetup(),
                        restaurantId = user.restaurantId,
                        fullName = user.fullName
                    )
                },
                onFailure = { e ->
                    _loginState.value = LoginUiState.Error(message = e.message ?: "Login failed")
                }
            )
        }
    }

    fun findUserByPhoneForReset(phone: String) {
        val cleanPhone = phone.trim()
        if (cleanPhone.isBlank()) {
            _loginState.value = LoginUiState.Error(message = "Enter Mobile Number")
            return
        }

        _loginState.value = LoginUiState.Loading

        viewModelScope.launch {
            repository.findUserByPhone(cleanPhone).fold(
                onSuccess = { user ->
                    if (user == null) {
                        Log.w("LoginVM", "User not found for reset: $cleanPhone")
                        _loginState.value = LoginUiState.Error(message = "Mobile number not registered")
                        return@launch
                    }

                    Log.d("LoginVM", "User found for reset: ${user.uid}")
                    val formattedPhone = if (cleanPhone.startsWith("+91")) cleanPhone else "+91$cleanPhone"
                    _loginState.value = LoginUiState.ResetUserFound(
                        uid = user.uid,
                        role = user.role,
                        phone = formattedPhone,
                        fullName = user.fullName
                    )
                },
                onFailure = { e ->
                    _loginState.value = LoginUiState.Error(message = e.message ?: "User lookup failed")
                }
            )
        }
    }

    fun reset() {
        _loginState.value = LoginUiState.Idle
    }
}
