package com.example.masterdashboard.login.viewmodel

import android.util.Log
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
                // Try querying by 'mobile' first
                var snapshot = db.collection(AppConstants.COLLECTION_USERS)
                    .whereEqualTo(AppConstants.FIELD_MOBILE, formattedPhone)
                    .get()
                    .await()

                // Fallback: Try querying by 'phone' if no user found with 'mobile'
                if (snapshot.isEmpty) {
                    Log.d("LoginVM", "User not found with 'mobile' field, trying 'phone' field fallback")
                    snapshot = db.collection(AppConstants.COLLECTION_USERS)
                        .whereEqualTo("phone", formattedPhone)
                        .get()
                        .await()
                }

                if(snapshot.isEmpty) {
                    Log.w("LoginVM", "User not found after checking both 'mobile' and 'phone' fields for: $formattedPhone")
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
                val fullName = userDoc.getString("fullName") ?: ""
                
                // DEBUG: Log all document fields to verify setup status
                Log.d("LoginVM", "User document fields: ${userDoc.data}")

                // NEW: Fetch restaurant setup status and ID
                val isSetupComplete = userDoc.getBoolean(AppConstants.FIELD_IS_SETUP_COMPLETE) ?: false
                val restaurantId = userDoc.getString(AppConstants.FIELD_RESTAURANT_ID) ?: ""
                
                Log.i("LoginVM", "Restaurant Setup Status: $isSetupComplete, ID: $restaurantId")

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
                        portalType = portalType,
                        isRestaurantSetup = isSetupComplete,
                        restaurantId = restaurantId,
                        fullName = fullName
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