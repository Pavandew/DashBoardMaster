package com.example.masterdashboard.manager_single_res_dash.home.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.masterdashboard.ActivityVisitorPortal
import com.example.masterdashboard.R
import com.example.masterdashboard.manager_single_res_dash.home.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.home.models.DrawerMenuItem
import com.example.masterdashboard.manager_single_res_dash.home.views.ManagerDashboardFragment
import com.example.masterdashboard.manager_single_res_dash.home.views.MenuManagementFragment
import com.example.masterdashboard.manager_single_res_dash.home.views.StaffManagementFragment
import com.example.masterdashboard.manager_single_res_dash.home.views.TableManagementFragment
import com.example.masterdashboard.staff_dash.home.order.views.StaffOrdersFragment
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class NavigationHelper(private val fragment: Fragment) {

    companion object {
        private const val TAG = "NavigationHelper"
    }

    private val context: Context
        get() = fragment.requireContext()

    private val activity: ManagerHomeActivity?
        get() = fragment.activity as? ManagerHomeActivity

    private val sessionManager = SessionManager(context)

    /**
     * Handles routing for any given sidebar drawer item click event
     */
    fun handleNavigation(item: DrawerMenuItem) {
        if (item.isLogout) {
            showLogoutDialog()
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
                Log.e(TAG, "Failed to instantiate fragment class: ${fragmentClass.simpleName}", e) // Critical Error Log
                Toast.makeText(context, "Error opening ${item.title}", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Fallback structural router matching your explicit index IDs sequence setup map fallback
        val destinationFragment: Fragment? = when (item.id) {
            0 -> ManagerDashboardFragment()
            1 -> StaffOrdersFragment()
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
        // Log screen switches so you can track user journey in logcat
        Log.i(TAG, "Navigating to Fragment: ${targetFragment.javaClass.simpleName}")

        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.manager_fragmentContainer, targetFragment)
            addToBackStack(null)
            commit()
        }
    }

    private fun showLogoutDialog() {
        activity?.let { hostingActivity ->
            MaterialAlertDialogBuilder(hostingActivity)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()
                    performFirebaseLogout()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun performFirebaseLogout() {
        Log.i(TAG, "User confirmed logout. Clearing cloud and local sessions.") // Critical Process Log

        // 1. Invalidate Firebase Auth cloud access authorization token
        FirebaseAuth.getInstance().signOut()

        // 2. Clear user session credentials within local SharedPreferences
        sessionManager.logout()

        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // 3. Clear activity back stack and return the app to ActivityVisitorPortal
        val intent = Intent(context, ActivityVisitorPortal::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)

        // 4. Terminate host activity instance loop lifecycle safely
        activity?.finish()
    }
}