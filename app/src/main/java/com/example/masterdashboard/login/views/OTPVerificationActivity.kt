package com.example.masterdashboard.login.views

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.databinding.ActivityOtpverificationBinding
import com.example.masterdashboard.home.HomeActivity
import com.example.masterdashboard.login.uistate.OtpUiState
import com.example.masterdashboard.login.viewmodel.OtpViewModel
import com.example.masterdashboard.login.views.SignUpActivity
import kotlinx.coroutines.launch

class OTPVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpverificationBinding

    private val viewModel: OtpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOtpverificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val phone = intent.getStringExtra("phone") ?: ""
        val fullName = intent.getStringExtra("fullName") ?: ""
        val password = intent.getStringExtra("password") ?: ""

        binding.tvPhone.text = "OTP sent to +91 $phone"

        // Send OTP
        viewModel.sendOtp(phone, this, fullName, password)

        setupBackPress()
        setupClick()
        observeState()
    }

    // Modern back press handling
    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this) {

            val intent = Intent(
                this@OTPVerificationActivity,
                SignUpActivity::class.java
            )

            startActivity(intent)
            finish()
        }
    }


    private fun setupClick() {

        binding.btnVerify.setOnClickListener {

            val otp = binding.etOtp.text.toString().trim()

            if (otp.isEmpty()) {
                Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show()
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
                            binding.btnVerify.text = "Verifying..."
                        }

                        is OtpUiState.CodeSent -> {
                            binding.btnVerify.isEnabled = true
                            binding.btnVerify.text = "Verify OTP"
                        }

                        is OtpUiState.Verified -> {

                            Toast.makeText(
                                this@OTPVerificationActivity,
                                "Verified Successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Save login state
                            val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            sharedPref.edit().putBoolean("isLoggedIn", true).apply()

                            startActivity(
                                Intent(
                                    this@OTPVerificationActivity,
                                    HomeActivity::class.java
                                )
                            )

                            finish()
                        }

                        is OtpUiState.Error -> {

                            binding.btnVerify.isEnabled = true
                            binding.btnVerify.text = "Verify OTP"

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
}