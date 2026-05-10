package com.example.masterdashboard.login.views

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.databinding.ActivitySignUpBinding
import com.example.masterdashboard.login.uistate.SignUpUiState
import com.example.masterdashboard.login.viewmodel.SignUpViewModel
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

            clearErrors()

            if (!binding.loginItem.signupCb.isChecked) {

                Toast.makeText(
                    this,
                    "Accept terms and conditions",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            viewModel.signUpUser(
                fullName = binding.loginItem.signupNameEt.text.toString().trim(),
                phone = binding.loginItem.signupPhoneEt.text.toString().trim(),
                password = binding.loginItem.signupPasswordEt.text.toString().trim(),
                confirmPassword = binding.loginItem.signupRePassEt.text.toString().trim()
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

                            intent.putExtra("phone", state.phone)
                            intent.putExtra("fullName",
                                binding.loginItem.signupNameEt.text.toString()
                            )
                            intent.putExtra("password",
                                binding.loginItem.signupPasswordEt.text.toString()
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