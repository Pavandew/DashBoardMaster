package com.restroone.app.login.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.restroone.app.databinding.ActivityOtpverificationBinding
import com.restroone.app.login.uistate.OtpUiState
import com.restroone.app.login.viewmodel.OtpViewModel
import com.restroone.app.manager_single_res_dash.ManagerHomeActivity
import com.restroone.app.manager_single_res_dash.SingleResOwnerHomeActivity
import com.restroone.app.master_dash.MasterHomeActivity
import com.restroone.app.staff_dash.waiter_screens.WaiterHomeActivity
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.SessionManager
import kotlinx.coroutines.launch

class OTPVerificationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OTPVerificationActivity"
    }

    private lateinit var binding: ActivityOtpverificationBinding

    private val viewModel: OtpViewModel by viewModels()

    private lateinit var sessionManager: SessionManager

    private var phone: String = ""
    private var fullName: String = ""
    private var password: String = ""
    private var role: String = ""

    private var portalType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: OTPVerificationActivity initialized")

        binding =
            ActivityOtpverificationBinding.inflate(layoutInflater)

        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Get Intent Data
        phone =
            intent.getStringExtra(AppConstants.KEY_MOBILE) ?: ""

        fullName =
            intent.getStringExtra("fullName") ?: ""

        password =
            intent.getStringExtra("password") ?: ""

        role =
            intent.getStringExtra("role") ?: ""

        portalType =
            intent.getStringExtra("portalType") ?: ""

        Log.i(TAG, "Received Intent Data: phone=$phone, fullName=$fullName, role=$role, portalType=$portalType")

        binding.tvPhone.text =
            "OTP sent to +91 $phone"

        // Send OTP
        Log.d(TAG, "Initiating viewModel.sendOtp for phone: $phone")
        viewModel.sendOtp(
            phone = phone,
            activity = this,
            fullName = fullName,
            password = password,
            role = role,
            portalType = portalType
        )

        setupBackPress()
        setupClick()
        observeState()
    }

    // Modern Back Press
    private fun setupBackPress() {

        onBackPressedDispatcher.addCallback(this) {
            Log.d(TAG, "Back button pressed. Navigating back to SignUpActivity.")
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                currentUser.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "Unverified temporary Auth user cleaned up on back press")
                    }
                }
            }
            startActivity(
                Intent(
                    this@OTPVerificationActivity,
                    SignUpActivity::class.java
                )
            )

            finish()
        }
    }

    private fun setupClick() {

        binding.etOtp.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 6) {
                val otp = text.toString().trim()
                Log.i(TAG, "Auto-submitting 6-digit OTP code")
                viewModel.verifyOtp(otp)
            }
        }

        binding.btnVerify.setOnClickListener {

            val otp =
                binding.etOtp.text.toString().trim()

            if (otp.length != 6) {
                Log.w(TAG, "Manual verification failed: OTP length is ${otp.length}, expected 6 digits")
                Toast.makeText(
                    this,
                    "Enter valid 6-digit OTP",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            Log.i(TAG, "Manual OTP verification button clicked")
            viewModel.verifyOtp(otp)
        }

        binding.tvResendOtp.setOnClickListener {
            Log.i(TAG, "Resend OTP clicked")
            viewModel.resendOtp(this)
        }
    }

    private fun observeState() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.otpState.collect { state ->
                    Log.d(TAG, "Observed OTP UI State: ${state::class.java.simpleName}")

                    when (state) {

                        is OtpUiState.Idle -> {
                            setInputsEnabled(true)
                            binding.btnVerify.text = "Verify OTP"
                        }

                        is OtpUiState.Sending -> {
                            Log.i(TAG, "State -> Sending OTP to +91 $phone")
                            setInputsEnabled(false)
                            binding.btnVerify.text = "Sending OTP..."
                        }

                        is OtpUiState.Verifying, is OtpUiState.Loading -> {
                            Log.i(TAG, "State -> Verifying OTP code...")
                            setInputsEnabled(false)

                            binding.btnVerify.text =
                                "Verifying..."
                        }

                        is OtpUiState.CodeSent -> {
                            Log.i(TAG, "State -> OTP Code Sent successfully via SMS")
                            setInputsEnabled(true)

                            binding.btnVerify.text =
                                "Verify OTP"
                        }

                        is OtpUiState.Verified -> {
                            Log.i(TAG, "State -> OTP Verified successfully for UID: ${state.uid}")
                            setInputsEnabled(false)
                            binding.btnVerify.text = "Verified!"

                            Toast.makeText(
                                this@OTPVerificationActivity,
                                "Verified Successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Save Login Session
                            sessionManager.setLogin(
                                uid = state.uid,
                                role = role,
                                mobile = phone,
                                name = fullName
                            )

                            if (role == AppConstants.ROLE_OWNER_SINGLE || role == AppConstants.ROLE_OWNER_MULTI) {
                                sessionManager.saveRestaurantId(state.uid)
                            } else {
                                sessionManager.saveRestaurantId("")
                            }

                            // Navigate Dashboard
                            navigateToDashboard(role)
                        }

                        is OtpUiState.Error -> {
                            Log.e(TAG, "State -> OTP Error: ${state.message}")
                            setInputsEnabled(true)

                            binding.btnVerify.text =
                                "Verify OTP"

                            Toast.makeText(
                                this@OTPVerificationActivity,
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToDashboard(role: String) {
        Log.i(TAG, "Navigating to dashboard for role: $role")

        val intent = when(role) {

            AppConstants.ROLE_OWNER_MULTI -> {

                Intent(
                    this,
                    MasterHomeActivity::class.java
                )
            }

            AppConstants.ROLE_OWNER_SINGLE -> {

                Intent(
                    this,
                    SingleResOwnerHomeActivity::class.java
                )
            }

            AppConstants.ROLE_MANAGER -> {

                Intent(
                    this,
                    ManagerHomeActivity::class.java
                )
            }

            AppConstants.ROLE_STAFF -> {

                Intent(
                    this,
                    WaiterHomeActivity::class.java
                )
            }

            else -> {

                Toast.makeText(
                    this,
                    "Invalid role",
                    Toast.LENGTH_SHORT
                ).show()

                null
            }
        }
        intent?.let {
            startActivity(it)
            finish()
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.btnVerify.isEnabled = enabled
        binding.etOtp.isEnabled = enabled
        binding.tvResendOtp.isEnabled = enabled
    }
}