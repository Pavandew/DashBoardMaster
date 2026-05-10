package com.example.masterdashboard.signup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.signup.SignUpUiState
import com.example.masterdashboard.signup.SignUpValidator
import com.example.masterdashboard.signup.ValidationResult
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpViewModel : ViewModel() {

    private val db = Firebase.firestore

    private val _signUpState =
        MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)

    val signUpState: StateFlow<SignUpUiState> = _signUpState

    fun signUpUser(
        fullName: String,
        phone: String,
        password: String,
        confirmPassword: String
    ) {

        val result = SignUpValidator.validate(
            fullName,
            phone,
            password,
            confirmPassword
        )

        when (result) {

            is ValidationResult.Error -> {

                _signUpState.value =
                    SignUpUiState.Error(
                        result.field,
                        result.message
                    )
                return
            }

            is ValidationResult.Success -> {

                viewModelScope.launch {

                    _signUpState.value =
                        SignUpUiState.Loading

                    try {

                        val snapshot = db.collection("users")
                            .whereEqualTo("phone", phone)
                            .get()
                            .await()

                        if (!snapshot.isEmpty) {

                            _signUpState.value =
                                SignUpUiState.Error(
                                    "phone",
                                    "Phone already registered"
                                )
                            return@launch
                        }

                        _signUpState.value =
                            SignUpUiState.Success(
                                message = "OTP Sent",
                                phone = phone
                            )

                    } catch (e: Exception) {

                        _signUpState.value =
                            SignUpUiState.Error(
                                "",
                                e.message ?: "Something went wrong"
                            )
                    }
                }
            }
        }
    }

    fun resetState() {
        _signUpState.value = SignUpUiState.Idle
    }
}