package com.example.masterdashboard.manager_single_res_dash.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.masterdashboard.R
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.models.DrawerMenuItem
import com.example.masterdashboard.manager_single_res_dash.views.ManagerDashboardFragment
import com.example.masterdashboard.manager_single_res_dash.views.MenuManagementFragment
import com.example.masterdashboard.manager_single_res_dash.views.StaffManagementFragment
import com.example.masterdashboard.manager_single_res_dash.views.TableManagementFragment
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.WaiterActiveOrdersFragment
import com.example.masterdashboard.login.utils.LogoutManager

class DrawerNavigationHelper(private val fragment: Fragment) {

    companion object {
        private const val TAG = "DrawerNavigationHelper"
    }

    private val context: Context
        get() = fragment.requireContext()

    private val activity: ManagerHomeActivity?
        get() = fragment.activity as? ManagerHomeActivity

    //  Initialize universal helper class lazily using the contextual reference.
    // This deprecates the local duplicate SessionManager and FirebaseAuth calls safely.
    private val logoutManager by lazy { LogoutManager(context) }

    /**
     * Handles routing for any given sidebar drawer item click event
     */
    fun handleNavigation(item: DrawerMenuItem) {
        if (item.isLogout) {
            // Displays popup modal and clears storage instantly on click.
            logoutManager.showLogoutConfirmation()
            return
        }

        // Check if the clicked item has a class defined directly on its model object wrapper
        item.fragmentClass?.let { fragmentClass ->
            val currentFragment = activity?.supportFragmentManager?.findFragmentById(R.id.manager_fragmentContainer)

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
            3 -> null
            4 -> MenuManagementFragment()
            5 -> StaffManagementFragment()
            6 -> null
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
        Log.i(TAG, "Navigating to Fragment: ${targetFragment.javaClass.simpleName}")

        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.manager_fragmentContainer, targetFragment)
            addToBackStack(null)
            commit()
        }
    }
}