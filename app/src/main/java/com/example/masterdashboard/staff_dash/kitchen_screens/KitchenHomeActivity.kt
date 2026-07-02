package com.example.masterdashboard.staff_dash.kitchen_screens

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityKitchenHomeBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenDashboardFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenInventoryFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenOrderFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenProfileFragment

class KitchenHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKitchenHomeBinding

    private var currentSelectedItem = R.id.kitchen_dashboardFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityKitchenHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default Fragment
        if (savedInstanceState == null) {
            replaceFragment(KitchenDashboardFragment())
            binding.kitchenBottomNavigation.selectedItemId = R.id.kitchen_dashboardFragment
        }

        binding.kitchenBottomNavigation.setOnItemSelectedListener { item ->

            // Prevent reloading same fragment
            if (currentSelectedItem == item.itemId) {
                return@setOnItemSelectedListener true
            }

            currentSelectedItem = item.itemId

            when (item.itemId) {

                R.id.kitchen_dashboardFragment -> {
                    replaceFragment(KitchenDashboardFragment())
                    true
                }

                R.id.kitchen_orderFragment -> {
                    replaceFragment(KitchenOrderFragment())
                    true
                }

                R.id.kitchen_Fragment -> {
                    replaceFragment(KitchenFragment())
                    true
                }

                R.id.kitchen_inventoryFragment -> {
                    replaceFragment(KitchenInventoryFragment())
                    true
                }

                R.id.kitchen_profileFragment -> {
                    replaceFragment(KitchenProfileFragment())
                    true
                }

                else -> false
            }
        }

        // Edge To Edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.kitchenFragmentContainer.setPadding(
                0,
                systemBars.top,
                0,
                systemBars.bottom
            )

            insets
        }

        // Back Press
        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    if (currentSelectedItem != R.id.kitchen_dashboardFragment) {

                        binding.kitchenBottomNavigation.selectedItemId =
                            R.id.kitchen_dashboardFragment

                    } else {

                        finish()

                    }
                }
            })
    }

    private fun replaceFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(0, 0)
            .replace(R.id.kitchen_fragment_container, fragment)
            .commit()
    }
}