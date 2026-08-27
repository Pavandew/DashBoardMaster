package com.example.masterdashboard.manager_single_res_dash.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.masterdashboard.R
import android.widget.TextView
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.SingleResOwnerHomeActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.manager_single_res_dash.adapter.DrawerMenuAdapter
import com.example.masterdashboard.manager_single_res_dash.models.DrawerMenuItem
import com.example.masterdashboard.manager_single_res_dash.views.*
import com.example.masterdashboard.manager_single_res_dash.views.MenuManagementFragment
import com.example.masterdashboard.manager_single_res_dash.views.StaffManagementFragment
import com.example.masterdashboard.manager_single_res_dash.views.TableManagementFragment
import com.example.masterdashboard.manager_single_res_dash.views.CustomerManagementFragment
import com.example.masterdashboard.notifications.alert.NotificationFragment
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.WaiterActiveOrdersFragment
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.WaiterTablesFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenInventoryFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenPreparationFragment
import com.example.masterdashboard.utils.LogoutManager
import com.example.masterdashboard.staff_dash.billing_screens.views.CashierBillingFragment
import com.example.masterdashboard.staff_dash.billing_screens.views.CashierOrderFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenOrderFragment
import com.example.masterdashboard.utils.NavigationUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class DrawerNavigationHelper(private val fragment: Fragment) {

    companion object {
        private const val TAG = "DrawerNavigationHelper"
    }

    private val context: Context
        get() = fragment.requireContext()

    private val activity: FragmentActivity?
        get() = fragment.activity

    private val containerId: Int
        get() = NavigationUtils.getHostContainerId(activity)

    //  Initialize universal helper class lazily using the contextual reference.
    // This deprecates the local duplicate SessionManager and FirebaseAuth calls safely.
    private val logoutManager by lazy { LogoutManager(context) }
    private val sessionManager by lazy { SessionManager(context) }

    private var menuAdapter: DrawerMenuAdapter? = null
    private var unreadListener: ListenerRegistration? = null
    private var menuItems: MutableList<DrawerMenuItem> = mutableListOf()

    /**
     * Handles routing for any given sidebar drawer item click event
     */
    fun handleNavigation(item: DrawerMenuItem) {
        if (item.isLogout) {
            // Displays popup modal and clears storage instantly on click.
            logoutManager.showLogoutConfirmation()
            return
        }

        // NEW: Handle Activity Navigation (Show "all things" like bottom nav)
        item.activityClass?.let { activityClass ->
            Log.i(TAG, "Navigating to Activity: ${activityClass.simpleName}")
            
            // FIX: If we open a new Activity, reset drawer selection in current activity to 'Dashboard' (ID 0)
            // so when user comes back, it's correct.
            menuAdapter?.setSelectedItem(0)
            
            val intent = Intent(context, activityClass)
            context.startActivity(intent)
            return
        }
        
        val targetContainer = containerId
        if (targetContainer == 0) {
            Log.e(TAG, "Error: Unknown activity container for navigation")
            return
        }

        // Check if the clicked item has a class defined directly on its model object wrapper
        item.fragmentClass?.let { fragmentClass ->
            val currentFragment = activity?.supportFragmentManager?.findFragmentById(targetContainer)

            // Safety guard: Do nothing if the user is clicking on the already active screen section
            if (currentFragment != null && currentFragment::class.java == fragmentClass) {
                return
            }

            try {
                val instantiatedFragment = fragmentClass.getDeclaredConstructor().newInstance()
                switchScreen(instantiatedFragment)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to instantiate fragment class: ${fragmentClass.simpleName}", e)
                Toast.makeText(context, "Error opening ${item.title}", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Fallback structural router matching your explicit index IDs sequence setup map fallback
        val destinationFragment: Fragment? = when (item.id) {
            0 -> ManagerDashboardFragment()
            1 -> WaiterTablesFragment()
            2 -> WaiterActiveOrdersFragment()
            3 -> TableManagementFragment()
            4 -> KitchenOrderFragment()
            5 -> KitchenPreparationFragment()
            6 -> KitchenInventoryFragment()
            7 -> CashierBillingFragment()
            8 -> CashierOrderFragment()
            9 -> MenuManagementFragment()
            10 -> StaffManagementFragment()
            11 -> ManagerDashboardFragment() // Reports
            12 -> CustomerManagementFragment()
            13 -> ManagerDashboardFragment() // Offers
            14 -> NotificationFragment()
            15 -> ManagerDashboardFragment() // Settings
            else -> null
        }

        if (destinationFragment != null) {
            switchScreen(destinationFragment)
        } else {
            Toast.makeText(context, "${item.title} screen coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchScreen(targetFragment: Fragment) {
        val targetContainer = containerId
        if (targetContainer == 0) return

        Log.i(TAG, "Navigating to Fragment: ${targetFragment.javaClass.simpleName}")

        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(targetContainer, targetFragment)
            addToBackStack(null)
            commit()
        }
    }

    /**
     * Updates the Navigation Drawer profile header with current user session data.
     */
    fun updateDrawerHeader(restaurantName: String? = null) {
        val currentActivity = activity ?: return
        val navigationView = when (currentActivity) {
            is ManagerHomeActivity -> currentActivity.binding.navigationView
            is SingleResOwnerHomeActivity -> currentActivity.activityBinding.navigationView
            else -> null
        } ?: return

        val nameTv = navigationView.findViewById<TextView>(R.id.drawerProfileName)
        val roleTv = navigationView.findViewById<TextView>(R.id.drawerProfileRole)

        val fullName = sessionManager.getUserName() ?: "User"

        val displayRole = when (val rawRole = sessionManager.getRole()) {
            AppConstants.ROLE_OWNER_SINGLE -> "Owner"
            AppConstants.ROLE_MANAGER -> "Manager"
            else -> rawRole.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }

        nameTv?.text = fullName

        if (restaurantName != null) {
            roleTv?.text = "$displayRole • $restaurantName"
        } else {
            // Initial load: show role, restaurant name will update after fetchRestaurantName()
            roleTv?.text = displayRole
        }
    }

    /**
     * Initializes the side navigation drawer with dynamic menu items.
     */
    fun initDrawerMenu() {
        val currentActivity = activity ?: return

        menuItems = mutableListOf(
            DrawerMenuItem(0, "Dashboard", R.drawable.ic_dashboard_24dp, fragmentClass = ManagerDashboardFragment::class.java),
            DrawerMenuItem(7, "Billing Screen", R.drawable.biling, fragmentClass = CashierBillingFragment::class.java),
            DrawerMenuItem(1, "Take Orders", R.drawable.waiter, fragmentClass = WaiterTablesFragment::class.java),
            DrawerMenuItem(4, "Kitchen Screen", R.drawable.ic_chef_24dp, fragmentClass = KitchenOrderFragment::class.java),
            DrawerMenuItem(10, "Staff Management", R.drawable.ic_staffs_24dp, fragmentClass = StaffManagementFragment::class.java),
            DrawerMenuItem(9, "Menu Management", R.drawable.ic_menu_24dp, fragmentClass = MenuManagementFragment::class.java),
            DrawerMenuItem(3, "Table Management", R.drawable.ic_table_24dp, fragmentClass = TableManagementFragment::class.java),
            DrawerMenuItem(6, "Inventory", R.drawable.ic_inventory_24dp, fragmentClass = KitchenInventoryFragment::class.java),
            DrawerMenuItem(11, "Reports & Analytics", R.drawable.ic_sales_report_24dp, fragmentClass = ManagerDashboardFragment::class.java),
            DrawerMenuItem(12, "Customers", R.drawable.ic_person_24dp, fragmentClass = CustomerManagementFragment::class.java),
            DrawerMenuItem(13, "Offers & Discounts", R.drawable.ic_discount_24dp, fragmentClass = ManagerDashboardFragment::class.java),
            DrawerMenuItem(14, "Notifications", R.drawable.ic_notifications_24dp, fragmentClass = NotificationFragment::class.java),
            DrawerMenuItem(15, "Settings", R.drawable.ic_settings_24dp, fragmentClass = ManagerDashboardFragment::class.java),
            DrawerMenuItem(16, "Logout", R.drawable.ic_logout_24dp, isLogout = true)
        )

        val (drawerLayout, navigationView) = when (currentActivity) {
            is ManagerHomeActivity -> currentActivity.binding.drawerLayout to currentActivity.binding.navigationView
            is SingleResOwnerHomeActivity -> currentActivity.activityBinding.drawerLayout to currentActivity.activityBinding.navigationView
            else -> null to null
        }

        if (drawerLayout == null || navigationView == null) return

        val adapter = DrawerMenuAdapter(menuItems) { selectedMenu ->
            handleNavigation(selectedMenu)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        this.menuAdapter = adapter

        val rvMenu = navigationView.findViewById<RecyclerView>(R.id.rvDrawerMenu)
        rvMenu?.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        startUnreadCountListener()
    }

    private fun startUnreadCountListener() {
        val managerId = sessionManager.getUid()
        val userRole = sessionManager.getRole().lowercase().trim()
        val staffId = sessionManager.getStaffDocId()

        if (managerId.isEmpty()) {
            Log.w(TAG, "Skipping unread listener because managerId is empty")
            return
        }

        // Build role-based filter list consistent with NotificationRepository
        val isManager = userRole == "manager" || userRole == "owner_single" || userRole == "owner_multi"
        val roleTargets = mutableListOf("all", userRole)
        when (userRole) {
            "waiter", "waiter_staff" -> roleTargets.addAll(listOf("waiter", "waiter_staff"))
            "chef", "kitchen" -> roleTargets.addAll(listOf("chef", "kitchen"))
            "cashier", "billing" -> roleTargets.addAll(listOf("cashier", "billing"))
        }
        val distinctRoleTargets = roleTargets.distinct()

        // Apply server-side filtering for efficiency
        val targets = if (isManager) {
            listOf("all", "manager")
        } else {
            distinctRoleTargets
        }

        Log.d(TAG, "Starting filtered unread badge listener for managerId: $managerId, role: $userRole, targets: $targets")
        unreadListener?.remove()

        val query = FirebaseFirestore.getInstance()
            .collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_NOTIFICATIONS)
            .whereEqualTo(AppConstants.FIELD_IS_READ, false)
            .whereIn(AppConstants.FIELD_TARGET_ROLE, targets)

        unreadListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error in unread badge listener", error)
                return@addSnapshotListener
            }

            val unreadCount = snapshot?.mapNotNull { doc ->
                val tStaffId = doc.getString(AppConstants.FIELD_TARGET_STAFF_ID) ?: ""
                
                // Local filtering for Staff-specific alerts (e.g. alerts sent to a specific waiter ID)
                val isRelevant = when {
                    isManager -> true
                    tStaffId.isNotEmpty() -> tStaffId == staffId
                    else -> true
                }
                
                if (isRelevant) 1 else null
            }?.size ?: 0

            Log.d(TAG, "Unread notifications update (filtered): count=$unreadCount")
            updateBadgeCount(14, unreadCount)
        }
    }

    private fun updateBadgeCount(itemId: Int, count: Int) {
        val index = menuItems.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val oldItem = menuItems[index]
            if (oldItem.badgeCount != count) {
                menuItems[index] = oldItem.copy(badgeCount = count)
                menuAdapter?.notifyItemChanged(index)
            }
        }
    }

    /**
     * Call this from Fragment.onDestroyView() to stop listeners
     */
    fun destroy() {
        unreadListener?.remove()
        unreadListener = null
    }
}
