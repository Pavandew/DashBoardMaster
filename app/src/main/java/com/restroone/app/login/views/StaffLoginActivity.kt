package com.restroone.app.login.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.restroone.app.databinding.ActivityStaffLoginBinding
import com.restroone.app.databinding.DialogForgotPasswordStaffBinding
import com.restroone.app.login.uistate.StaffLoginUiState
import com.restroone.app.login.viewmodel.StaffLoginViewModel
import com.restroone.app.staff_dash.billing_screens.CashierHomeActivity
import com.restroone.app.staff_dash.kitchen_screens.KitchenHomeActivity
import com.restroone.app.staff_dash.waiter_screens.WaiterHomeActivity
import com.restroone.app.utils.SessionManager
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.restroone.app.utils.NetworkUtils
import kotlinx.coroutines.launch

class StaffLoginActivity : AppCompatActivity() {
    companion object{
        private const val TAG = "StaffLoginActivity"
    }

    private lateinit var binding: ActivityStaffLoginBinding
    private val viewModel: StaffLoginViewModel by viewModels()
    private val sessionManager by lazy { SessionManager(this) }
    private var isForgotFlow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Staff Login Activity Initialized")

        enableEdgeToEdge()
        binding = ActivityStaffLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeLoginUiPipeline()
    }

    private fun setupClickListeners() {
        binding.staffLoginBtn.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val enteredId = binding.staffEtInput.text.toString().trim()
            val enteredPass = binding.staffPasswordEt.text.toString().trim()

            binding.textInputLayout.error = null
            binding.textInputLayout2.error = null

            isForgotFlow = false
            viewModel.processStaffLogin(enteredId, enteredPass)
        }

        binding.staffForgotPass.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogBinding = DialogForgotPasswordStaffBinding.inflate(layoutInflater)

        AlertDialog.Builder(this)
            .setTitle("Forgot Password")
            .setMessage("Enter your registered mobile number to reset your password.")
            .setView(dialogBinding.root)
            .setPositiveButton("Verify Mobile") { _, _ ->
                val mobileInput = dialogBinding.etStaffId.text.toString().trim()
                if (mobileInput.isNotEmpty()) {
                    isForgotFlow = true
                    viewModel.findStaffByPhoneForReset(mobileInput)
                } else {
                    Toast.makeText(this, "Please enter mobile number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

                        is StaffLoginUiState.Success -> {
                            if (isForgotFlow) {
                                isForgotFlow = false
                                openResetPasswordFragment(state)
                                return@collect
                            }
                            
                            Log.i(TAG, "Navigation Verified: Appending sub-collection parameters to local Session Cache.")

                            val authenticatedRole = state.role
                            sessionManager.setLogin(
                                uid = state.restaurantId,
                                role = authenticatedRole,
                                mobile = state.mobile,
                                name = state.staffName,
                                staffId = state.staffId,
                                staffDocId = state.staffDocId
                            )
                            sessionManager.saveRestaurantId(state.restaurantId)

                            sessionManager.savePermissions(state.permissions)

                            saveFcmToken(state.staffDocId)

                            Toast.makeText(this@StaffLoginActivity, "Welcome, ${state.staffName}!", Toast.LENGTH_SHORT).show()

                            val targetActivityClass = when(authenticatedRole.lowercase().trim()) {
                                "kitchen", "chef", "master chef", "chef_staff" -> KitchenHomeActivity::class.java
                                "billing", "cashier" -> CashierHomeActivity::class.java
                                else -> WaiterHomeActivity::class.java
                            }

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

    private fun openResetPasswordFragment(state: StaffLoginUiState.Success) {
        val fragment = ChangePasswordFragment.newInstance(
            phone = state.mobile,
            ownerUid = state.restaurantOwnerUid,
            staffDocId = state.staffDocId,
            role = state.role,
            isForgotPassword = true
        )
        
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun saveFcmToken(staffDocId: String) {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener

                val token = task.result ?: return@addOnCompleteListener
                FirebaseFunctions.getInstance()
                    .getHttpsCallable("updateStaffFcmToken")
                    .call(mapOf("fcmToken" to token))
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Unable to update staff FCM token for $staffDocId", error)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Safe staff FCM token handling exception", e)
        }
    }
}
