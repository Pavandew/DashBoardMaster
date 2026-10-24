package com.restroone.app.login.views

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.restroone.app.R
import com.restroone.app.databinding.ActivitySignUpBinding
import com.restroone.app.login.uistate.SignUpUiState
import com.restroone.app.login.viewmodel.SignUpViewModel
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.NetworkUtils
import com.restroone.app.utils.SessionManager
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

        setupPortalTheme()
        clickListeners()
        observeState()
    }

    private fun setupPortalTheme() {
        val (label, colorRes) = when (sessionManager.getSelectedPortal()) {
            AppConstants.PORTAL_MULTI_RESTAURANT ->
                "Multi-Restaurant Portal" to R.color.blue_gradient_start
            else ->
                "Restaurant Portal" to R.color.portal_restaurant_primary
        }
        val accentColor = ContextCompat.getColor(this, colorRes)
        val accentTint = ColorStateList.valueOf(accentColor)
        binding.loginItem.signupPortalBadge.apply {
            text = label
            backgroundTintList = accentTint
            visibility = View.VISIBLE
        }
        binding.loginItem.signupCreateAccBtn.backgroundTintList = accentTint
        binding.loginItem.signupLoginTv.setTextColor(accentTint)
        binding.loginItem.signupTermCond.setTextColor(accentTint)
        binding.loginItem.signupCb.buttonTintList = accentTint
        listOf(
            binding.loginItem.signupNameTil,
            binding.loginItem.signupPhoneTil,
            binding.loginItem.signupPasswordTil,
            binding.loginItem.signupRePasswordTil
        ).forEach { layout ->
            layout.boxStrokeColor = accentColor
            layout.setStartIconTintList(accentTint)
            layout.setEndIconTintList(accentTint)
        }
    }

    private fun clickListeners() {

        binding.loginItem.signupLoginTv.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }

        binding.loginItem.signupCreateAccBtn.setOnClickListener {

            Log.d(TAG, "Create Account button clicked")
            clearErrors()

            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!binding.loginItem.signupCb.isChecked) {
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

                            setInputsEnabled(false)
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

                            intent.putExtra(AppConstants.KEY_MOBILE, state.phone)
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

                                    AppConstants.FIELD_FULL_NAME ->
                                        binding.loginItem.signupNameTil.error =
                                            state.message

                                    AppConstants.FIELD_MOBILE ->
                                        binding.loginItem.signupPhoneTil.error =
                                            state.message

                                    AppConstants.FIELD_PASSWORD ->
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

    private fun setInputsEnabled(enabled: Boolean) {
        binding.loginItem.signupCreateAccBtn.isEnabled = enabled
        binding.loginItem.signupNameEt.isEnabled = enabled
        binding.loginItem.signupPhoneEt.isEnabled = enabled
        binding.loginItem.signupPasswordEt.isEnabled = enabled
        binding.loginItem.signupRePassEt.isEnabled = enabled
        binding.loginItem.signupCb.isEnabled = enabled
        binding.loginItem.signupTermCond.isEnabled = enabled
        binding.loginItem.signupLoginTv.isEnabled = enabled
    }

    private fun resetButton() {

        setInputsEnabled(true)
        binding.loginItem.signupCreateAccBtn.text = "Create Account"
    }
}