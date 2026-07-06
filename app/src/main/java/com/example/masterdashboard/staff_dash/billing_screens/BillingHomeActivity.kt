package com.example.masterdashboard.staff_dash.billing_screens

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityBillingHomeBinding
import com.example.masterdashboard.databinding.ActivityKitchenHomeBinding
import com.example.masterdashboard.utils.LogoutManager

class BillingHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBillingHomeBinding

    // Available instantaneously for use across your entire layout activity lifecycle
    private val logoutManager by lazy { LogoutManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBillingHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.billingLogoutBtn.setOnClickListener {
            logoutManager.showLogoutConfirmation()
        }
    }
}