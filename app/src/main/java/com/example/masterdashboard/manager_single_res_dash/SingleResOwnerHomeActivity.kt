package com.example.masterdashboard.manager_single_res_dash

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivitySingleResOwnerHomeBinding
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.RegistrationDataViewModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.views.FormStep1Fragment
import com.example.masterdashboard.notifications.NotificationPermissionHelper

class SingleResOwnerHomeActivity : AppCompatActivity() {

    private lateinit var permissionHelper: NotificationPermissionHelper
    private var _binding: ActivitySingleResOwnerHomeBinding? = null
    val activityBinding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private val dataViewModel: RegistrationDataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sessionManager = SessionManager(this)

        // 1. FAST ROUTING: Check setup status before inflating any UI
        if (sessionManager.isRestaurantSetup()) {
            val intent = Intent(this, ManagerHomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()

        _binding = ActivitySingleResOwnerHomeBinding.inflate(layoutInflater)
        setContentView(activityBinding.root)

        permissionHelper = NotificationPermissionHelper(this)

        // RESTORE DRAFT IF EXISTS
        sessionManager.getRegistrationDraft()?.let { draft ->
            dataViewModel.restoreFromDraft(draft)
        }

        ViewCompat.setOnApplyWindowInsetsListener(activityBinding.mainContent) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadRestaurantDetailsForm()
    }

    private fun loadRestaurantDetailsForm() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.single_owner_fragmentContainer, FormStep1Fragment())
            .commit()

        // Match activity background to form
        activityBinding.root.setBackgroundResource(R.color.bg_form_light)

        // Disable Drawer for Setup Form to keep user focused
        activityBinding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onBackPressed() {
        if (activityBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            activityBinding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun openNavigationDrawer() {
        if (!activityBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            activityBinding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}