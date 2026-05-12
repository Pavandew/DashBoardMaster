package com.example.masterdashboard.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityHomeBinding
import com.example.masterdashboard.home.create_res.CreateNewResFragment
import com.example.masterdashboard.home.dashboard.DashboardFragment
import com.example.masterdashboard.home.logs.LogsFragment
import com.example.masterdashboard.home.res_lists.views.RestaurantListsFragment
import com.example.masterdashboard.home.settings.manager.SettingsDrawerManager
import com.example.masterdashboard.home.settings.views.ChangePasswordFragment
import com.example.masterdashboard.home.settings.views.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var settingsDrawerManager: SettingsDrawerManager

    private var currentTag: String? = null
    private var pendingTag: String? = null

    companion object {
        private const val KEY_CURRENT_TAG = "current_tag"

        const val TAG_DASHBOARD = "dashboard"
        const val TAG_CREATE = "create"
        const val TAG_RESTAURANTS = "restaurants"
        const val TAG_LOGS = "logs"
        const val TAG_PROFILE = "profile"
        const val TAG_CHANGE_PASSWORD = "change_password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNav = binding.hostBottomNav

        setupBottomNavigation()
        setupDrawer()
        setupBackPress()

        if (savedInstanceState == null) {
            openDashboard()
        } else {
            currentTag = savedInstanceState.getString(KEY_CURRENT_TAG)
            navigateTo(currentTag ?: TAG_DASHBOARD)
        }
    }

    // ------------------------------------------------
    // Bottom Navigation
    // ------------------------------------------------
    private fun setupBottomNavigation() {

        bottomNav.setOnItemSelectedListener { item ->

            val tag = when (item.itemId) {
                R.id.dashboardFragment -> TAG_DASHBOARD
                R.id.createNewResFragment -> TAG_CREATE
                R.id.resListsFragment -> TAG_RESTAURANTS
                R.id.logFragment -> TAG_LOGS
                R.id.settingFragment -> pendingTag ?: TAG_PROFILE
                else -> return@setOnItemSelectedListener false
            }

            pendingTag = null
            loadFragment(tag)
            true
        }
    }

    // ------------------------------------------------
    // Drawer Setup
    // ------------------------------------------------
    private fun setupDrawer() {

        settingsDrawerManager =
            SettingsDrawerManager(
                this,
                binding.navigationView,
                binding.drawerLayout
            )

        settingsDrawerManager.setupDrawerItem()
    }

    // ------------------------------------------------
    // Public Navigation Methods (Use Anywhere)
    // ------------------------------------------------
    fun openDashboard() = navigateTo(TAG_DASHBOARD)

    fun openCreateRestaurant() = navigateTo(TAG_CREATE)

    fun openRestaurantList() = navigateTo(TAG_RESTAURANTS)

    fun openLogs() = navigateTo(TAG_LOGS)

    fun openProfile() = navigateTo(TAG_PROFILE)

    fun openChangePassword() = navigateTo(TAG_CHANGE_PASSWORD)

    // ------------------------------------------------
    // Single Navigation Entry Point
    // ------------------------------------------------
    fun navigateTo(tag: String) {

        pendingTag = tag

        when (tag) {
            TAG_DASHBOARD -> bottomNav.selectedItemId = R.id.dashboardFragment
            TAG_CREATE -> bottomNav.selectedItemId = R.id.createNewResFragment
            TAG_RESTAURANTS -> bottomNav.selectedItemId = R.id.resListsFragment
            TAG_LOGS -> bottomNav.selectedItemId = R.id.logFragment

            TAG_PROFILE,
            TAG_CHANGE_PASSWORD -> {

                if (bottomNav.selectedItemId == R.id.settingFragment) {
                    loadFragment(tag)
                } else {
                    bottomNav.selectedItemId = R.id.settingFragment
                }
            }
        }

        closeDrawer()
    }

    // ------------------------------------------------
    // Main Fragment Loader
    // ------------------------------------------------
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

        var targetFragment =
            supportFragmentManager.findFragmentByTag(tag)

        if (targetFragment == null) {
            targetFragment = createFragment(tag)
            transaction.add(
                R.id.home_fragment_container,
                targetFragment,
                tag
            )
        } else {
            transaction.show(targetFragment)
        }

        transaction.commit()

        currentTag = tag

        bottomNav.visibility =
            if (tag == TAG_CHANGE_PASSWORD)
                View.GONE
            else
                View.VISIBLE

        return true
    }

    // ------------------------------------------------
    // Create Fragment
    // ------------------------------------------------
    private fun createFragment(tag: String): Fragment {

        return when (tag) {
            TAG_DASHBOARD -> DashboardFragment()
            TAG_CREATE -> CreateNewResFragment()
            TAG_RESTAURANTS -> RestaurantListsFragment()
            TAG_LOGS -> LogsFragment()
            TAG_PROFILE -> ProfileFragment()
            TAG_CHANGE_PASSWORD -> ChangePasswordFragment()
            else -> DashboardFragment()
        }
    }

    // ------------------------------------------------
    // Drawer Control
    // ------------------------------------------------
    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    // ------------------------------------------------
    // Back Press
    // ------------------------------------------------
    private var backPressedTime: Long = 0

    private fun setupBackPress() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    when {

                        binding.drawerLayout.isDrawerOpen(
                            GravityCompat.START
                        ) -> closeDrawer()

                        currentTag != TAG_DASHBOARD -> {
                            openDashboard()
                        }

                        else -> {

                            if (
                                backPressedTime + 2000 >
                                System.currentTimeMillis()
                            ) {
                                finish()
                            } else {
                                Toast.makeText(
                                    this@HomeActivity,
                                    "Press back again to exit",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            backPressedTime =
                                System.currentTimeMillis()
                        }
                    }
                }
            }
        )
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENT_TAG, currentTag)
    }
}