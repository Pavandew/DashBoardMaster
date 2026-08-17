package com.example.masterdashboard.staff_dash.kitchen_screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityKitchenHomeBinding
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.notifications.NotificationHelper
import com.example.masterdashboard.notifications.NotificationPermissionHelper
import com.example.masterdashboard.staff_dash.kitchen_screens.service.KitchenNotificationService
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenPreparationFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenInventoryFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenOrderFragment
import com.example.masterdashboard.staff_dash.profile.StaffProfileFragment
import com.example.masterdashboard.staff_dash.waiter_screens.alert.StaffNotificationFragment
import com.example.masterdashboard.login.views.ChangePasswordFragment

class KitchenHomeActivity : AppCompatActivity() {

    private lateinit var permissionHelper: NotificationPermissionHelper
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var binding: ActivityKitchenHomeBinding
    private var currentSelectedItem = R.id.kitchen_orderFragment

    // Tags to keep track of main base fragments in supportFragmentManager
    private val tags = object {
        val ORDER = "order"
        val KITCHEN = "kitchen"
        val INVENTORY = "inventory"
        val PROFILE = "profile"
        val NOTIFICATION = "notification"
        val CHANGE_PASSWORD = "change_password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityKitchenHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHelper = NotificationPermissionHelper(this)
        notificationHelper = NotificationHelper(this)
        sessionManager = SessionManager(this)

        startKitchenService()

        // Restore state if available
        if (savedInstanceState != null) {
            currentSelectedItem = savedInstanceState.getInt("currentSelectedItem", R.id.kitchen_orderFragment)
        }

        // Initialize fragments on first boot safely without replacing
        if (savedInstanceState == null) {
            initializeNavigationFramework()
        }

        // Listener to monitor menu selections
        binding.kitchenBottomNavigation.setOnItemSelectedListener { item ->
            if (currentSelectedItem == item.itemId) {
                return@setOnItemSelectedListener true
            }

            val targetTag = when (item.itemId) {
                R.id.kitchen_orderFragment -> tags.ORDER
                R.id.kitchen_Fragment -> tags.KITCHEN
                R.id.kitchen_inventoryFragment -> tags.INVENTORY
                R.id.kitchen_profileFragment -> tags.PROFILE
                R.id.kitchen_notificationFragment -> tags.NOTIFICATION
                else -> null
            }

            if (targetTag != null) {
                switchFragmentTo(targetTag)
                currentSelectedItem = item.itemId
                true
            } else {
                false
            }
        }

        // Sync selection on launch (especially if restored)
        binding.kitchenBottomNavigation.selectedItemId = currentSelectedItem

        // --- NEW: DYNAMIC BACKSTACK MONITOR TO HIDE/SHOW BOTTOM NAV ---
        supportFragmentManager.addOnBackStackChangedListener {
            // Trigger insets update to refresh visibility based on new backstack count
            ViewCompat.requestApplyInsets(binding.main)
        }

        // Edge To Edge Handler & Keyboard Visibility Monitor
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val backStackCount = supportFragmentManager.backStackEntryCount

            // 1. Pad top of container for status bar
            binding.kitchenFragmentContainer.setPadding(0, statusBars.top, 0, 0)

            // 2. Hide bottom navigation if keyboard is visible OR if we are in a deep screen (backstack > 0)
            val shouldHideBottomNav = imeVisible || backStackCount > 0
            
            if (shouldHideBottomNav) {
                binding.bottomNavContainer.visibility = View.GONE
                // When hidden, ensure container doesn't have extra bottom padding
                // and its constraints let it fill the bottom area (clearing nav bar)
                binding.kitchenFragmentContainer.updatePadding(bottom = navigationBars.bottom)
            } else {
                binding.bottomNavContainer.visibility = View.VISIBLE
                binding.kitchenFragmentContainer.updatePadding(bottom = 0)
            }

            insets
        }

        // Optimized Back Press Routing
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val backStackCount = supportFragmentManager.backStackEntryCount

                if (backStackCount > 0) {
                    // 1. If there's a detailed screen open, pop it off and return to previous fragment safely
                    supportFragmentManager.popBackStack()
                } else if (currentSelectedItem != R.id.kitchen_orderFragment) {
                    // 2. If no backstack exists but user isn't on the Home tab, route them back to the Home tab
                    binding.kitchenBottomNavigation.selectedItemId = R.id.kitchen_orderFragment
                } else {
                    // 3. Close the application gracefully
                    finish()
                }
            }
        })

        permissionHelper.askNotificationPermission()
    }

    fun hideBottomNavigation() {
        binding.bottomNavContainer.visibility = View.GONE
    }

    fun showBottomNavigation() {
        binding.bottomNavContainer.visibility = View.VISIBLE
    }

    /**
     * Instantiates all fragments once and hides the inactive ones.
     * This keeps data warm in background memory for instant loading.
     */
    private fun initializeNavigationFramework() {
        val fm = supportFragmentManager
        val order = fm.findFragmentByTag(tags.ORDER) ?: KitchenOrderFragment()
        val kitchen = fm.findFragmentByTag(tags.KITCHEN) ?: KitchenPreparationFragment()
        val inventory = fm.findFragmentByTag(tags.INVENTORY) ?: KitchenInventoryFragment()
        val profile = fm.findFragmentByTag(tags.PROFILE) ?: StaffProfileFragment()
        val notification = fm.findFragmentByTag(tags.NOTIFICATION) ?: StaffNotificationFragment()

        val transaction = fm.beginTransaction().setReorderingAllowed(true)

        // Add all tabs to layout container if not already existing
        if (!order.isAdded) transaction.add(R.id.kitchen_fragment_container, order, tags.ORDER)
        if (!kitchen.isAdded) transaction.add(R.id.kitchen_fragment_container, kitchen, tags.KITCHEN).hide(kitchen)
        if (!inventory.isAdded) transaction.add(R.id.kitchen_fragment_container, inventory, tags.INVENTORY).hide(inventory)
        if (!profile.isAdded) transaction.add(R.id.kitchen_fragment_container, profile, tags.PROFILE).hide(profile)
        if (!notification.isAdded) transaction.add(R.id.kitchen_fragment_container, notification, tags.NOTIFICATION).hide(notification)

        // Show the default tab
        transaction.show(order)
        transaction.commit()
    }

    /**
     * Instantly shows the selected tab fragment and hides all others via memory tags.
     */
    private fun switchFragmentTo(targetTag: String) {
        val fm = supportFragmentManager
        val transaction = fm.beginTransaction().setReorderingAllowed(true)

        val allTags = listOf(tags.ORDER, tags.KITCHEN, tags.INVENTORY, tags.PROFILE, tags.NOTIFICATION)

        for (tag in allTags) {
            val fragment = fm.findFragmentByTag(tag)
            if (fragment != null) {
                if (tag == targetTag) {
                    transaction.show(fragment)
                } else {
                    transaction.hide(fragment)
                }
            }
        }
        transaction.commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentSelectedItem", currentSelectedItem)
    }

    private fun startKitchenService() {
        val intent = Intent(this, KitchenNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
