package com.restroone.app.login.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.widget.doOnTextChanged
import com.restroone.app.R
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.appcompat.app.AlertDialog
import com.restroone.app.databinding.ActivityLoginBinding
import com.restroone.app.databinding.DialogForgotPasswordCustomBinding
import com.restroone.app.master_dash.MasterHomeActivity
import com.restroone.app.login.uistate.LoginUiState
import com.restroone.app.login.viewmodel.LoginViewModel
import com.restroone.app.manager_single_res_dash.ManagerHomeActivity
import com.restroone.app.manager_single_res_dash.SingleResOwnerHomeActivity
import com.restroone.app.staff_dash.waiter_screens.WaiterHomeActivity
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.restroone.app.utils.NetworkUtils
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

        setupPortalTheme()
        clickListeners()
        observeState()
    }

    private fun setupPortalTheme() {
        val selectedPortal = sessionManager.getSelectedPortal()
        val portalBadge = binding.loginItem.tvPortalBadge
        portalBadge.visibility = View.VISIBLE

        // Future-proof country prefix handling (default: "+91 ")
        val countryPrefix = "+91 "
        binding.loginItem.loginPhoneLayout.prefixText = countryPrefix
        val darkTextColor = androidx.core.content.ContextCompat.getColor(this, R.color.black)
        binding.loginItem.loginPhoneLayout.setPrefixTextColor(android.content.res.ColorStateList.valueOf(darkTextColor))

        when (selectedPortal) {
            AppConstants.PORTAL_MULTI_RESTAURANT -> {
                portalBadge.text = "Multi-Restaurant Portal"
                val blueColor = androidx.core.content.ContextCompat.getColor(this, R.color.blue_gradient_start)
                portalBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(blueColor)
                binding.loginItem.loginBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(blueColor)
                binding.loginItem.loginTvForgotPassword.setTextColor(blueColor)
                binding.loginItem.loginCreateAccTv.setTextColor(blueColor)
                binding.loginItem.loginPhoneLayout.boxStrokeColor = blueColor
                binding.loginItem.loginPasswordLayout.boxStrokeColor = blueColor
                val accentTint = android.content.res.ColorStateList.valueOf(blueColor)
                binding.loginItem.loginPhoneLayout.setStartIconTintList(accentTint)
                binding.loginItem.loginPasswordLayout.setStartIconTintList(accentTint)
                binding.loginItem.loginPasswordLayout.setEndIconTintList(accentTint)
            }
            AppConstants.PORTAL_STAFF -> {
                portalBadge.text = "Staff Portal"
                val orangeColor = androidx.core.content.ContextCompat.getColor(this, R.color.orange_gradient_start)
                portalBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(orangeColor)
                binding.loginItem.loginBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(orangeColor)
                binding.loginItem.loginTvForgotPassword.setTextColor(orangeColor)
                binding.loginItem.loginPhoneLayout.boxStrokeColor = orangeColor
                binding.loginItem.loginPasswordLayout.boxStrokeColor = orangeColor
                val accentTint = android.content.res.ColorStateList.valueOf(orangeColor)
                binding.loginItem.loginPhoneLayout.setStartIconTintList(accentTint)
                binding.loginItem.loginPasswordLayout.setStartIconTintList(accentTint)
                binding.loginItem.loginPasswordLayout.setEndIconTintList(accentTint)

                binding.loginItem.loginCreateAccTv.visibility = View.GONE
                binding.loginItem.loginDontHaveCreateAccTv.visibility = View.GONE
            }
            else -> {
                portalBadge.text = "Single Restaurant Portal"
                val purpleColor = androidx.core.content.ContextCompat.getColor(this, R.color.portal_restaurant_primary)
                portalBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(purpleColor)
                binding.loginItem.loginBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(purpleColor)
                binding.loginItem.loginTvForgotPassword.setTextColor(purpleColor)
                binding.loginItem.loginCreateAccTv.setTextColor(purpleColor)
                binding.loginItem.loginPhoneLayout.boxStrokeColor = purpleColor
                binding.loginItem.loginPasswordLayout.boxStrokeColor = purpleColor
                val accentTint = android.content.res.ColorStateList.valueOf(purpleColor)
                binding.loginItem.loginPhoneLayout.setStartIconTintList(accentTint)
                binding.loginItem.loginPasswordLayout.setStartIconTintList(accentTint)
                binding.loginItem.loginPasswordLayout.setEndIconTintList(accentTint)
            }
        }
    }

    private fun clickListeners() {
        binding.loginItem.loginPhoneEt.doOnTextChanged { _, _, _, _ ->
            binding.loginItem.loginPhoneLayout.error = null
        }

        binding.loginItem.loginPasswordEt.doOnTextChanged { _, _, _, _ ->
            binding.loginItem.loginPasswordLayout.error = null
        }

        binding.loginItem.loginCreateAccTv.setOnClickListener {
            val selectedPortal = sessionManager.getSelectedPortal()
            Log.d(TAG, "Navigating to SignUpActivity. Current portal in session: $selectedPortal")
            val intent = Intent(this, SignUpActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }

        binding.loginItem.loginBtn.setOnClickListener {
            binding.loginItem.loginPhoneLayout.error = null
            binding.loginItem.loginPasswordLayout.error = null

            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val phone =
                binding.loginItem.loginPhoneEt.text.toString().trim()

            val password =
                binding.loginItem.loginPasswordEt.text.toString().trim()

            viewModel.loginUser(phone, password)
        }

        binding.loginItem.loginTvForgotPassword.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val typedPhone = binding.loginItem.loginPhoneEt.text.toString().trim()
            val cleanDigits = typedPhone.replace("[^0-9]".toRegex(), "")
            if (cleanDigits.length == 10) {
                val formattedPhone = "+91$cleanDigits"
                Log.i(TAG, "Directly opening Reset Password for valid typed phone: $formattedPhone")
                openResetPasswordFragment(phone = formattedPhone)
            } else {
                showForgotPasswordDialog(prefilledPhone = typedPhone)
            }
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
                                binding.loginItem.loginPhoneLayout.error = "Access denied for this portal"

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
                                mobile = phone,
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

                        is LoginUiState.ResetRequested -> {
                            binding.loginItem.loginBtn.isEnabled = true
                            binding.loginItem.loginBtn.text = "Login"

                            viewModel.reset()

                            openResetPasswordFragment(phone = state.phone)
                        }

                        is LoginUiState.Error -> {
                            Log.e(TAG, "Login Error: ${state.message}")

                            binding.loginItem.loginBtn.isEnabled = true
                            binding.loginItem.loginBtn.text = "Login"

                            binding.loginItem.loginPhoneLayout.error = null
                            binding.loginItem.loginPasswordLayout.error = null

                            when (state.field) {
                                "BOTH" -> {
                                    binding.loginItem.loginPhoneLayout.error = "Phone number required"
                                    binding.loginItem.loginPasswordLayout.error = "Password required"
                                }
                                "PHONE" -> {
                                    binding.loginItem.loginPhoneLayout.error = state.message
                                }
                                "PASSWORD" -> {
                                    binding.loginItem.loginPasswordLayout.error = state.message
                                }
                                else -> {
                                    if (state.message.contains("phone", ignoreCase = true) || state.message.contains("user", ignoreCase = true) || state.message.contains("mobile", ignoreCase = true)) {
                                        binding.loginItem.loginPhoneLayout.error = state.message
                                    } else if (state.message.contains("password", ignoreCase = true)) {
                                        binding.loginItem.loginPasswordLayout.error = state.message
                                    } else {
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

            }
        }
    }

    private fun showForgotPasswordDialog(prefilledPhone: String = "") {
        val dialogBinding = DialogForgotPasswordCustomBinding.inflate(layoutInflater)

        if (prefilledPhone.isNotEmpty()) {
            val cleanDigits = prefilledPhone.replace("[^0-9]".toRegex(), "")
            val tenDigits = if (cleanDigits.length > 10) cleanDigits.takeLast(10) else cleanDigits
            dialogBinding.dialogPhoneEt.setText(tenDigits)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.dialogBtnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.dialogBtnVerify.setOnClickListener {
            val rawInput = dialogBinding.dialogPhoneEt.text?.toString()?.trim().orEmpty()
            val cleanDigits = rawInput.replace("[^0-9]".toRegex(), "")

            if (cleanDigits.length != 10) {
                dialogBinding.dialogPhoneTil.error = "Please enter a valid 10-digit mobile number"
                return@setOnClickListener
            }

            dialogBinding.dialogPhoneTil.error = null
            dialog.dismiss()

            val formattedPhone = "+91$cleanDigits"
            Log.i(TAG, "Opening Reset Password directly for phone: $formattedPhone")
            openResetPasswordFragment(phone = formattedPhone)
        }

        dialog.show()
    }

    private fun openResetPasswordFragment(phone: String) {
        val fragment = ChangePasswordFragment.newInstance(
            phone = phone,
            ownerUid = "",
            staffDocId = "",
            role = "",
            isForgotPassword = true
        )

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
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
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result ?: return@addOnCompleteListener
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
        } catch (e: Exception) {
            Log.e(TAG, "Safe FCM token handling encountered exception", e)
        }
    }

}
