package com.example.masterdashboard.login.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.example.masterdashboard.R
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.databinding.ActivityLoginBinding
import com.example.masterdashboard.master_dash.MasterHomeActivity
import com.example.masterdashboard.login.uistate.LoginUiState
import com.example.masterdashboard.login.viewmodel.LoginViewModel
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.SingleResOwnerHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.login.utils.AppConstants
import com.example.masterdashboard.login.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private val TAG = "LoginActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Login Activity started")

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        clickListeners()
        observeState()

    }

    private fun clickListeners() {
        val selectedPortal =
            sessionManager.getSelectedPortal()

        // Hide Create Account for staff
        if(selectedPortal == AppConstants.PORTAL_STAFF) {
            binding.loginItem.loginCreateAccTv.visibility =
                View.GONE
            binding.loginItem.loginDontHaveCreateAccTv.visibility =
                View.GONE
            binding.loginItem.loginBtn.backgroundTintList =
                getColorStateList(R.color.orange_gradient_start)
            binding.root.setBackgroundResource(R.drawable.app_background_orange)
        }
        // Hide Create Account for staff
        if(selectedPortal == AppConstants.PORTAL_MULTI_RESTAURANT) {
            binding.loginItem.loginBtn.backgroundTintList =
                getColorStateList(R.color.blue_gradient_start)
            binding.root.setBackgroundResource(R.drawable.app_backround_blue)
        }

        binding.loginItem.loginCreateAccTv.setOnClickListener {
            val selectedPortal = sessionManager.getSelectedPortal()
            Log.d(TAG, "Navigating to SignUpActivity. Current portal in session: $selectedPortal")
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.loginItem.loginBtn.setOnClickListener {

            val phone =
                binding.loginItem.loginPhoneEt.text.toString().trim()

            val password =
                binding.loginItem.loginPasswordEt.text.toString().trim()

            Log.i(TAG, "Attempting login for phone: $phone")
            viewModel.loginUser(phone, password)
        }
    }

    private fun observeState() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.loginState.collect { state ->
                    Log.d(TAG, "Observed state: ${state::class.simpleName}")

                    when(state) {


                        is LoginUiState.Idle -> {}

                        is LoginUiState.Loading -> {

                            binding.loginItem.loginBtn.isEnabled = false
                            binding.loginItem.loginBtn.text = "Logging in... "
                        }

                        is LoginUiState.Success -> {

                            Log.i(TAG, "Login Successful: ${state.message}")

                            binding.loginItem.loginBtn.isEnabled = true
                            binding.loginItem.loginBtn.text = "Login"

                            val selectedPortal =
                                sessionManager.getSelectedPortal()

                            // Validate Portal Access
                            val isValidPortal =
                                validatePortalAccess(
                                    selectedPortal = selectedPortal,
                                    role = state.role
                                )

                            if (!isValidPortal) {

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Access denied for this portal",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@collect
                            }

                            // Save Session
                            val phone =
                                binding.loginItem.loginPhoneEt.text.toString().trim()

                            sessionManager.setLogin(
                                uid = state.uid,
                                role = state.role,
                                phone = phone,
                                name = state.fullName
                            )
                            
                            // NEW: Save setup status and ID from Firestore into local session
                            Log.i(TAG, "Setup Status Received: ${state.isRestaurantSetup}, ID: ${state.restaurantId}")
                            sessionManager.setRestaurantSetup(state.isRestaurantSetup)
                            if (state.restaurantId.isNotEmpty()) {
                                sessionManager.saveRestaurantId(state.restaurantId)
                            }

                            // NEW: Save FCM token to Firestore on login
                            saveFcmToken(state.uid)

                            Toast.makeText(
                                this@LoginActivity,
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()

                            // Open Dashboard Based On Role
                            navigateToDashboard(state.role, state.isRestaurantSetup)
                        }

                        is LoginUiState.Error -> {
                            Log.e(TAG, "Login Error: ${state.message}")

                            binding.loginItem.loginBtn.isEnabled = true
                            binding.loginItem.loginBtn.text = "Login"

                            Toast.makeText(
                                this@LoginActivity,
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            }
        }
    }

    private fun validatePortalAccess(
        selectedPortal: String,
        role: String
    ) : Boolean{

        return when(selectedPortal) {

            AppConstants.PORTAL_MULTI_RESTAURANT -> {
                role == AppConstants.ROLE_OWNER_MULTI
            }
            AppConstants.PORTAL_RESTAURANT -> {
                role == AppConstants.ROLE_OWNER_SINGLE || role == AppConstants.ROLE_MANAGER
            }
            AppConstants.PORTAL_STAFF -> {
                role == AppConstants.ROLE_STAFF
            }
            else -> false
        }
    }
    private fun navigateToDashboard(role: String, isSetup: Boolean) {

        val intent = when (role) {
            AppConstants.ROLE_OWNER_MULTI -> {
                Intent(this, MasterHomeActivity::class.java)
            }

            AppConstants.ROLE_OWNER_SINGLE -> {
                if (isSetup) {
                    Intent(this, ManagerHomeActivity::class.java)
                } else {
                    Intent(this, SingleResOwnerHomeActivity::class.java)
                }
            }

            AppConstants.ROLE_MANAGER -> {
                Intent(this, ManagerHomeActivity::class.java)
            }

            AppConstants.ROLE_STAFF -> {

                Intent(this, WaiterHomeActivity::class.java)
            }

            else -> {
                Toast.makeText(this, "Invalid role: $role", Toast.LENGTH_SHORT).show()
                null
            }
        }

        intent?.let {
            startActivity(it)
            finish()
        }
    }

    private fun saveFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            // Update token in Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection(AppConstants.COLLECTION_USERS).document(uid)
                .update(AppConstants.FIELD_FCM_TOKEN, token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token saved successfully for user: $uid")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving FCM token", e)
                }
        }
    }

}
