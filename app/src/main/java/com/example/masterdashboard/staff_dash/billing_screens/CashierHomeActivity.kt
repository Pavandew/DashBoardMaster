package com.example.masterdashboard.staff_dash.billing_screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityCashierHomeBinding
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.notifications.NotificationPermissionHelper
import com.example.masterdashboard.staff_dash.billing_screens.views.CashierBillingFragment
import com.example.masterdashboard.staff_dash.billing_screens.views.CashierOrderFragment
import com.example.masterdashboard.staff_dash.profile.StaffProfileFragment
import com.example.masterdashboard.notifications.alert.NotificationFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class CashierHomeActivity : AppCompatActivity() {

    private lateinit var permissionHelper: NotificationPermissionHelper
    private lateinit var binding: ActivityCashierHomeBinding
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var sessionManager: SessionManager

    private var currentTag: String? = null

    companion object {
        private const val KEY_CURRENT_TAG = "current_tag"
        const val TAG_BILLS = "bills"
        const val TAG_ORDERS = "order"
        const val TAG_LOGS = "alert"
        const val TAG_PROFILE = "profile"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure system bar icons for light/dark background visibility
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding = ActivityCashierHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHelper = NotificationPermissionHelper(this)
        sessionManager = SessionManager(this)
        bottomNav = binding.billingBottomNavigation

        setupBottomNavigation()
        setupBackstackListener()
        setupBackPress()

        if (savedInstanceState == null) {
            openBills()
        } else {
            currentTag = savedInstanceState.getString(KEY_CURRENT_TAG)
            navigateTo(currentTag ?: TAG_BILLS)
        }

        permissionHelper.askNotificationPermission()
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            val tag = when (item.itemId) {
                R.id.cashier_billsFragment      -> TAG_BILLS
                R.id.cashier_orderFragment      -> TAG_ORDERS
                R.id.cashier_logFragment        -> TAG_LOGS
                R.id.cashier_settingFragment    -> TAG_PROFILE
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

    fun openBills() = navigateTo(TAG_BILLS)
    fun openOrders() = navigateTo(TAG_ORDERS)
    fun openAlerts() = navigateTo(TAG_LOGS)
    fun openProfile() = navigateTo(TAG_PROFILE)

    fun navigateTo(tag: String) {
        when (tag) {
            TAG_BILLS    -> bottomNav.selectedItemId = R.id.cashier_billsFragment
            TAG_ORDERS    -> bottomNav.selectedItemId = R.id.cashier_orderFragment
            TAG_LOGS      -> bottomNav.selectedItemId = R.id.cashier_logFragment
            TAG_PROFILE  -> bottomNav.selectedItemId = R.id.cashier_settingFragment
        }
    }

    fun loadFragment(tag: String) {
        if (currentTag == tag) return

        val transaction = supportFragmentManager.beginTransaction()

        currentTag?.let { oldTag ->
            supportFragmentManager.findFragmentByTag(oldTag)?.let { transaction.hide(it) }
        }

        var targetFragment = supportFragmentManager.findFragmentByTag(tag)
        if (targetFragment == null) {
            targetFragment = when (tag) {
                TAG_BILLS           -> CashierBillingFragment()
                TAG_ORDERS          -> CashierOrderFragment()
                TAG_LOGS            -> NotificationFragment()
                TAG_PROFILE         -> StaffProfileFragment()
                else                -> CashierBillingFragment()
            }
            transaction.add(R.id.billing_fragment_container, targetFragment, tag)
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
                    if (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStack()
                        return
                    }

                    // If this activity is NOT the root (e.g. opened from Manager Dash), just go back
                    if (!isTaskRoot) {
                        finish()
                        return
                    }

                    // Standard "Root Home" behavior: Back to first tab, then exit toast
                    if (currentTag != TAG_BILLS) {
                        openBills()
                    } else {
                        if (backPressedTime + 2000 > System.currentTimeMillis()) {
                            finish()
                        } else {
                            Toast.makeText(this@CashierHomeActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
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
