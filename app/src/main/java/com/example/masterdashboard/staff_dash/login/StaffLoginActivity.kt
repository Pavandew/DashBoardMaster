package com.example.masterdashboard.staff_dash.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.masterdashboard.databinding.ActivityStaffLoginBinding
import com.example.masterdashboard.staff_dash.home.StaffHomeActivity

class StaffLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffLoginBinding
    private val TAG = "StaffLoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Staff Login Activity Initialized")
        
        // 1. Setup Edge-to-Edge and Inflate Binding
        enableEdgeToEdge()
        binding = ActivityStaffLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.apply {
            // Login Button Click
            staffLoginBtn.setOnClickListener {
                val id = staffEtInput.text.toString().trim()
                val password = staffPasswordEt.text.toString().trim()

                Log.d(TAG, "Login button clicked with ID: $id")

                if (id.isEmpty()) {
                    textInputLayout.error = "Enter ID Number"
                    Log.w(TAG, "Validation Failed: ID Number is empty")
                    return@setOnClickListener
                } else {
                    textInputLayout.error = null
                }

                if (password.isEmpty()) {
                    textInputLayout2.error = "Enter Password"
                    Log.w(TAG, "Validation Failed: Password is empty")
                    return@setOnClickListener
                } else {
                    textInputLayout2.error = null
                }

                // Handle Login Logic
                Log.i(TAG, "Validation Successful: Initiating login for ID $id")
                Toast.makeText(this@StaffLoginActivity, "Logging in...", Toast.LENGTH_SHORT).show()

                startActivity(Intent(this@StaffLoginActivity, StaffHomeActivity::class.java))
            }

            // Forgot Password Click
            staffForgotPass.setOnClickListener {
                Log.d(TAG, "Forgot password link clicked")
                Toast.makeText(this@StaffLoginActivity, "Forgot Password Clicked", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
