package com.example.masterdashboard.home

import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityHomeBinding
import com.example.masterdashboard.home.dashboard.DashboardFragment
import com.example.masterdashboard.home.create_res.CreateNewResFragment
import com.example.masterdashboard.home.res_lists.views.RestaurantListsFragment
import com.example.masterdashboard.home.logs.LogsFragment
import com.example.masterdashboard.home.settings.views.SettingsFragment
import com.example.masterdashboard.home.settings.manager.SettingsDrawerManager
import com.example.masterdashboard.home.settings.views.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var settingsDrawerManager: SettingsDrawerManager

    // Fragment caching - keep fragments in memory for instant switching
    private val fragmentMap = mutableMapOf<Int, Fragment>()
    private var currentFragmentTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNav = binding.hostBottomNav
        // set Up Bottom Navigation
        setUpBottomNav(savedInstanceState)

        // Set up Drawer
        settingsDrawerManager =
            SettingsDrawerManager(this, binding.navigationView, binding.drawerLayout)
        settingsDrawerManager.setupDrawerItem()

    }

    private fun setUpBottomNav(savedInstanceState: Bundle?) {

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboardFragment -> loadFragment(R.id.dashboardFragment, "dashboard")
                R.id.createNewResFragment -> loadFragment(R.id.createNewResFragment, "create")
                R.id.resListsFragment -> loadFragment(R.id.resListsFragment, "restaurants")
                R.id.logFragment -> loadFragment(R.id.logFragment, "logs")
                R.id.settingFragment -> loadFragment(R.id.settingFragment, "settings")
                else -> false
            }
            return@setOnItemSelectedListener true
        }

        // Set default fragment
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.dashboardFragment
        }
    }

    private fun loadFragment(menuItemId: Int, tag: String): Boolean {
        // If the same fragment is already displayed, don't reload
        if (currentFragmentTag == tag) {
            return true
        }

        // Get or create fragment from cache
        var fragment = fragmentMap[menuItemId]
        if (fragment == null) {
            fragment = when (menuItemId) {
                R.id.dashboardFragment -> DashboardFragment()
                R.id.createNewResFragment -> CreateNewResFragment()
                R.id.resListsFragment -> RestaurantListsFragment()
                R.id.logFragment -> LogsFragment()
                R.id.settingFragment -> ProfileFragment()
                else -> return false
            }
            fragmentMap[menuItemId] = fragment
        }

        // Fast fragment transaction with optimizations
        supportFragmentManager.beginTransaction().apply {
            setReorderingAllowed(true)  // Better animation and performance
            replace(R.id.home_fragment_container, fragment, tag)
            commitNow()  // Executes immediately instead of queuing
        }

        currentFragmentTag = tag
        return true
    }

    // click the Drawer Item
    fun onClickDrawerItem(fragment: Fragment, tag: String){

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.home_fragment_container, fragment, tag)
            .addToBackStack(tag)
            .commit()

        currentFragmentTag = tag
         closeDrawer()

    }

    // Open the side drawer
    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    // close the side drawer
    fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    // Back Press close drawer first
    override fun onBackPressed() {
        if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }
}