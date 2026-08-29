package com.example.masterdashboard.manager_single_res_dash.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentManagerDashboardBinding
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.SingleResOwnerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.adapter.ManagerDashboardAdapter
import com.example.masterdashboard.manager_single_res_dash.models.DrawerMenuItem
import com.example.masterdashboard.manager_single_res_dash.models.StatMetric
import com.example.masterdashboard.manager_single_res_dash.models.TopSellingFoodItem
import com.example.masterdashboard.manager_single_res_dash.utils.DrawerNavigationHelper
import com.example.masterdashboard.manager_single_res_dash.viewModel.ManagerDashboardViewModel
import com.example.masterdashboard.manager_single_res_dash.views.MenuManagementFragment
import com.example.masterdashboard.manager_single_res_dash.views.StaffManagementFragment
import com.example.masterdashboard.manager_single_res_dash.views.TableManagementFragment
import com.example.masterdashboard.manager_single_res_dash.views.CustomerManagementFragment
import com.example.masterdashboard.staff_dash.billing_screens.views.CashierBillingFragment
import com.example.masterdashboard.staff_dash.billing_screens.views.CashierOrderFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenOrderFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenPreparationFragment
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.WaiterActiveOrdersFragment
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.WaiterTablesFragment
import kotlinx.coroutines.launch
import kotlin.getValue

class ManagerDashboardFragment : Fragment() {

    private var _binding: FragmentManagerDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var navigationHelper: DrawerNavigationHelper
    private lateinit var sessionManager: SessionManager
    private val viewModel: ManagerDashboardViewModel by viewModels()
    private lateinit var dashboardAdapter: ManagerDashboardAdapter

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
        sessionManager = SessionManager(requireContext())
        
        // 1. Initialize central navigation engine helper
        navigationHelper = DrawerNavigationHelper(this)

        configureHeaderNavigationAndProfile()
        initializeMainDashboardAdapter()
        subscribeToDashboardData()
        
        navigationHelper.initDrawerMenu()
        
        // 3. Start real-time order status tracking
        val managerId = sessionManager.getUid()
        viewModel.startRealTimeOrderStatusTracking(managerId)
        viewModel.loadRestaurantDetails(managerId, sessionManager)
        
        // 4. Update Drawer Header immediately with available User Info
        navigationHelper.updateDrawerHeader()
    }

    /**
     * Binds UI components to the ViewModel's data flows for reactive dashboard updates.
     */
    private fun subscribeToDashboardData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Order Status Summary
                launch {
                    viewModel.orderStatusSummary.collect { summary ->
                        Log.d("ManagerDashboard", "UI Update: Order status counts refreshed")
                        dashboardAdapter.updateData(
                            newMetrics = getDummyMetrics(), 
                            newSummary = summary,
                            newTopSelling = getDummyTopSelling(),
                            isExpanded = viewModel.isQuickActionsExpanded.value
                        )
                    }
                }

                // Observe Quick Actions Expansion state
                launch {
                    viewModel.isQuickActionsExpanded.collect { isExpanded ->
                        dashboardAdapter.updateData(
                            newMetrics = getDummyMetrics(),
                            newSummary = viewModel.orderStatusSummary.value,
                            newTopSelling = getDummyTopSelling(),
                            isExpanded = isExpanded
                        )
                    }
                }
                
                // Observe Restaurant Name
                launch {
                    viewModel.restaurantName.collect { name ->
                        if (name.isNotEmpty()) {
                            Log.d("ManagerDashboard", "UI Update: Restaurant name updated: $name")
                            binding.masterDashHeader.txtRestaurantName.text = "$name ▾"
                            navigationHelper.updateDrawerHeader(name)
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets up the dashboard greeting, navigation drawer triggers, and restaurant profile details.
     */
    private fun configureHeaderNavigationAndProfile() {
        val header = binding.masterDashHeader
        
        // 1. Set personalized greeting
        val userName = sessionManager.getUserName() ?: "Manager"
        header.txtGreeting.text = "Good Morning, $userName 👋"

        header.btnDrawerMenu.setOnClickListener {
            Log.d("ManagerDashboard", "Action: Drawer Menu button clicked")
            when (val currentActivity = activity) {
                is ManagerHomeActivity -> currentActivity.openNavigationDrawer()
                is SingleResOwnerHomeActivity -> currentActivity.openNavigationDrawer()
            }
        }
        
        // 2. Set up click listeners for the Bottom Sheet
        val clickListener = View.OnClickListener {
            Log.i("ManagerDashboard", "Action: Restaurant Name/Profile clicked - Opening Bottom Sheet")
            val ownerUid = sessionManager.getUid() 
            if (ownerUid.isNotEmpty()) {
                // 🛡️ Guard against double-tap: only show if not already showing
                if (childFragmentManager.findFragmentByTag("ResDetails") == null) {
                    RestaurantDetailsBottomSheet.newInstance(ownerUid).show(childFragmentManager, "ResDetails")
                }
            } else {
                Log.w("ManagerDashboard", "Warning: Cannot open bottom sheet, User ID is empty")
            }
        }
        
        header.txtRestaurantName.setOnClickListener(clickListener)
        header.imgProfile.setOnClickListener(clickListener)
    }

    /**
     * Configures the main RecyclerView and its multi-view adapter.
     */
    private fun initializeMainDashboardAdapter() {
        // Initial setup with current (empty/default) summary
        dashboardAdapter = ManagerDashboardAdapter(
            getDummyMetrics(), 
            viewModel.orderStatusSummary.value, 
            getDummyTopSelling(),
            viewModel.isQuickActionsExpanded.value,
            onQuickActionClicked = { actionType ->
                val simulatedItem = when (actionType) {
                    ManagerDashboardAdapter.QuickActionType.WAITER_TABLES -> 
                        DrawerMenuItem(1, "Manage Floor Tables", 0, fragmentClass = WaiterTablesFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.WAITER_ORDERS -> 
                        DrawerMenuItem(2, "Track Active Orders", 0, fragmentClass = WaiterActiveOrdersFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.KITCHEN_ORDERS -> 
                        DrawerMenuItem(4, "Live Order Station", 0, fragmentClass = KitchenOrderFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.KITCHEN_PREP -> 
                        DrawerMenuItem(5, "Cooking Workstation", 0, fragmentClass = KitchenPreparationFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.BILLING_MAIN -> 
                        DrawerMenuItem(7, "Settlement Center", 0, fragmentClass = CashierBillingFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.BILLING_ORDERS -> 
                        DrawerMenuItem(8, "Quick Bill Counter", 0, fragmentClass = CashierOrderFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.ADD_STAFF -> 
                        DrawerMenuItem(10, "Staff Management", 0, fragmentClass = StaffManagementFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.MENU -> 
                        DrawerMenuItem(9, "Menu Management", 0, fragmentClass = MenuManagementFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.FLOOR_TABLE ->
                        DrawerMenuItem(3, "Table Management", 0, fragmentClass = TableManagementFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.REPORTS -> 
                        DrawerMenuItem(11, "Reports & Analytics", 0, fragmentClass = ManagerDashboardFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.CUSTOMERS -> 
                        DrawerMenuItem(12, "Customer Insights", 0, fragmentClass = CustomerManagementFragment::class.java)
                }

                navigationHelper.handleNavigation(simulatedItem)
            },
            onToggleQuickActions = {
                viewModel.toggleQuickActionsExpanded()
            },
            onSummaryClicked = { status ->
                val simulatedItem = when (status) {
                    "KITCHEN" -> 
                        DrawerMenuItem(4, "Kitchen Screen", 0, fragmentClass = KitchenOrderFragment::class.java)
                    "READY" -> 
                        DrawerMenuItem(2, "Waiter Orders", 0, fragmentClass = WaiterActiveOrdersFragment::class.java)
                    "SERVED" -> 
                        DrawerMenuItem(7, "Billing Screen", 0, fragmentClass = CashierBillingFragment::class.java)
                    else -> 
                        DrawerMenuItem(1, "Take Orders", 0, fragmentClass = WaiterTablesFragment::class.java)
                }
                navigationHelper.handleNavigation(simulatedItem)
            }
        )

        binding.mainDashboardRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dashboardAdapter
        }
    }

    private fun getDummyMetrics(): List<StatMetric> {
        return listOf(
            StatMetric("Total Sales", "₹ 45,670", "↑ 18.6%", true),
            StatMetric("Total Orders", "86", "↑ 12.4%", true),
            StatMetric("Active Orders", "24", "↑ 15.3%", true),
            StatMetric("Avg. Order Time", "18 mins", "↑ 2.2 mins", false)
        )
    }

    private fun getDummyTopSelling(): List<TopSellingFoodItem> {
        return listOf(
            TopSellingFoodItem("1", "Paneer Butter Masala", 120, "₹ 18,240", R.drawable.shield),
            TopSellingFoodItem("2", "Veg Biryani", 98, "₹ 14,700", R.drawable.shield),
            TopSellingFoodItem("3", "Chili Paneer", 80, "₹ 12,450", R.drawable.shield)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navigationHelper.destroy()
        _binding = null
    }
}
