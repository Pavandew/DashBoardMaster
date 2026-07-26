package com.example.masterdashboard.staff_dash.kitchen_screens

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityKitchenHomeBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenDashboardFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenPreparationFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenInventoryFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenOrderFragment
import com.example.masterdashboard.staff_dash.profile.StaffProfileFragment

class KitchenHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKitchenHomeBinding
    private var currentSelectedItem = R.id.kitchen_orderFragment

    // Tags to keep track of main base fragments in supportFragmentManager
    private val tags = object {
        val ORDER = "order"
        val KITCHEN = "kitchen"
        val INVENTORY = "inventory"
        val PROFILE = "profile"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityKitchenHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // --- NEW: DYNAMIC BACKSTACK MONITOR TO HIDE/SHOW BOTTOM NAV ---
        supportFragmentManager.addOnBackStackChangedListener {
            val backStackCount = supportFragmentManager.backStackEntryCount
            if (backStackCount > 0) {
                // User is in a deep screen (like detail view). Hide the custom floating menu completely.
                binding.bottomNavContainer.visibility = View.GONE
            } else {
                // User returned to one of the main 5 tabs. Restore bottom navigation visibility immediately.
                binding.bottomNavContainer.visibility = View.VISIBLE
            }
        }

        // Edge To Edge Handler
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.kitchenFragmentContainer.setPadding(0, systemBars.top, 0, systemBars.bottom)
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

        val transaction = fm.beginTransaction().setReorderingAllowed(true)

        // Add all tabs to layout container if not already existing
        if (!order.isAdded) transaction.add(R.id.kitchen_fragment_container, order, tags.ORDER).hide(order)
        if (!kitchen.isAdded) transaction.add(R.id.kitchen_fragment_container, kitchen, tags.KITCHEN).hide(kitchen)
        if (!inventory.isAdded) transaction.add(R.id.kitchen_fragment_container, inventory, tags.INVENTORY).hide(inventory)
        if (!profile.isAdded) transaction.add(R.id.kitchen_fragment_container, profile, tags.PROFILE).hide(profile)

        transaction.commit()
    }

    /**
     * Instantly shows the selected tab fragment and hides all others via memory tags.
     */
    private fun switchFragmentTo(targetTag: String) {
        val fm = supportFragmentManager
        val transaction = fm.beginTransaction().setReorderingAllowed(true)

        val allTags = listOf(tags.ORDER, tags.KITCHEN, tags.INVENTORY, tags.PROFILE)

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
}