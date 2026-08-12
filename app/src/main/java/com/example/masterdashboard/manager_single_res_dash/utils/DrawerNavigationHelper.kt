package com.example.masterdashboard.manager_single_res_dash.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.masterdashboard.R
import android.widget.TextView
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.login.utils.AppConstants
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
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.WaiterActiveOrdersFragment
import com.example.masterdashboard.login.utils.LogoutManager
import com.example.masterdashboard.staff_dash.billing_screens.views.CashierBillingFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenOrderFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.KitchenHomeActivity
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity

class DrawerNavigationHelper(private val fragment: Fragment) {

    companion object {
        private const val TAG = "DrawerNavigationHelper"
    }

    private val context: Context
        get() = fragment.requireContext()

    private val activity: FragmentActivity?
        get() = fragment.activity

    private val containerId: Int
        get() = when (activity) {
            is ManagerHomeActivity -> R.id.manager_fragmentContainer
            is SingleResOwnerHomeActivity -> R.id.single_owner_fragmentContainer
            else -> 0
        }

    //  Initialize universal helper class lazily using the contextual reference.
    // This deprecates the local duplicate SessionManager and FirebaseAuth calls safely.
    private val logoutManager by lazy { LogoutManager(context) }
    private val sessionManager by lazy { SessionManager(context) }

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
            1 -> WaiterActiveOrdersFragment()
            2 -> TableManagementFragment()
            3 -> KitchenOrderFragment()
            4 -> MenuManagementFragment()
            5 -> StaffManagementFragment()
            6 -> CashierBillingFragment()
            7 -> null
            8 -> null
            9 -> null
            10 -> null
            11 -> null
            12 -> null
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

        val dynamicDrawerOptions = listOf(
            DrawerMenuItem(0, "Dashboard", R.drawable.ic_dashboard_24dp, fragmentClass = ManagerDashboardFragment::class.java),
            DrawerMenuItem(6, "Billing Screen", R.drawable.biling, activityClass = CashierHomeActivity::class.java),
            DrawerMenuItem(1, "Take Orders", R.drawable.waiter, activityClass = WaiterHomeActivity::class.java),
            DrawerMenuItem(3, "Kitchen Screen", R.drawable.ic_chef_24dp, activityClass = KitchenHomeActivity::class.java),
            DrawerMenuItem(5, "Staff Management", R.drawable.ic_staffs_24dp, fragmentClass = StaffManagementFragment::class.java),
            DrawerMenuItem(4, "Menu Management", R.drawable.ic_menu_24dp, fragmentClass = MenuManagementFragment::class.java),
            DrawerMenuItem(2, "Table Management", R.drawable.ic_table_24dp, fragmentClass = TableManagementFragment::class.java),
            DrawerMenuItem(7, "Inventory", R.drawable.ic_inventory_24dp, fragmentClass = ManagerDashboardFragment::class.java),
            DrawerMenuItem(8, "Reports & Analytics", R.drawable.ic_sales_report_24dp, fragmentClass = ManagerDashboardFragment::class.java),
            DrawerMenuItem(9, "Customers", R.drawable.ic_person_24dp, fragmentClass = ManagerDashboardFragment::class.java),
            DrawerMenuItem(10, "Offers & Discounts", R.drawable.ic_discount_24dp, fragmentClass = ManagerDashboardFragment::class.java),
            DrawerMenuItem(11, "Notifications", R.drawable.ic_notifications_24dp, fragmentClass = ManagerDashboardFragment::class.java, badgeCount = 2),
            DrawerMenuItem(12, "Settings", R.drawable.ic_settings_24dp, fragmentClass = ManagerDashboardFragment::class.java),
            DrawerMenuItem(13, "Logout", R.drawable.ic_logout_24dp, isLogout = true)
        )

        val (drawerLayout, navigationView) = when (currentActivity) {
            is ManagerHomeActivity -> currentActivity.binding.drawerLayout to currentActivity.binding.navigationView
            is SingleResOwnerHomeActivity -> currentActivity.activityBinding.drawerLayout to currentActivity.activityBinding.navigationView
            else -> null to null
        }

        if (drawerLayout == null || navigationView == null) return

        val menuAdapter = DrawerMenuAdapter(dynamicDrawerOptions) { selectedMenu ->
            handleNavigation(selectedMenu)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        val rvMenu = navigationView.findViewById<RecyclerView>(R.id.rvDrawerMenu)
        rvMenu?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = menuAdapter
        }
    }
}

