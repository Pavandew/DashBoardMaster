package com.example.masterdashboard.master_dash.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.master_dash.login.uistate.LoginUiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel: ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
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


            try {
                val snapshot = db.collection("users")
                    .whereEqualTo("phone", "+91$phone")
                    .get()
                    .await()

                if(snapshot.isEmpty) {

                    _loginState.value =
                        LoginUiState.Error(
                                message = "User not found"
                        )
                    return@launch

                }

                val userDoc = snapshot.documents[0]
                val storedPassword =
                    userDoc.getString("passwordHash") ?: ""

                val isVerified =
                    userDoc.getBoolean("isVerified") ?: false

                // step 2 password chek
                if(storedPassword != password.hashCode().toString()) {
                    _loginState.value =
                        LoginUiState.Error(field = "",
                            "Wrong password")
                    return@launch
                }

                // STEP 3: optional OTP check
                if (!isVerified) {
                    _loginState.value =
                        LoginUiState.Error(field = "",
                            "Account not verified")
                    return@launch
                }

                _loginState.value =
                    LoginUiState.Success("Login successful")

            } catch (e: Exception) {
                _loginState.value =
                    LoginUiState.Error(
                        message = e.message ?: "Login failed"
                    )
            }
        }
    }

    fun reset() {
        _loginState.value = LoginUiState.Idle
    }

}