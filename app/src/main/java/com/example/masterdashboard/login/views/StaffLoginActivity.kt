package com.example.masterdashboard.login.views

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
import com.example.masterdashboard.databinding.ActivityStaffLoginBinding
import com.example.masterdashboard.databinding.DialogForgotPasswordStaffBinding
import com.example.masterdashboard.login.uistate.StaffLoginUiState
import com.example.masterdashboard.login.viewmodel.StaffLoginViewModel
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.kitchen_screens.KitchenHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
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
                                uid = state.restaurantOwnerUid,
                                role = authenticatedRole,
                                mobile = state.mobile,
                                name = state.staffName,
                                staffId = state.staffId,
                                staffDocId = state.staffDocId
                            )

                            sessionManager.saveStaffId(state.staffId)
                            sessionManager.saveStaffDocId(state.staffDocId)
                            sessionManager.savePermissions(state.permissions)

                            saveFcmToken(state.restaurantOwnerUid, state.staffDocId)

                            Toast.makeText(this@StaffLoginActivity, "Welcome, ${state.staffName}!", Toast.LENGTH_SHORT).show()

                            val targetActivityClass = when(authenticatedRole.lowercase().trim()) {
                                "kitchen", "chef" -> KitchenHomeActivity::class.java
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
            role = state.role
        )
        
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun saveFcmToken(ownerUid: String, staffDocId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener

            val token = task.result ?: return@addOnCompleteListener
            val db = FirebaseFirestore.getInstance()
            db.collection(AppConstants.COLLECTION_USERS)
                .document(ownerUid)
                .collection(AppConstants.COLLECTION_STAFF)
                .document(staffDocId)
                .update(AppConstants.FIELD_FCM_TOKEN, token)
        }
    }
}
