package com.example.masterdashboard

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.masterdashboard.databinding.ActivitySplashBinding
import com.example.masterdashboard.manager_single_res_dash.home.ManagerHomeActivity
import com.example.masterdashboard.master_dash.home.MasterHomeActivity
import com.example.masterdashboard.staff_dash.home.StaffHomeActivity
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    private lateinit var binding: ActivitySplashBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            Log.d(TAG, "checkLoginState: isLoggedIn=$isLoggedIn, role=$role")

            if (isLoggedIn) {
                navigateToDashboard(role)
            } else {
                Log.d(TAG, "Not logged in, navigating to Visitor Portal")
                startActivity(Intent(this, ActivityVisitorPortal::class.java))
                finish()
            }
        }, 1500)
    }

    private fun navigateToDashboard(role: String) {
        val intent = when (role) {
            AppConstants.ROLE_OWNER_MULTI -> {
                Intent(this, MasterHomeActivity::class.java)
            }
            AppConstants.ROLE_OWNER_SINGLE, AppConstants.ROLE_MANAGER -> {
                Intent(this, ManagerHomeActivity::class.java)
            }
            AppConstants.ROLE_STAFF -> {
                Intent(this, StaffHomeActivity::class.java)
            }
            else -> {
                Log.w(TAG, "Invalid role found: $role. Redirecting to portal.")
                Intent(this, ActivityVisitorPortal::class.java)
            }
        }
        startActivity(intent)
        finish()
    }
}
