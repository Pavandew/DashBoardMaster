package com.example.masterdashboard.login.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.databinding.ActivityOtpverificationBinding
import com.example.masterdashboard.login.uistate.OtpUiState
import com.example.masterdashboard.login.viewmodel.OtpViewModel
import com.example.masterdashboard.manager_single_res_dash.home.ManagerHomeActivity
import com.example.masterdashboard.master_dash.home.MasterHomeActivity
import com.example.masterdashboard.staff_dash.home.StaffHomeActivity
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
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

        binding =
            ActivityOtpverificationBinding.inflate(layoutInflater)

        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Get Intent Data
        phone =
            intent.getStringExtra("phone") ?: ""

        fullName =
            intent.getStringExtra("fullName") ?: ""

        password =
            intent.getStringExtra("password") ?: ""

        role =
            intent.getStringExtra("role") ?: ""

        portalType =
            intent.getStringExtra("portalType") ?: ""

        Log.d(TAG, "onCreate: Received data from intent: phone=$phone, fullName=$fullName, role=$role, portalType=$portalType")

        binding.tvPhone.text =
            "OTP sent to +91 $phone"

        // Send OTP
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

        binding.btnVerify.setOnClickListener {

            val otp =
                binding.etOtp.text.toString().trim()

            if (otp.isEmpty()) {

                Toast.makeText(
                    this,
                    "Enter OTP",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            viewModel.verifyOtp(otp)
        }
    }

    private fun observeState() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.otpState.collect { state ->

                    when (state) {

                        is OtpUiState.Idle -> {}

                        is OtpUiState.Loading -> {

                            binding.btnVerify.isEnabled = false

                            binding.btnVerify.text =
                                "Verifying..."
                        }

                        is OtpUiState.CodeSent -> {

                            binding.btnVerify.isEnabled = true

                            binding.btnVerify.text =
                                "Verify OTP"
                        }

                        is OtpUiState.Verified -> {

                            Toast.makeText(
                                this@OTPVerificationActivity,
                                "Verified Successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Save Login Session
                            sessionManager.setLogin(
                                uid = "",
                                role = role,
                                phone = phone
                            )

                            // Navigate Dashboard
                            navigateToDashboard(role)
                        }

                        is OtpUiState.Error -> {

                            binding.btnVerify.isEnabled = true

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
                    ManagerHomeActivity::class.java
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
                    StaffHomeActivity::class.java
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
}