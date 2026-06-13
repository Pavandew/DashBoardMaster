package com.example.masterdashboard.manager_single_res_dash.home.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentManagerDashboardBinding
import com.example.masterdashboard.manager_single_res_dash.home.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.home.utils.NavigationHelper
import com.example.masterdashboard.manager_single_res_dash.home.adapter.DrawerMenuAdapter
import com.example.masterdashboard.manager_single_res_dash.home.adapter.ManagerDashboardAdapter
import com.example.masterdashboard.manager_single_res_dash.home.models.DashboardSummary
import com.example.masterdashboard.manager_single_res_dash.home.models.DrawerMenuItem
import com.example.masterdashboard.manager_single_res_dash.home.models.TopSellingFoodItem
import com.example.masterdashboard.manager_single_res_dash.home.models.StatMetric
import com.example.masterdashboard.staff_dash.home.order.views.StaffOrdersFragment

class ManagerDashboardFragment : Fragment() {

    private var _binding: FragmentManagerDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var navigationHelper: NavigationHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("ManagerDashboardFragment", "Navigation: ManagerDashboardFragment Opened")
        // 1. Initialize central navigation engine helper
        navigationHelper = NavigationHelper(this)

        setupHeaderClickListeners()
        setupDashboardRecyclerView()
        setupActivityDrawerRecyclerView() // 2. Bind side menu layout dynamically
    }

    private fun setupHeaderClickListeners() {
        binding.masterDashHeader.btnDrawerMenu.setOnClickListener {
            (activity as? ManagerHomeActivity)?.openNavigationDrawer()
        }
    }

    private fun setupDashboardRecyclerView() {
        val metricsData = listOf(
            StatMetric("Total Sales", "₹ 45,670", "↑ 18.6%", true),
            StatMetric("Total Orders", "86", "↑ 12.4%", true),
            StatMetric("Active Orders", "24", "↑ 15.3%", true),
            StatMetric("Avg. Order Time", "18 mins", "↑ 2.2 mins", false)
        )

        val summaryData = DashboardSummary(
            newCount = "12",
            kitchenCount = "08",
            readyCount = "05",
            servedCount = "42",
            cancelledCount = "02"
        )

        val foodItemsList = listOf(
            TopSellingFoodItem("1", "Paneer Butter Masala", 120, "₹ 18,240", R.drawable.shield),
            TopSellingFoodItem("2", "Veg Biryani", 98, "₹ 14,700", R.drawable.shield),
            TopSellingFoodItem("3", "Chili Paneer", 80, "₹ 12,450", R.drawable.shield)
        )

        // Wired up Quick Action types mapping to IDs via your navigation helper
        val dashboardAdapter = ManagerDashboardAdapter(metricsData, summaryData, foodItemsList) { actionType ->
            val simulatedMenuId = when (actionType) {
                ManagerDashboardAdapter.QuickActionType.ADD_STAFF -> 5
                ManagerDashboardAdapter.QuickActionType.MENU -> 4
                ManagerDashboardAdapter.QuickActionType.FLOOR_TABLE -> 2
                ManagerDashboardAdapter.QuickActionType.ORDERS -> 1
                ManagerDashboardAdapter.QuickActionType.REPORTS -> 8
            }

            // Build temporary model instance package placeholder to trigger routing handler smoothly
            val simulatedItem = DrawerMenuItem(simulatedMenuId, "", 0, null)
            navigationHelper.handleNavigation(simulatedItem)
        }

        binding.mainDashboardRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dashboardAdapter
        }
    }

    /**
     * Finds the side navigation list inside the hosting Activity's layouts,
     * and sets its adapter to forward selections directly to the NavigationHelper.
     */
    private fun setupActivityDrawerRecyclerView() {
        val homeActivity = activity as? ManagerHomeActivity ?: return

        val dynamicDrawerOptions = listOf(
            DrawerMenuItem(0, "Dashboard", R.drawable.ic_dashboard_24dp, ManagerDashboardFragment::class.java),
            DrawerMenuItem(1, "Orders", R.drawable.bg_order_notes, StaffOrdersFragment::class.java),
            DrawerMenuItem(2, "Tables", R.drawable.ic_table_24dp, TableManagementFragment::class.java),
            DrawerMenuItem(3, "Kitchen", R.drawable.bg_order_notes, ManagerDashboardFragment::class.java),
            DrawerMenuItem(4, "Menu Management", R.drawable.biling, MenuManagementFragment::class.java),
            DrawerMenuItem(5, "Staff Management", R.drawable.ic_staffs_24dp, StaffManagementFragment::class.java),
            DrawerMenuItem(6, "Billing", R.drawable.biling, ManagerDashboardFragment::class.java),
            DrawerMenuItem(7, "Inventory", R.drawable.bg_order_notes, ManagerDashboardFragment::class.java),
            DrawerMenuItem(8, "Reports & Analytics", R.drawable.ic_sales_report_24dp, ManagerDashboardFragment::class.java),
            DrawerMenuItem(9, "Customers", R.drawable.ic_person_24dp, ManagerDashboardFragment::class.java),
            DrawerMenuItem(10, "Offers & Discounts", R.drawable.bg_order_notes, ManagerDashboardFragment::class.java),
            DrawerMenuItem(11, "Notifications", R.drawable.ic_notifications_24dp, ManagerDashboardFragment::class.java, badgeCount = 2),
            DrawerMenuItem(12, "Settings", R.drawable.ic_settings_24dp, ManagerDashboardFragment::class.java),
            DrawerMenuItem(13, "Logout", R.drawable.ic_inventory_24dp, null, isLogout = true)
        )

        val menuAdapter = DrawerMenuAdapter(dynamicDrawerOptions) { selectedMenu ->
            navigationHelper.handleNavigation(selectedMenu)
            homeActivity.binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Direct view accessor parsing onto the Activity layout components
        homeActivity.binding.navigationView.findViewById<RecyclerView>(R.id.rvDrawerMenu).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = menuAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}