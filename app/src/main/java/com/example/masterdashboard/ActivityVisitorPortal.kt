package com.example.masterdashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.masterdashboard.databinding.ActivityVisitorPortalBinding
import com.example.masterdashboard.master_dash.login.models.PortalFeature
import com.example.masterdashboard.master_dash.login.models.PortalItem
import com.example.masterdashboard.master_dash.login.views.LoginActivity
import com.example.masterdashboard.staff_dash.login.StaffLoginActivity

class ActivityVisitorPortal : AppCompatActivity() {

    private lateinit var binding: ActivityVisitorPortalBinding
    private lateinit var portalManager: PortalManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVisitorPortalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        portalManager = PortalManager(this)

        // 1. Setup Title Gradient
        portalManager.applyTextGradient(binding.visitorPortalTitle)

        // 2. Setup All Cards
        setupAllPortals()
    }

    private fun setupAllPortals() {
        // 1. Master Portal
        val master = PortalItem(
            "Master Dashboard",
            "Manage all restaurants and system settings",
            R.drawable.shield,
            R.color.primary_blue,
            listOf(
                PortalFeature(R.drawable.ic_restaurant_24dp, "Manage", R.color.primary_blue),
                PortalFeature(R.drawable.ic_analytics_24dp, "Reports", R.color.primary_blue),
                PortalFeature(R.drawable.ic_settings_24dp, "Settings", R.color.primary_blue)
            )
        ) {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // 2. Billing Portal
        val billing = PortalItem(
            "Billing Dashboard",
            "Handle orders, payments and daily billing",
            R.drawable.biling,
            R.color.primary_green,
            listOf(
                PortalFeature(R.drawable.ic_logs_24dp, "Create bill", R.color.primary_green),
                PortalFeature(R.drawable.ic_payments_24dp, "Payments", R.color.primary_green),
                PortalFeature(R.drawable.ic_inventory_24dp, "Inventory", R.color.primary_green)
            )
        ) {
            // No activity currently
            startActivity(Intent(this, StaffLoginActivity::class.java))
        }

        // 3. Manager Portal
        val manager = PortalItem(
            "Manager Dashboard",
            "View sales reports and manage staff",
            R.drawable.manager,
            R.color.primary_purple,
            listOf(
                PortalFeature(
                    R.drawable.ic_staffs_24dp,
                    "Staff Management",
                    R.color.primary_purple
                ),
                PortalFeature(
                    R.drawable.ic_sales_report_24dp,
                    "Sales & Report",
                    R.color.primary_purple
                ),
                PortalFeature(
                    R.drawable.ic_visibility_24dp,
                    "Inventory Overview",
                    R.color.primary_purple
                )
            )
        ) {
            // No activity currently
        }

        // 4. Waiter Portal
        val waiter = PortalItem(
            "Waiter Dashboard",
            "Take orders and manage tables",
            R.drawable.waiter,
            R.color.primary_orange,
            listOf(
                PortalFeature(R.drawable.ic_table_24dp, "Floor & Tables", R.color.primary_orange),
                PortalFeature(
                    R.drawable.ic_order_approve_24dp,
                    "Take Order",
                    R.color.primary_orange
                ),
                PortalFeature(R.drawable.ic_send_24dp, "Send KOT", R.color.primary_orange)
            )
        ) {
            // No activity currently
            startActivity(Intent(this, StaffLoginActivity::class.java))
        }

        // Bind data using the manager
        portalManager.bindCard(binding.cardMaster, master)
        portalManager.bindCard(binding.cardBilling, billing)
        portalManager.bindCard(binding.cardManager, manager)
        portalManager.bindCard(binding.cardWaiter, waiter)
    }
}