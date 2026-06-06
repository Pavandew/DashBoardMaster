package com.example.masterdashboard.manager_single_res_dash.home

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityManagerHomeBinding
import com.example.masterdashboard.manager_single_res_dash.home.views.ManagerDashboardFragment

class ManagerHomeActivity : AppCompatActivity() {

    // Clean standard backing layout variable setup for safe View Binding access
    private var _binding: ActivityManagerHomeBinding? = null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _binding = ActivityManagerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContent) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // CRITICAL FIX: Instantiates and displays the dashboard fragment on initial clean app launch
        if (supportFragmentManager.findFragmentById(R.id.manager_fragmentContainer) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.manager_fragmentContainer, ManagerDashboardFragment())
                .commit()
        }
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