package com.example.masterdashboard.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.login.repo.AuthRepository
import com.example.masterdashboard.login.uistate.SignUpUiState
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SignUpValidator
import com.example.masterdashboard.utils.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignUpViewModel : ViewModel() {

    private val repository = AuthRepository()
    
    companion object{
        const val TAG = "SignUpViewModel"
    }

    private val _signUpState =
        MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)

    val signUpState: StateFlow<SignUpUiState> = _signUpState

    fun signUpUser(
        fullName: String,
        phone: String,
        password: String,
        confirmPassword: String,
        role: String,
        portalType: String
    ) {
        Log.i(TAG, "signUpUser: Attempting to sign up user with phone: $phone")

        // validation
        val result = SignUpValidator.validate(
            fullName,
            phone,
            password,
            confirmPassword
        )

        when (result) {
            is ValidationResult.Error -> {
                Log.w(TAG, "signUpUser: Validation failed for field: ${result.field} - ${result.message}")
                _signUpState.value = SignUpUiState.Error(result.field, result.message)
                return
            }

            is ValidationResult.Success -> {
                Log.d(TAG, "signUpUser: Validation successful, checking if user already exists")

                viewModelScope.launch {
                    _signUpState.value = SignUpUiState.Loading

                    repository.findUserByPhone(phone).fold(
                        onSuccess = { user ->
                            if (user != null) {
                                Log.w(TAG, "signUpUser: Phone $phone is already registered")
                                _signUpState.value = SignUpUiState.Error(
                                    AppConstants.FIELD_MOBILE,
                                    "Mobile already registered"
                                )
                                return@launch
                            }

                            // Success -> Trigger OTP screen flow
                            Log.i(TAG, "signUpUser: User not found, proceeding to OTP for $phone")
                            _signUpState.value = SignUpUiState.Success(
                                message = "Ready to send OTP",
                                phone = phone,
                                role = role,
                                portalType = portalType
                            )
                        },
                        onFailure = { e ->
                            Log.e(TAG, "signUpUser: Repository error", e)
                            _signUpState.value = SignUpUiState.Error("", e.message ?: "Something went wrong")
                        }
                    )
                }
            }
        }
    }

    fun resetState() {
        _signUpState.value = SignUpUiState.Idle
    }
}
