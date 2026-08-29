package com.example.masterdashboard.manager_single_res_dash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityManagerHomeBinding
import com.example.masterdashboard.manager_single_res_dash.views.ManagerDashboardFragment
import com.example.masterdashboard.notifications.NotificationPermissionHelper

class ManagerHomeActivity : AppCompatActivity() {

    private lateinit var permissionHelper: NotificationPermissionHelper

    // Clean standard backing layout variable setup for safe View Binding access
    private var _binding: ActivityManagerHomeBinding? = null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ManagerHomeActivity", "onCreate: Instance created")
        enableEdgeToEdge()

        _binding = ActivityManagerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Ensure background is solid from the start to prevent flickering
        binding.root.setBackgroundResource(R.color.bg_main)

        permissionHelper = NotificationPermissionHelper(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContent) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // CRITICAL FIX: Instantiates and displays the dashboard fragment only on fresh launch
        if (savedInstanceState == null && supportFragmentManager.findFragmentById(R.id.manager_fragmentContainer) == null) {
            Log.d("ManagerHomeActivity", "onCreate: First time launch - adding dashboard")
            supportFragmentManager.beginTransaction()
                .replace(R.id.manager_fragmentContainer, ManagerDashboardFragment())
                .commit()
        }

        permissionHelper.askNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("ManagerHomeActivity", "onNewIntent: Activity brought to front")
        // No logic needed here to reset, singleTask handles bringing current state forward.
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun openNavigationDrawer() {
        if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}