package com.example.masterdashboard.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.login.uistate.LoginUiState
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel: ViewModel() {

    private val db = FirebaseFirestore.getInstance()

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

            val formattedPhone =
                if(phone.startsWith("+91")) {
                    phone
                } else {
                    "+91$phone"
                }


            try {
                val snapshot = db.collection(AppConstants.COLLECTION_USERS)
                    .whereEqualTo(AppConstants.FIELD_PHONE, formattedPhone)
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
                
                // Use a more robust way to get values to handle potential type or naming mismatches
                val storedPassword = userDoc.get(AppConstants.FIELD_PASSWORD_HASH)?.toString() ?: ""

                // Firestore often strips the 'is' prefix from boolean fields (isVerified -> verified)
                // We check both keys and handle cases where it might be stored as a String
                val isVerifiedValue = userDoc.get(AppConstants.FIELD_IS_VERIFIED) ?: userDoc.get("verified")
                val isVerified = when (isVerifiedValue) {
                    is Boolean -> isVerifiedValue
                    is String -> isVerifiedValue.toBoolean()
                    else -> false
                }

                val uid = userDoc.getString(AppConstants.FIELD_UID) ?: ""
                val role = userDoc.getString(AppConstants.FIELD_ROLE) ?: ""
                val portalType = userDoc.getString(AppConstants.FIELD_PORTAL_TYPE) ?: ""

                // step 2 password chek
                if(storedPassword != password.hashCode().toString()) {
                    _loginState.value =
                        LoginUiState.Error(field = "",
                            "Wrong password")
                    return@launch
                }

                // STEP 3:  OTP Verification check
                if (!isVerified) {
                    _loginState.value =
                        LoginUiState.Error(field = "",
                            "Account not verified")
                    return@launch
                }

                _loginState.value =
                    LoginUiState.Success(
                        message = "Login successful",
                        uid = uid,
                        role = role,
                        portalType = portalType
                    )

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