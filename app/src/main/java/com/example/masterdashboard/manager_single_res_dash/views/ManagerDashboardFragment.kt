package com.example.masterdashboard.manager_single_res_dash.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentManagerDashboardBinding
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.SingleResOwnerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.adapter.ManagerDashboardAdapter
import com.example.masterdashboard.manager_single_res_dash.models.DashboardSummary
import com.example.masterdashboard.manager_single_res_dash.models.DrawerMenuItem
import com.example.masterdashboard.manager_single_res_dash.models.StatMetric
import com.example.masterdashboard.manager_single_res_dash.models.TopSellingFoodItem
import com.example.masterdashboard.manager_single_res_dash.utils.DrawerNavigationHelper
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.kitchen_screens.KitchenHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ManagerDashboardFragment : Fragment() {

    private var _binding: FragmentManagerDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var navigationHelper: DrawerNavigationHelper
    private lateinit var sessionManager: SessionManager

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

        setupHeader()
        setupDashboardRecyclerView()
        navigationHelper.initDrawerMenu()
        
        // 3. Update Drawer Header immediately with available User Info
        navigationHelper.updateDrawerHeader()
    }

    private fun setupHeader() {
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
            val ownerUid = sessionManager.getUid() // Use UID as the key now
            if (ownerUid.isNotEmpty()) {
                RestaurantDetailsBottomSheet.newInstance(ownerUid).show(childFragmentManager, "ResDetails")
            } else {
                Log.w("ManagerDashboard", "Warning: Cannot open bottom sheet, User ID is empty")
            }
        }
        
        header.txtRestaurantName.setOnClickListener(clickListener)
        header.imgProfile.setOnClickListener(clickListener)
        
        // Fetch and show current restaurant name
        fetchRestaurantName()
    }

    private fun fetchRestaurantName() {
        val ownerUid = sessionManager.getUid()
        if (ownerUid.isEmpty()) return

        lifecycleScope.launch {
            try {
                Log.d("ManagerDashboard", "Fetching restaurant name from User document: $ownerUid")
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection(AppConstants.COLLECTION_USERS).document(ownerUid).get().await()
                val name = doc.getString(AppConstants.FIELD_RESTAURANT_NAME) ?: "My Restaurant"
                
                binding.masterDashHeader.txtRestaurantName.text = "$name ▾"
                
                // Update Drawer Header with the fetched restaurant name
                navigationHelper.updateDrawerHeader(name)
                
            } catch (e: Exception) {
                Log.e("ManagerDashboard", "Error fetching restaurant name from Firebase", e)
            }
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
        val dashboardAdapter = ManagerDashboardAdapter(
            metricsData, 
            summaryData, 
            foodItemsList,
            onQuickActionClicked = { actionType ->
                val simulatedItem = when (actionType) {
                    ManagerDashboardAdapter.QuickActionType.WAITER -> 
                        DrawerMenuItem(1, "Take Orders", 0, activityClass = WaiterHomeActivity::class.java)
                    ManagerDashboardAdapter.QuickActionType.KITCHEN -> 
                        DrawerMenuItem(3, "Kitchen Screen", 0, activityClass = KitchenHomeActivity::class.java)
                    ManagerDashboardAdapter.QuickActionType.BILLING -> 
                        DrawerMenuItem(6, "Billing Screen", 0, activityClass = CashierHomeActivity::class.java)
                    ManagerDashboardAdapter.QuickActionType.ADD_STAFF -> 
                        DrawerMenuItem(5, "Staff Management", 0, fragmentClass = StaffManagementFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.MENU -> 
                        DrawerMenuItem(4, "Menu Management", 0, fragmentClass = MenuManagementFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.FLOOR_TABLE -> 
                        DrawerMenuItem(2, "Table Management", 0, fragmentClass = TableManagementFragment::class.java)
                    ManagerDashboardAdapter.QuickActionType.REPORTS -> 
                        DrawerMenuItem(8, "Reports & Analytics", 0, fragmentClass = ManagerDashboardFragment::class.java)
                }

                navigationHelper.handleNavigation(simulatedItem)
            },
            onSummaryClicked = { status ->
                val simulatedItem = when (status) {
                    "KITCHEN" -> 
                        DrawerMenuItem(3, "Kitchen Screen", 0, activityClass = KitchenHomeActivity::class.java)
                    "READY", "SERVED" -> 
                        DrawerMenuItem(6, "Billing Screen", 0, activityClass = CashierHomeActivity::class.java)
                    else -> 
                        DrawerMenuItem(1, "Take Orders", 0, activityClass = WaiterHomeActivity::class.java)
                }
                navigationHelper.handleNavigation(simulatedItem)
            }
        )

        binding.mainDashboardRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dashboardAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navigationHelper.destroy()
        _binding = null
    }
}
