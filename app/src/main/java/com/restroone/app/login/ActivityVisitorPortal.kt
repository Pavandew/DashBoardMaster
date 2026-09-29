package com.restroone.app.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.restroone.app.utils.PortalManager
import com.restroone.app.R
import com.restroone.app.databinding.ActivityVisitorPortalBinding
import com.restroone.app.login.models.PortalFeature
import com.restroone.app.login.models.PortalItem
import com.restroone.app.login.views.LoginActivity
import com.restroone.app.login.views.StaffLoginActivity
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.SessionManager

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

        // 1. Multi Restaurant Portal (Slate Blue Theme)
        val master = PortalItem(
            "Multi Restaurant",
            subTitle = "Multi Restaurant Owner Login",
            description = "Manage all restaurants and system settings",
            R.drawable.shield,
            R.color.portal_multi_primary,
            R.color.portal_multi_bg_light,
            listOf(
                PortalFeature(R.drawable.ic_restaurant_24dp, "Manage", R.color.portal_multi_primary),
                PortalFeature(R.drawable.ic_analytics_24dp, "Reports", R.color.portal_multi_primary),
                PortalFeature(R.drawable.ic_settings_24dp, "Settings", R.color.portal_multi_primary)
            )
        ) {
            sessionManager.setSelectedPortal(AppConstants.PORTAL_MULTI_RESTAURANT)
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // 2. Restaurant Portal (Muted Purple Theme)
        val manager = PortalItem(
            "Single Restaurant ",
            subTitle = "Owner/Manager Login",
            description = "Manage restaurant and staff",
            R.drawable.manager,
            R.color.portal_restaurant_primary,
            R.color.portal_restaurant_bg_light,
            listOf(
                PortalFeature(
                    R.drawable.ic_staffs_24dp,
                    "Staff Management",
                    R.color.portal_restaurant_primary
                ),
                PortalFeature(
                    R.drawable.ic_sales_report_24dp,
                    "Sales & Report",
                    R.color.portal_restaurant_primary
                ),
                PortalFeature(
                    R.drawable.ic_visibility_24dp,
                    "Inventory Overview",
                    R.color.portal_restaurant_primary
                )
            )
        ) {
            sessionManager.setSelectedPortal(AppConstants.PORTAL_RESTAURANT)
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // 3. Staff Portal (Warm Terracotta/Peach Theme)
        val staff = PortalItem(
            "Staff Portal",
            subTitle = "Working Staffs Login",
            "Take orders, manage tables and kitchens",
            R.drawable.waiter,
            R.color.portal_staff_primary,
            R.color.portal_staff_bg_light,
            listOf(
                PortalFeature(R.drawable.ic_table_24dp, "View Tables", R.color.portal_staff_primary),
                PortalFeature(
                    R.drawable.ic_order_approve_24dp,
                    "Take Order",
                    R.color.portal_staff_primary
                ),
                PortalFeature(R.drawable.ic_send_24dp, "Send KOT", R.color.portal_staff_primary)
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