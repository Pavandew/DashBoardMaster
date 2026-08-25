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

        if(phone.isBlank() || password.isBlank()) {
            _loginState.value =
                LoginUiState.Error(
                    message = "All fields required"
                )
            return
        }

        viewModelScope.launch {

            _loginState.value = LoginUiState.Loading

            repository.findUserByPhone(phone).fold(
                onSuccess = { user ->
                    if (user == null) {
                        Log.w("LoginVM", "User not found for: $phone")
                        _loginState.value = LoginUiState.Error(message = "User not found")
                        return@launch
                    }

                    // Log all document fields for debugging
                    Log.d("LoginVM", "User data: $user")

                    // password check
                    if (user.passwordHash != repository.hashPassword(password)) {
                        _loginState.value = LoginUiState.Error(field = "", "Wrong password")
                        return@launch
                    }

                    // OTP Verification check
                    if (!user.isVerified) {
                        _loginState.value = LoginUiState.Error(field = "", "Account not verified")
                        return@launch
                    }

                    _loginState.value = LoginUiState.Success(
                        message = "Login successful",
                        uid = user.uid,
                        role = user.role,
                        portalType = user.portalType,
                        isRestaurantSetup = user.isSetupComplete,
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

    fun reset() {
        _loginState.value = LoginUiState.Idle
    }
}
