package com.example.masterdashboard.staff_dash.waiter_screens.table.views

import android.os.Bundle
import android.util.Log
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.utils.NavigationUtils

/**
 * Specialized fragment for Waiters. Handles navigation to the Waiter's cart.
 */
class WaiterOrderTakingFragment : BaseOrderTakingFragment() {

    companion object {
        private const val TAG = "WaiterOrderTaking"
    }

    override fun onStart() {
        super.onStart()
        (activity as? WaiterHomeActivity)?.hideBottomNavigation()
    }

    override fun onCartClicked() {
        Log.i(TAG, "Waiter viewing cart details.")
        val bundle = Bundle().apply {
            putString("tableId", arguments?.getString("tableId"))
            putString("tableName", arguments?.getString("tableName"))
            putString("floorId", arguments?.getString("floorId"))
            putString("status", arguments?.getString("status"))
        }

        // Navigate to the Waiter-specific cart view
        val cartFragment = WaiterViewCartDetailsFragment().apply { arguments = bundle }
        val containerId = NavigationUtils.getHostContainerId(activity)
        if (containerId != 0) {
            parentFragmentManager.beginTransaction()
                .replace(containerId, cartFragment)
                .addToBackStack(null).commit()
        }
    }
}