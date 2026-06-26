package com.example.masterdashboard.staff_dash.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.databinding.ActivityStaffLoginBinding
import com.example.masterdashboard.staff_dash.billing_screens.BillingHomeActivity
import com.example.masterdashboard.staff_dash.kitchen_screens.KitchenHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.StaffHomeActivity
import com.example.masterdashboard.staff_dash.login.uistate.StaffLoginUiState
import com.example.masterdashboard.staff_dash.login.veiwModel.StaffLoginViewModel
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

class StaffLoginActivity : AppCompatActivity() {
    companion object{
        private const val TAG = "StaffLoginActivity"
    }

    private lateinit var binding: ActivityStaffLoginBinding
    // Instantiate ViewModels using standard delegation property extensions cleanly
    private val viewModel: StaffLoginViewModel by viewModels()
    private val sessionManager by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Staff Login Activity Initialized")

        // 1. Setup Edge-to-Edge and Inflate Binding
        enableEdgeToEdge()
        binding = ActivityStaffLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeLoginUiPipeline()
    }

    private fun setupClickListeners() {
        binding.staffLoginBtn.setOnClickListener {
            val enteredId = binding.staffEtInput.text.toString()
            val enteredPass = binding.staffPasswordEt.text.toString()

            // Reset validation markings before processing next validation cycle
            binding.textInputLayout.error = null
            binding.textInputLayout2.error = null

            viewModel.processStaffLogin(enteredId, enteredPass)
        }

        binding.staffForgotPass.setOnClickListener {
            Toast.makeText(this, "Please contact your Restaurant Manager to look up credentials.", Toast.LENGTH_LONG).show()
        }
    }

    private fun observeLoginUiPipeline() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->

                    when(state) {
                        is StaffLoginUiState.Idle -> {
                            binding.staffLoginBtn.isEnabled = true
                            binding.staffLoginBtn.text = "Login"
                        }

                        is StaffLoginUiState.Loading -> {
                            binding.staffLoginBtn.isEnabled = false
                            binding.staffLoginBtn.text = "Verifying..."
                        }

                        is StaffLoginUiState.ValidationError -> {
                            binding.staffLoginBtn.isEnabled = true
                            binding.staffLoginBtn.text = "Login"

                            if (state.message.contains("ID")) {
                                binding.textInputLayout.error = state.message
                            } else {
                                binding.textInputLayout2.error = state.message
                            }
                        }

                        is StaffLoginUiState.AuthError -> {
                            binding.staffLoginBtn.isEnabled = true
                            binding.staffLoginBtn.text = "Login"

                            if (state.message.contains("Password")) {
                                binding.textInputLayout2.error = state.message
                            } else {
                                Toast.makeText(this@StaffLoginActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                        }

                        // Inside your observeLoginUiPipeline() state collector matching logic:
                        is StaffLoginUiState.Success -> {
                            Log.i(TAG, "Navigation Verified: Appending sub-collection parameters to local Session Cache.")

                            // 1. Read the explicit string role parsed out of the Firestore Document
                            val authenticatedRole = state.role
                            Log.d(TAG, "Authenticated Role: $authenticatedRole")

                            // PERSIST LOGIN STATUS SO LOGINS SURVIVE CLOSING THE APP
                            // 2. Fire your complete setLogin method to switch the KEY_IS_LOGGED_IN boolean to true
                            sessionManager.setLogin(
                                uid = state.restaurantOwnerUid, // Parent Restaurant ID context
                                role = authenticatedRole, // Staff profile lock boundary
                                phone = "",                      // Leave blank or pass profile mobile string if available
                                name = state.staffName,          // Cache staff display name string
                                staffId = state.staffId
                            )

                            // 3. Cache your remaining multi-tenant worker session variables
                            sessionManager.saveStaffId(state.staffId)
                            sessionManager.savePermissions(state.permissions)

                            Toast.makeText(this@StaffLoginActivity, "Welcome, ${state.staffName}!", Toast.LENGTH_SHORT).show()

                            // 4. DYNAMIC ACTIVITY REDIRECTION ROUTER MATRIX
                            // Compares lowercase trimmed role variants to match values like "Waiter", "Kitchen", etc.
                            val targetActivityClass = when(authenticatedRole.lowercase().trim()) {
                                "kitchen", "chef" -> KitchenHomeActivity::class.java
                                "billing", "cashier" -> BillingHomeActivity::class.java
                                else -> StaffHomeActivity::class.java
                            }

                            Log.d(TAG, "Success Auth Routing: Forwarding profile session to ${targetActivityClass.simpleName}")

                            // Route cleanly to the functional shift tracking hub dashboard activity space
                            val intent = Intent(this@StaffLoginActivity, targetActivityClass).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
        }
    }
}
