package com.example.masterdashboard.staff_dash.home

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityStaffHomeBinding
import com.example.masterdashboard.master_dash.home.settings.views.ChangePasswordFragment
import com.example.masterdashboard.staff_dash.home.alert.StaffAlertFragment
import com.example.masterdashboard.staff_dash.home.dashboard.StaffDashboardFragment
import com.example.masterdashboard.staff_dash.home.order.views.StaffOrdersFragment
import com.example.masterdashboard.staff_dash.home.profile.StaffProfileFragment
import com.example.masterdashboard.staff_dash.home.table.views.StaffTablesFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class StaffHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStaffHomeBinding
    private lateinit var bottomNav: BottomNavigationView

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
        enableEdgeToEdge()

        binding = ActivityStaffHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Close drawer by default
        binding.main.closeDrawer(GravityCompat.START, false)

        bottomNav = binding.staffHostBottomNav

        setupBottomNavigation()
        setupDrawer()
        setupBackPress()

        if (savedInstanceState == null) {
            Log.i(TAG, "onCreate: Fresh launch, loading Dashboard")
            openDashboard()
        } else {
            currentTag = savedInstanceState.getString(KEY_CURRENT_TAG)
            isBottomNavVisible = savedInstanceState.getBoolean(KEY_BOTTOM_NAV_VISIBLE, true)

            Log.d(
                TAG,
                "onCreate: Restoring state, currentTag: $currentTag, bottomNavVisible: $isBottomNavVisible"
            )

            navigateTo(currentTag ?: TAG_DASHBOARD)
            updateBottomNavVisibility()
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
                R.id.profile -> {
                    Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
                    openProfile()
                }
                R.id.changePassword -> {
                    Toast.makeText(this, "Change Password", Toast.LENGTH_SHORT).show()
                    openChangePassword()
                }
                R.id.logout -> {
                    // Logic for logout can be added here
                    Toast.makeText(this, "Logout clicked", Toast.LENGTH_SHORT).show()
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

    // Drawer Control
    fun openDrawer() {
        binding.main.openDrawer(GravityCompat.START)
    }

    fun closeDrawer() {
        binding.main.closeDrawer(GravityCompat.START)
    }

    fun hideBottomNavigation() {
        isBottomNavVisible = false
        updateBottomNavVisibility()
    }

    fun showBottomNavigation() {
        isBottomNavVisible = true
        updateBottomNavVisibility()
    }

    private fun updateBottomNavVisibility() {
        binding.staffHostBottomNav.visibility = if (isBottomNavVisible) View.VISIBLE else View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENT_TAG, currentTag)
        outState.putBoolean(KEY_BOTTOM_NAV_VISIBLE, isBottomNavVisible)
    }
}
