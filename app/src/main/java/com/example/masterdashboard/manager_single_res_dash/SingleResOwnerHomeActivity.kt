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
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.RegistrationDataViewModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.views.FormStep1Fragment
import com.example.masterdashboard.manager_single_res_dash.form_screen.views.FormStep2Fragment
import com.example.masterdashboard.manager_single_res_dash.form_screen.views.FormStep3Fragment
import com.example.masterdashboard.manager_single_res_dash.form_screen.views.FormStep4Fragment
import com.example.masterdashboard.manager_single_res_dash.form_screen.views.FormStep5Fragment
import com.example.masterdashboard.manager_single_res_dash.form_screen.views.FormStep6Fragment
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

        val isEditMode = intent.getBooleanExtra(AppConstants.EXTRA_EDIT_MODE, false)
        val startStep = intent.getIntExtra(AppConstants.EXTRA_START_STEP, 1)

        // 1. FAST ROUTING: Check setup status before inflating any UI
        // Bypass if we are explicitly coming here to EDIT
        if (sessionManager.isRestaurantSetup() && !isEditMode) {
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

        if (isEditMode) {
            dataViewModel.setEditMode(true)
        }

        // RESTORE DRAFT IF EXISTS
        sessionManager.getRegistrationDraft()?.let { draft ->
            dataViewModel.restoreFromDraft(draft)
        }

        ViewCompat.setOnApplyWindowInsetsListener(activityBinding.mainContent) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadRestaurantDetailsForm(startStep)
    }

    private fun loadRestaurantDetailsForm(startStep: Int = 1) {
        val fragment = when (startStep) {
            1 -> FormStep1Fragment()
            2 -> FormStep2Fragment()
            3 -> FormStep3Fragment()
            4 -> FormStep4Fragment()
            5 -> FormStep5Fragment()
            6 -> FormStep6Fragment()
            else -> FormStep1Fragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.single_owner_fragmentContainer, fragment)
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
