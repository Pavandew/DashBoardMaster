package com.restroone.app.utils

import android.app.Activity
import com.restroone.app.R
import com.restroone.app.manager_single_res_dash.ManagerHomeActivity
import com.restroone.app.manager_single_res_dash.SingleResOwnerHomeActivity
import com.restroone.app.staff_dash.waiter_screens.WaiterHomeActivity
import com.restroone.app.staff_dash.kitchen_screens.KitchenHomeActivity
import com.restroone.app.staff_dash.billing_screens.CashierHomeActivity

/**
 * Centrally manages host container IDs to support multi-activity fragment hosting.
 * This prevents "No view found for id" crashes when a staff fragment is hosted 
 * within the Manager/Owner's Home activity.
 */
object NavigationUtils {
    fun getHostContainerId(activity: Activity?): Int {
        return when (activity) {
            is ManagerHomeActivity -> R.id.manager_fragmentContainer
            is SingleResOwnerHomeActivity -> R.id.single_owner_fragmentContainer
            is WaiterHomeActivity -> R.id.waiter_fragment_container
            is KitchenHomeActivity -> R.id.kitchen_fragment_container
            is CashierHomeActivity -> R.id.billing_fragment_container
            else -> 0
        }
    }
}
