package com.restroone.app.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.restroone.app.login.repo.AuthCloudFunctionRepository
import com.restroone.app.login.repo.AuthRepository
import com.restroone.app.login.uistate.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel: ViewModel() {

    private val repository = AuthRepository()
    private val cloudFunctionRepo = AuthCloudFunctionRepository()

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

            // 1. Authenticate via Secure Cloud Function (bcrypt verified server-side)
            cloudFunctionRepo.authenticateUser(phone, password).fold(
                onSuccess = { token ->
                    try {
                        // 2. Sign in with Custom Token to establish FirebaseAuth.currentUser.uid
                        val authResult = FirebaseAuth.getInstance().signInWithCustomToken(token).await()
                        val uid = authResult.user?.uid ?: throw Exception("UID is null after custom token sign-in")

                        // 3. Fetch user profile from Firestore users/{uid}
                        repository.getUserProfileByUid(uid).fold(
                            onSuccess = { user ->
                                if (user == null) {
                                    _loginState.value = LoginUiState.Error(
                                        field = "PASSWORD",
                                        message = "Invalid mobile number or password"
                                    )
                                    return@fold
                                }

                                Log.d("LoginVM", "User authenticated successfully")

                                // OTP Verification check
                                if (!user.isVerified) {
                                    _loginState.value = LoginUiState.Error(
                                        field = "PHONE",
                                        message = "Invalid mobile number or password"
                                    )
                                    return@fold
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
                                _loginState.value = LoginUiState.Error(message = "Invalid mobile number or password")
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("LoginVM", "Error signing in with custom token", e)
                        _loginState.value = LoginUiState.Error(message = e.message ?: "Authentication failed")
                    }
                },
                onFailure = { _ ->
                    _loginState.value = LoginUiState.Error(
                        field = "PASSWORD",
                        message = "Invalid mobile number or password"
                    )
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
            cloudFunctionRepo.findUserForReset(cleanPhone).fold(
                onSuccess = {
                    val formattedPhone = if (cleanPhone.startsWith("+91")) cleanPhone else "+91$cleanPhone"
                    _loginState.value = LoginUiState.ResetRequested(phone = formattedPhone)
                },
                onFailure = { _ ->
                    _loginState.value = LoginUiState.Error(
                        message = "If an account exists, continue with the verification process."
                    )
                }
            )
        }
    }

    fun reset() {
        _loginState.value = LoginUiState.Idle
    }
}
