package com.example.masterdashboard.staff_dash.waiter_screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityWaiterHomeBinding
import com.example.masterdashboard.notifications.alert.StaffNotificationFragment
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.WaiterActiveOrdersFragment
import com.example.masterdashboard.staff_dash.profile.StaffProfileFragment
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.WaiterTablesFragment
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.notifications.NotificationPermissionHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class WaiterHomeActivity : AppCompatActivity() {

    private lateinit var permissionHelper: NotificationPermissionHelper
    private lateinit var binding: ActivityWaiterHomeBinding
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var sessionManager: SessionManager

    private var currentTag: String? = null
    private val TAG = "WaiterHomeActivity"

    companion object {
        private const val KEY_CURRENT_TAG = "current_tag"
        const val TAG_TABLES = "table"
        const val TAG_ORDERS = "order"
        const val TAG_LOGS = "alert"
        const val TAG_PROFILE = "profile"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityWaiterHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHelper = NotificationPermissionHelper(this)
        sessionManager = SessionManager(this)
        bottomNav = binding.waiterBottomNavigation

        setupBottomNavigation()
        setupBackstackListener()
        setupBackPress()

        if (savedInstanceState == null) {
            openTables()
        } else {
            currentTag = savedInstanceState.getString(KEY_CURRENT_TAG)
            navigateTo(currentTag ?: TAG_TABLES)
        }

        permissionHelper.askNotificationPermission()
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            val tag = when (item.itemId) {
                R.id.staff_tableFragment   -> TAG_TABLES
                R.id.staff_orderFragment   -> TAG_ORDERS
                R.id.staff_logFragment     -> TAG_LOGS
                R.id.staff_profileFragment -> TAG_PROFILE
                else -> return@setOnItemSelectedListener false
            }
            loadFragment(tag)
            true
        }
    }

    /**
     * Automatic Bottom Nav Visibility: 
     * Hides nav when we are in a sub-screen (backstack > 0)
     */
    private fun setupBackstackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            val isSubScreen = supportFragmentManager.backStackEntryCount > 0
            binding.bottomNavContainer.visibility = if (isSubScreen) View.GONE else View.VISIBLE
        }
    }

    fun openTables() = navigateTo(TAG_TABLES)
    fun openOrders() = navigateTo(TAG_ORDERS)
    fun openAlerts() = navigateTo(TAG_LOGS)
    fun openProfile() = navigateTo(TAG_PROFILE)

    fun navigateTo(tag: String) {
        when (tag) {
            TAG_TABLES  -> bottomNav.selectedItemId = R.id.staff_tableFragment
            TAG_ORDERS  -> bottomNav.selectedItemId = R.id.staff_orderFragment
            TAG_LOGS    -> bottomNav.selectedItemId = R.id.staff_logFragment
            TAG_PROFILE -> bottomNav.selectedItemId = R.id.staff_profileFragment
        }
    }

    fun loadFragment(tag: String) {
        if (currentTag == tag) return

        val transaction = supportFragmentManager.beginTransaction()
        
        // Hide existing
        currentTag?.let { oldTag ->
            supportFragmentManager.findFragmentByTag(oldTag)?.let { transaction.hide(it) }
        }

        // Show/Add target
        var targetFragment = supportFragmentManager.findFragmentByTag(tag)
        if (targetFragment == null) {
            targetFragment = when (tag) {
                TAG_TABLES  -> WaiterTablesFragment()
                TAG_ORDERS  -> WaiterActiveOrdersFragment()
                TAG_LOGS    -> StaffNotificationFragment()
                TAG_PROFILE -> StaffProfileFragment()
                else        -> WaiterTablesFragment()
            }
            transaction.add(R.id.waiter_fragment_container, targetFragment, tag)
        } else {
            transaction.show(targetFragment)
        }

        transaction.commit()
        currentTag = tag
    }

    private var backPressedTime: Long = 0

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        supportFragmentManager.backStackEntryCount > 0 -> {
                            supportFragmentManager.popBackStack()
                        }
                        currentTag != TAG_TABLES -> {
                            openTables()
                        }
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
            }
        )
    }

    fun hideBottomNavigation() { binding.bottomNavContainer.visibility = View.GONE }
    fun showBottomNavigation() { binding.bottomNavContainer.visibility = View.VISIBLE }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENT_TAG, currentTag)
    }
}
