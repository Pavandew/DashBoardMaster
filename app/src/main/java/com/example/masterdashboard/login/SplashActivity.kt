package com.example.masterdashboard.login

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.masterdashboard.databinding.ActivitySplashBinding
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.master_dash.MasterHomeActivity
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.kitchen_screens.KitchenHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.manager_single_res_dash.SingleResOwnerHomeActivity
import kotlin.jvm.java

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    private lateinit var binding: ActivitySplashBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🔴 ABSOLUTE FIX for Android App Restart Issue:
        // If this activity is not the root of the task, it means the app was already 
        // running and Android is incorrectly trying to start it again from Splash.
        // We finish immediately to let the existing task stack show.
        if (!isTaskRoot) {
            finish()
            return
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        startAnimation()
        checkLoginState()
    }

    private fun startAnimation() {
        binding.ivLogo.alpha = 0f
        binding.tvAppName.alpha = 0f

        ObjectAnimator.ofFloat(
            binding.ivLogo,
            View.ALPHA,
            0f,
            1f
        ).apply {
            duration = 1000
            start()
        }

        ObjectAnimator.ofFloat(
            binding.tvAppName,
            View.ALPHA,
            0f,
            1f
        ).apply {
            duration = 1200
            start()
        }
    }

    private fun checkLoginState() {
        Handler(Looper.getMainLooper()).postDelayed({

            val isLoggedIn = sessionManager.isLoggedIn()
            val role = sessionManager.getRole()

            if (isLoggedIn) {
                val userName = sessionManager.getUserName() ?: "Unknown Name"
                val userPhone = sessionManager.getPhone() ?: "Unknown Number"
                // NEW: Read the persistently preserved alphanumeric Staff ID from preferences cache
                val staffId = sessionManager.getStaffId() ?: "No Stored ID"

                // UPDATED LOG: Includes [StaffID: $staffId] directly in the trace matrix output
                Log.d(TAG, "SESSION ACTIVE: User Logged In! [Name: $userName | StaffID: $staffId | Phone: $userPhone | Role: $role]")
                navigateToDashboard(role)
            } else {
                Log.w(TAG, "NO SESSION: No user logged in. Redirecting to Visitor Portal.")
                startActivity(Intent(this, ActivityVisitorPortal::class.java))
                finish()
            }

        }, 800)
    }
    private fun navigateToDashboard(role: String) {
        // Normalizing the role string eliminates bugs caused by case mismatch (e.g., "Waiter" vs "waiter")
        val cleanRole = role.lowercase().trim()

        val nextIntent = when (cleanRole) {
            AppConstants.ROLE_OWNER_MULTI -> {
                Intent(this, MasterHomeActivity::class.java)
            }
            AppConstants.ROLE_OWNER_SINGLE -> {
                if (sessionManager.isRestaurantSetup()) {
                    Intent(this, ManagerHomeActivity::class.java)
                } else {
                    Intent(this, SingleResOwnerHomeActivity::class.java)
                }
            }
            AppConstants.ROLE_MANAGER -> {
                Intent(this, ManagerHomeActivity::class.java)
            }

            // Match the exact "Waiter" or general staff roles to your bottom nav activity
            "waiter", "staff", AppConstants.ROLE_STAFF -> {
                Log.d(TAG, "Routing to Waiter Workspace Workspace")
                Intent(this, WaiterHomeActivity::class.java)
            }

            // Match Kitchen / Chef roles directly to full screen KDS activity
            "kitchen", "chef" -> {
                Log.d(TAG, "Routing to Kitchen Workspace KDS")
                Intent(this, KitchenHomeActivity::class.java)
            }

            // Match Billing / Cashier roles directly to Checkout activity
            "billing", "cashier" -> {
                Log.d(TAG, "Routing to Billing Checkout Workspace")
                Intent(this, CashierHomeActivity::class.java)
            }

            else -> {
                Log.w(TAG, "⚠️ INVALID ROLE: Found unexpected role string: '$role'. Redirecting to portal.")
                Intent(this, ActivityVisitorPortal::class.java)
            }
        }

        // Pass any extras (like orderId from a notification) forward to the next screen
        intent.extras?.let { nextIntent.putExtras(it) }

        startActivity(nextIntent)
        finish()
    }
}