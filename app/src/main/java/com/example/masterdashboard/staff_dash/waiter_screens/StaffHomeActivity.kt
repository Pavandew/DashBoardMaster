package com.example.masterdashboard.staff_dash.waiter_screens

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityStaffHomeBinding
import com.example.masterdashboard.master_dash.settings.views.ChangePasswordFragment
import com.example.masterdashboard.staff_dash.waiter_screens.alert.StaffAlertFragment
import com.example.masterdashboard.staff_dash.waiter_screens.dashboard.StaffDashboardFragment
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.StaffOrdersFragment
import com.example.masterdashboard.staff_dash.waiter_screens.profile.StaffProfileFragment
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.StaffTablesFragment
import com.example.masterdashboard.login.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class StaffHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStaffHomeBinding
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var sessionManager: SessionManager

    private var currentTag: String? = null
    private var pendingTag: String? = null
    private var isBottomNavVisible = true
    private val TAG = "StaffHomeActivity_Debug"

    companion object {
        private const val KEY_CURRENT_TAG = "current_tag"
        private const val KEY_BOTTOM_NAV_VISIBLE = "bottom_nav_visible"

        const val TAG_DASHBOARD = "dashboard"
        const val TAG_CREATE = "table"
        const val TAG_RESTAURANTS = "order"
        const val TAG_LOGS = "alert"
        const val TAG_PROFILE = "profile"
        const val TAG_CHANGE_PASSWORD = "change_password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Setup Safe edge-to-edge window configurations
        enableEdgeToEdge()

        binding = ActivityStaffHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Render transparent backgrounds onto system hardware bars
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT

        // Initialize Session Preferences
        sessionManager = SessionManager(this)

        // 3. EXPLICIT LOGCAT MONITORING LAYER
        val staffName = sessionManager.getUserName() ?: "Unknown Name"
        val staffRole = sessionManager.getRole() ?: "Unknown Role"
        val staffCustomId = sessionManager.getStaffId() ?: "No Stored ID"

        Log.i(TAG, "==========================================================")
        Log.i(TAG, "🚀 ACTIVITY INITIALIZED: StaffHomeActivity is now running.")
        Log.i(TAG, "👤 ACTIVE STAFF USER: $staffName")
        Log.i(TAG, "💼 ASSIGNED WORK ROLE: $staffRole")
        Log.i(TAG, "🆔 UNIQUE SYSTEM ID  : $staffCustomId")
        Log.i(TAG, "==========================================================")

        // Close drawer components by default
        binding.main.closeDrawer(GravityCompat.START, false)
        bottomNav = binding.staffHostBottomNav

        // 4. Set up safe view bounds paddings
        setupWindowInsets()

        setupBottomNavigation()
        setupDrawer()
        setupBackPress()

        if (savedInstanceState == null) {
            openDashboard()
        } else {
            currentTag = savedInstanceState.getString(KEY_CURRENT_TAG)
            isBottomNavVisible = savedInstanceState.getBoolean(KEY_BOTTOM_NAV_VISIBLE, true)
            navigateTo(currentTag ?: TAG_DASHBOARD)
            updateBottomNavVisibility()
        }
    }

    /**
     * Window padding layout compensations for edge-to-edge screens.
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            view.updatePadding(top = statusBars.top)
            bottomNav.updatePadding(bottom = navigationBars.bottom)

            windowInsets
        }
    }

    // Bottom Navigation
    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            Log.d(TAG, "BottomNav: Selected ${item.title}")

            val tag = when (item.itemId) {
                R.id.staff_dashboardFragment -> TAG_DASHBOARD
                R.id.staff_tableFragment -> TAG_CREATE
                R.id.staff_orderFragment -> TAG_RESTAURANTS
                R.id.staff_logFragment -> TAG_LOGS
                R.id.staff_settingFragment -> pendingTag ?: TAG_PROFILE
                else -> {
                    Log.w(TAG, "BottomNav: Unknown menu item selected")
                    return@setOnItemSelectedListener false
                }
            }

            pendingTag = null
            loadFragment(tag)
            true
        }
    }

    // Drawer Setup
    private fun setupDrawer() {
        binding.staffNavigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.profile -> openProfile()
                R.id.changePassword -> openChangePassword()
                R.id.logout -> {
                    Toast.makeText(this, "Session terminated.", Toast.LENGTH_SHORT).show()
                    sessionManager.logout() // Clears security caches entirely
                    finish()
                }
            }
            closeDrawer()
            true
        }
    }

    // Public Navigation Methods
    fun openDashboard() = navigateTo(TAG_DASHBOARD)
    fun openTables() = navigateTo(TAG_CREATE)
    fun openOrders() = navigateTo(TAG_RESTAURANTS)
    fun openAlerts() = navigateTo(TAG_LOGS)
    fun openProfile() = navigateTo(TAG_PROFILE)
    fun openChangePassword() = navigateTo(TAG_CHANGE_PASSWORD)

    // Single Navigation Entry Point
    fun navigateTo(tag: String) {
        pendingTag = tag

        when (tag) {
            TAG_DASHBOARD -> bottomNav.selectedItemId = R.id.staff_dashboardFragment
            TAG_CREATE -> bottomNav.selectedItemId = R.id.staff_tableFragment
            TAG_RESTAURANTS -> bottomNav.selectedItemId = R.id.staff_orderFragment
            TAG_LOGS -> bottomNav.selectedItemId = R.id.staff_logFragment

            TAG_PROFILE,
            TAG_CHANGE_PASSWORD -> {
                if (bottomNav.selectedItemId == R.id.staff_settingFragment) {
                    loadFragment(tag)
                } else {
                    bottomNav.selectedItemId = R.id.staff_settingFragment
                }
            }
        }

        closeDrawer()
    }

    // Main Fragment Loader
    fun loadFragment(tag: String): Boolean {
        if (currentTag == tag) return true

        supportFragmentManager.executePendingTransactions()
        val transaction = supportFragmentManager.beginTransaction()

        val currentFragment = currentTag?.let {
            supportFragmentManager.findFragmentByTag(it)
        }

        currentFragment?.let {
            transaction.hide(it)
        }

        var targetFragment = supportFragmentManager.findFragmentByTag(tag)

        if (targetFragment == null) {
            targetFragment = createFragment(tag)
            transaction.add(
                R.id.staff_home_fragment_container,
                targetFragment,
                tag
            )
        } else {
            transaction.show(targetFragment)
        }

        transaction.commit()

        currentTag = tag

        isBottomNavVisible = (tag != TAG_CHANGE_PASSWORD)
        updateBottomNavVisibility()

        return true
    }

    // Create Fragment
    private fun createFragment(tag: String): Fragment {
        return when (tag) {
            TAG_DASHBOARD -> StaffDashboardFragment()
            TAG_CREATE -> StaffTablesFragment()
            TAG_RESTAURANTS -> StaffOrdersFragment()
            TAG_LOGS -> StaffAlertFragment()
            TAG_PROFILE -> StaffProfileFragment()
            TAG_CHANGE_PASSWORD -> ChangePasswordFragment()
            else -> StaffDashboardFragment()
        }
    }

    // Back Press
    private var backPressedTime: Long = 0

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        binding.main.isDrawerOpen(GravityCompat.START) -> closeDrawer()
                        supportFragmentManager.backStackEntryCount > 0 -> {
                            supportFragmentManager.popBackStack()
                        }
                        currentTag != TAG_DASHBOARD -> openDashboard()
                        else -> {
                            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                                finish()
                            } else {
                                Toast.makeText(
                                    this@StaffHomeActivity,
                                    "Press back again to exit",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            backPressedTime = System.currentTimeMillis()
                        }
                    }
                }
            }
        )
    }

    fun openDrawer() { binding.main.openDrawer(GravityCompat.START) }
    fun closeDrawer() { binding.main.closeDrawer(GravityCompat.START) }
    fun hideBottomNavigation() { isBottomNavVisible = false; updateBottomNavVisibility() }
    fun showBottomNavigation() { isBottomNavVisible = true; updateBottomNavVisibility() }

    private fun updateBottomNavVisibility() {
        binding.staffHostBottomNav.visibility = if (isBottomNavVisible) View.VISIBLE else View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENT_TAG, currentTag)
        outState.putBoolean(KEY_BOTTOM_NAV_VISIBLE, isBottomNavVisible)
    }
}