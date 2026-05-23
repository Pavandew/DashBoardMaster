package com.example.masterdashboard

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.masterdashboard.databinding.ActivitySplashBinding
import com.example.masterdashboard.master_dash.home.MasterHomeActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

            val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
            val user = FirebaseAuth.getInstance().currentUser

            if (user != null || isLoggedIn) {

                startActivity(
                    Intent(
                        this,
                        MasterHomeActivity::class.java
                    )
                )

            } else {

                startActivity(
                    Intent(
                        this,
                        ActivityVisitorPortal::class.java
                    )
                )
            }

            finish()

        }, 1500)
    }
}