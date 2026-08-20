package com.example.masterdashboard.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.masterdashboard.utils.PortalManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityVisitorPortalBinding
import com.example.masterdashboard.login.models.PortalFeature
import com.example.masterdashboard.login.models.PortalItem
import com.example.masterdashboard.login.views.LoginActivity
import com.example.masterdashboard.login.views.StaffLoginActivity
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager

class ActivityVisitorPortal : AppCompatActivity() {

    companion object{
        private val TAG = "ActivityVisitorPortal"
    }

    private lateinit var binding: ActivityVisitorPortalBinding
    private lateinit var portalManager: PortalManager
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityVisitorPortalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        portalManager = PortalManager(this)
        sessionManager = SessionManager(this)

        // 1. Setup Title Gradient
        portalManager.applyTextGradient(binding.visitorPortalTitle)

        // 2. Setup All Cards
        setupAllPortals()
    }

    private fun setupAllPortals() {

        // 1. Multi Restaurant Portal
        val master = PortalItem(
            "Multi Restaurant",
            subTitle = "Manage Multiple",
            description = "Manage all restaurants and system settings",
            R.drawable.shield,
            R.color.primary_blue,
            listOf(
                PortalFeature(R.drawable.ic_restaurant_24dp, "Manage", R.color.primary_blue),
                PortalFeature(R.drawable.ic_analytics_24dp, "Reports", R.color.primary_blue),
                PortalFeature(R.drawable.ic_settings_24dp, "Settings", R.color.primary_blue)
            )
        ) {
            sessionManager.setSelectedPortal(AppConstants.PORTAL_MULTI_RESTAURANT)

            startActivity(Intent(this, LoginActivity::class.java))
//            Toast.makeText(this, "Master Portal - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        // 2. Restaurant Portal
        val manager = PortalItem(
            "Restaurant Portal",
            subTitle = "Owner/Manager",
            description = "Manage restaurant and staff",
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
            sessionManager.setSelectedPortal(AppConstants.PORTAL_RESTAURANT)
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // 3. Staff Portal
        val staff = PortalItem(
            "Staff Dashboard",
            subTitle = "Working Staffs",
            "Take orders manage tables and Kitchens",
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
            sessionManager.setSelectedPortal(AppConstants.PORTAL_STAFF)

            startActivity(Intent(this, StaffLoginActivity::class.java))
        }

        // Bind data using the manager
        portalManager.bindCard(binding.cardMaster, master)
        portalManager.bindCard(binding.cardManager, manager)
        portalManager.bindCard(binding.cardWaiter, staff)
    }
}