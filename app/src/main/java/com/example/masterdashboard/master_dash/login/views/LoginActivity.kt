package com.example.masterdashboard.master_dash.login.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.databinding.ActivityLoginBinding
import com.example.masterdashboard.master_dash.home.MasterHomeActivity
import com.example.masterdashboard.master_dash.login.uistate.LoginUiState
import com.example.masterdashboard.master_dash.login.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private val TAG = "LoginActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Login Activity started")

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clickListeners()
        observeState()

    }

    private fun clickListeners() {
        binding.loginItem.loginCreateAccTv.setOnClickListener {
            Log.d(TAG, "Navigating to SignUpActivity")
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.loginItem.loginBtn.setOnClickListener {

            val phone =
                binding.loginItem.loginEmailEt.text.toString().trim()

            val password =
                binding.loginItem.loginPasswordEt.text.toString().trim()

            Log.i(TAG, "Attempting login for phone: $phone")
            viewModel.loginUser(phone, password)
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

                            // Save login state
                            val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            sharedPref.edit().putBoolean("isLoggedIn", true).apply()

                            Toast.makeText(
                                this@LoginActivity,
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    MasterHomeActivity::class.java
                                )
                            )
                            finish()
                        }
                        is LoginUiState.Error -> {
                            Log.e(TAG, "Login Error: ${state.message}")

                            binding.loginItem.loginBtn.isEnabled = true
                            binding.loginItem.loginBtn.text = "Login"

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
