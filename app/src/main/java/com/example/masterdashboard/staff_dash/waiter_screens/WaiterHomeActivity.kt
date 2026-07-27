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
import com.example.masterdashboard.databinding.ActivityWaiterHomeBinding
import com.example.masterdashboard.master_dash.settings.views.ChangePasswordFragment
import com.example.masterdashboard.staff_dash.waiter_screens.alert.StaffAlertFragment
import com.example.masterdashboard.staff_dash.waiter_screens.dashboard.WaiterDashboardFragment
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.WaiterActiveOrdersFragment
import com.example.masterdashboard.staff_dash.profile.StaffProfileFragment
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.WaiterTablesFragment
import com.example.masterdashboard.login.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class WaiterHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWaiterHomeBinding
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var sessionManager: SessionManager

    private var currentTag: String? = null
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
        // Add this to fix navigation bar icon colors over white backgrounds
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityWaiterHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT

        sessionManager = SessionManager(this)
        bottomNav = binding.waiterBottomNavigation

        // Explicit Logging
        Log.i(TAG, "🚀 ACTIVITY INITIALIZED: WaiterHomeActivity running.")

        binding.main.closeDrawer(GravityCompat.START, false)

//        setupWindowInsets()
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

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())

            // 1. Pad only the top of the container to clear the status bar clock/battery
            binding.waiterFragmentContainer.updatePadding(top = statusBars.top)

            // 2. Pad BOTTOM of container to clear navigation bar ONLY IF bottom nav is hidden
            val bottomPadding = if (isBottomNavVisible) 0 else navigationBars.bottom
            binding.waiterFragmentContainer.updatePadding(bottom = bottomPadding)

            // 3. Adjust the margin of the card layout so it floats perfectly above the system navigation pill/buttons
            val cardParams = binding.bottomNavContainer.layoutParams as android.view.ViewGroup.MarginLayoutParams
            val baseMarginInPx = (8 * resources.displayMetrics.density).toInt()
            cardParams.bottomMargin = baseMarginInPx + navigationBars.bottom
            binding.bottomNavContainer.layoutParams = cardParams

            // Automatically hide bottom navigation when keyboard is open
            binding.bottomNavContainer.visibility = if (imeVisible || !isBottomNavVisible) View.GONE else View.VISIBLE

            windowInsets
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            Log.d(TAG, "BottomNav: Selected ${item.title}")

            val tag = when (item.itemId) {
                R.id.staff_dashboardFragment -> TAG_DASHBOARD
                R.id.staff_tableFragment      -> TAG_CREATE
                R.id.staff_orderFragment      -> TAG_RESTAURANTS
                R.id.staff_logFragment        -> TAG_LOGS
                R.id.staff_settingFragment    -> {
                    // FIX: Avoid breaking flow if current screen is already deep inside Settings
                    if (currentTag == TAG_CHANGE_PASSWORD) TAG_CHANGE_PASSWORD else TAG_PROFILE
                }
                else -> return@setOnItemSelectedListener false
            }

            loadFragment(tag)
            true
        }
    }

    private fun setupDrawer() {
        binding.staffNavigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.profile -> openProfile()
                R.id.changePassword -> openChangePassword()
                R.id.logout -> {
                    Toast.makeText(this, "Session terminated.", Toast.LENGTH_SHORT).show()
                    sessionManager.logout()
                    finish()
                }
            }
            closeDrawer()
            true
        }
    }

    fun openDashboard() = navigateTo(TAG_DASHBOARD)
    fun openTables() = navigateTo(TAG_CREATE)
    fun openOrders() = navigateTo(TAG_RESTAURANTS)
    fun openAlerts() = navigateTo(TAG_LOGS)
    fun openProfile() = navigateTo(TAG_PROFILE)
    fun openChangePassword() = navigateTo(TAG_CHANGE_PASSWORD)

    fun navigateTo(tag: String) {
        // Handle custom navigation cases without tracking intermediate pending variables
        when (tag) {
            TAG_DASHBOARD -> bottomNav.selectedItemId = R.id.staff_dashboardFragment
            TAG_CREATE    -> bottomNav.selectedItemId = R.id.staff_tableFragment
            TAG_RESTAURANTS -> bottomNav.selectedItemId = R.id.staff_orderFragment
            TAG_LOGS      -> bottomNav.selectedItemId = R.id.staff_logFragment
            TAG_PROFILE, TAG_CHANGE_PASSWORD -> {
                if (bottomNav.selectedItemId == R.id.staff_settingFragment) {
                    loadFragment(tag)
                } else {
                    // This triggers bottomNav listener which defaults to TAG_PROFILE or checks currentTag
                    bottomNav.selectedItemId = R.id.staff_settingFragment
                    if (tag == TAG_CHANGE_PASSWORD) {
                        loadFragment(TAG_CHANGE_PASSWORD)
                    }
                }
            }
        }
        closeDrawer()
    }

    fun loadFragment(tag: String): Boolean {
        if (currentTag == tag) return true

        supportFragmentManager.executePendingTransactions()
        val transaction = supportFragmentManager.beginTransaction()

        currentTag?.let { oldTag ->
            supportFragmentManager.findFragmentByTag(oldTag)?.let { transaction.hide(it) }
        }

        var targetFragment = supportFragmentManager.findFragmentByTag(tag)
        if (targetFragment == null) {
            targetFragment = createFragment(tag)
            transaction.add(R.id.waiter_fragment_container, targetFragment, tag)
        } else {
            transaction.show(targetFragment)
        }

        transaction.commit()
        currentTag = tag

        isBottomNavVisible = (tag != TAG_CHANGE_PASSWORD)
        updateBottomNavVisibility()

        return true
    }

    private fun createFragment(tag: String): Fragment {
        return when (tag) {
            TAG_DASHBOARD       -> WaiterDashboardFragment()
            TAG_CREATE          -> WaiterTablesFragment()
            TAG_RESTAURANTS     -> WaiterActiveOrdersFragment()
            TAG_LOGS            -> StaffAlertFragment()
            TAG_PROFILE         -> StaffProfileFragment()
            TAG_CHANGE_PASSWORD -> ChangePasswordFragment()
            else                -> WaiterDashboardFragment()
        }
    }

    private var backPressedTime: Long = 0

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val handledByChild = supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.popBackStackImmediate() ?: false

                when {
                    binding.main.isDrawerOpen(GravityCompat.START) -> closeDrawer()
                    handledByChild -> { /* Child fragment internal backstack consumed press */ }
                    supportFragmentManager.backStackEntryCount > 0 -> supportFragmentManager.popBackStack()
                    currentTag != TAG_DASHBOARD -> openDashboard()
                    else -> {
                        if (backPressedTime + 2000 > System.currentTimeMillis()) {
                            finish()
                        } else {
                            Toast.makeText(this@WaiterHomeActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                        }
                        backPressedTime = System.currentTimeMillis()
                    }
                }
            }
        })
    }

    fun openDrawer() { binding.main.openDrawer(GravityCompat.START) }
    fun closeDrawer() { binding.main.closeDrawer(GravityCompat.START) }
    fun hideBottomNavigation() { isBottomNavVisible = false; updateBottomNavVisibility() }
    fun showBottomNavigation() { isBottomNavVisible = true; updateBottomNavVisibility() }

    private fun updateBottomNavVisibility() {
        binding.waiterBottomNavigation.visibility = if (isBottomNavVisible) View.VISIBLE else View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENT_TAG, currentTag)
        outState.putBoolean(KEY_BOTTOM_NAV_VISIBLE, isBottomNavVisible)
    }
}