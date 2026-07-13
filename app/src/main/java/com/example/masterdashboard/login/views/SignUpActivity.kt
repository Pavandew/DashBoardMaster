package com.example.masterdashboard.login.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.databinding.ActivitySignUpBinding
import com.example.masterdashboard.login.uistate.SignUpUiState
import com.example.masterdashboard.login.viewmodel.SignUpViewModel
import com.example.masterdashboard.login.utils.AppConstants
import com.example.masterdashboard.login.utils.SessionManager
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SignUpActivity"
    }

    private lateinit var binding: ActivitySignUpBinding

    private val viewModel: SignUpViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        clickListeners()
        observeState()
    }

    private fun clickListeners() {

        binding.loginItem.signupLoginTv.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }

        binding.loginItem.signupCreateAccBtn.setOnClickListener {

            Log.d(TAG, "Create Account button clicked")
            clearErrors()

            if (!binding.loginItem.signupCb.isChecked) {
                Log.d(TAG, "Terms and conditions not checked")
                Toast.makeText(
                    this,
                    "Accept terms and conditions",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // selectedPortal
            val selectedPortal =
                sessionManager.getSelectedPortal()
            Log.d(TAG, "Selected portal from session: $selectedPortal")

            // Role Based on Portal
            val role =
                when(selectedPortal) {

                    AppConstants.PORTAL_MULTI_RESTAURANT -> {
                        AppConstants.ROLE_OWNER_MULTI
                    }

                    AppConstants.PORTAL_RESTAURANT -> {
                        AppConstants.ROLE_OWNER_SINGLE
                    }
                     else -> {
                         ""
                     }
                }
            Log.d(TAG, "Determined role: $role")

            // Block Invalid Signup
            if(role.isEmpty()) {
                Log.w(TAG, "Signup blocked: role is empty for portal $selectedPortal")
                Toast.makeText(
                    this,
                    "Invalid portal selection. Please go back and select a portal.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            Log.i(TAG, "Calling viewModel.signUpUser with role=$role, portal=$selectedPortal")
            viewModel.signUpUser(
                fullName =
                    binding.loginItem.signupNameEt.text.toString().trim(),
                phone =
                    binding.loginItem.signupPhoneEt.text.toString().trim(),
                password =
                    binding.loginItem.signupPasswordEt.text.toString().trim(),
                confirmPassword =
                    binding.loginItem.signupRePassEt.text.toString().trim(),
                role = role,
                portalType = selectedPortal
            )
        }
    }

    private fun observeState() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.signUpState.collect { state ->

                    when (state) {

                        is SignUpUiState.Idle -> {
                            resetButton()
                        }

                        is SignUpUiState.Loading -> {

                            binding.loginItem.signupCreateAccBtn.isEnabled = false
                            binding.loginItem.signupCreateAccBtn.text = "Creating..."
                        }

                        is SignUpUiState.Success -> {

                            resetButton()

                            Toast.makeText(
                                this@SignUpActivity,
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()

                            // IMPORTANT CHANGE (GO TO OTP SCREEN)
                            val intent = Intent(
                                this@SignUpActivity,
                                OTPVerificationActivity::class.java
                            )

                            Log.d(TAG, "Navigating to OTPVerificationActivity with data: phone=${state.phone}, role=${state.role}, portalType=${state.portalType}")

                            intent.putExtra("phone", state.phone)
                            intent.putExtra(
                                "fullName",
                                binding.loginItem.signupNameEt.text.toString()
                            )
                            intent.putExtra("password",
                                binding.loginItem.signupPasswordEt.text.toString()
                            )
                            intent.putExtra("role", state.role)

                            intent.putExtra(
                                "portalType",
                                state.portalType
                            )

                            startActivity(intent)
                            finish()

                        }

                        is SignUpUiState.Error -> {

                            resetButton()

                            clearErrors()

                            binding.root.post {

                                when (state.field) {

                                    "fullName" ->
                                        binding.loginItem.signupNameTil.error =
                                            state.message

                                    "phone" ->
                                        binding.loginItem.signupPhoneTil.error =
                                            state.message

                                    "password" ->
                                        binding.loginItem.signupPasswordTil.error =
                                            state.message

                                    "confirmPassword" ->
                                        binding.loginItem.signupRePasswordTil.error =
                                            state.message

                                    else ->
                                        Toast.makeText(
                                            this@SignUpActivity,
                                            state.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                }

                                viewModel.resetState()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun clearErrors() {

        binding.loginItem.signupNameTil.error = null
        binding.loginItem.signupPhoneTil.error = null
        binding.loginItem.signupPasswordTil.error = null
        binding.loginItem.signupRePasswordTil.error = null
    }

    private fun resetButton() {

        binding.loginItem.signupCreateAccBtn.isEnabled = true
        binding.loginItem.signupCreateAccBtn.text = "Create Account"
    }
}