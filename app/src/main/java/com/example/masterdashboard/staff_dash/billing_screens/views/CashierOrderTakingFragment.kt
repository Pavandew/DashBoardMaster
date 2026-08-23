package com.example.masterdashboard.staff_dash.billing_screens.views

import android.os.Bundle
import android.util.Log
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.BaseOrderTakingFragment
import com.example.masterdashboard.utils.NavigationUtils

/**
 * Specialized fragment for Cashiers. Handles navigation to the Cashier's payment flow.
 */
class CashierOrderTakingFragment : BaseOrderTakingFragment() {

    companion object {
        private const val TAG = "CashierOrderTaking"
    }

    override fun onStart() {
        super.onStart()
        (activity as? CashierHomeActivity)?.hideBottomNavigation()
    }

    override fun onCartClicked() {
        Log.i(TAG, "Cashier proceeding to payment details.")
        val bundle = Bundle().apply {
            putString("tableId", arguments?.getString("tableId"))
            putString("tableName", arguments?.getString("tableName"))
            putString("floorId", arguments?.getString("floorId"))
            putString("status", arguments?.getString("status"))
        }

        // Navigate to the Cashier-specific cart/payment view
        val paymentFragment = CashierViewCartDetailsFragment().apply { arguments = bundle }
        val containerId = NavigationUtils.getHostContainerId(activity)
        if (containerId != 0) {
            parentFragmentManager.beginTransaction()
                .replace(containerId, paymentFragment)
                .addToBackStack(null).commit()
        }
    }
}