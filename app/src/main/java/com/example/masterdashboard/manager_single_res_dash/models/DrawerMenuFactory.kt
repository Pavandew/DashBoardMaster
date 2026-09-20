package com.example.masterdashboard.manager_single_res_dash.models

import com.example.masterdashboard.R
import com.example.masterdashboard.manager_single_res_dash.settings.ManagerSettingsFragment
import com.example.masterdashboard.manager_single_res_dash.views.CustomerManagementFragment
import com.example.masterdashboard.manager_single_res_dash.views.ManagerDashboardFragment
import com.example.masterdashboard.manager_single_res_dash.views.MenuManagementFragment
import com.example.masterdashboard.manager_single_res_dash.views.ReportsAnalyticsFragment
import com.example.masterdashboard.manager_single_res_dash.views.StaffManagementFragment
import com.example.masterdashboard.manager_single_res_dash.views.TableManagementFragment
import com.example.masterdashboard.notifications.alert.NotificationFragment
import com.example.masterdashboard.staff_dash.billing_screens.views.CashierBillingFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenInventoryFragment
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenOrderFragment
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.WaiterTablesFragment
import com.example.masterdashboard.subscription.views.SubscriptionPlansFragment

object DrawerMenuFactory {

    fun getMenuItems(unreadCount: Int = 0): List<DrawerMenuItem> {
        return listOf(
            // 1. Dashboard (Single item)
            DrawerMenuItem(
                id = 0,
                title = "Dashboard",
                iconRes = R.drawable.ic_dashboard_24dp,
                fragmentClass = ManagerDashboardFragment::class.java,
                isHeader = false
            ),

            // 2. Floor & Orders (Parent Header)
            DrawerMenuItem(
                id = 100,
                title = "Floor & Orders",
                iconRes = R.drawable.ic_table_24dp,
                isHeader = true,
                isExpanded = true,
                children = listOf(
                    DrawerMenuItem(
                        id = 1,
                        title = "Take Orders",
                        iconRes = R.drawable.waiter,
                        fragmentClass = WaiterTablesFragment::class.java,
                        parentHeaderId = 100
                    ),
                    DrawerMenuItem(
                        id = 3,
                        title = "Table Management",
                        iconRes = R.drawable.ic_table_24dp,
                        fragmentClass = TableManagementFragment::class.java,
                        parentHeaderId = 100
                    )
                )
            ),

            // 3. Kitchen & Cooking (Parent Header)
            DrawerMenuItem(
                id = 200,
                title = "Kitchen & Cooking",
                iconRes = R.drawable.ic_chef_24dp,
                isHeader = true,
                children = listOf(
                    DrawerMenuItem(
                        id = 4,
                        title = "Kitchen Screen",
                        iconRes = R.drawable.ic_chef_24dp,
                        fragmentClass = KitchenOrderFragment::class.java,
                        parentHeaderId = 200
                    ),
                    DrawerMenuItem(
                        id = 6,
                        title = "Inventory",
                        iconRes = R.drawable.ic_inventory_24dp,
                        fragmentClass = KitchenInventoryFragment::class.java,
                        parentHeaderId = 200
                    )
                )
            ),

            // 4. Billing & Cashier (Parent Header)
            DrawerMenuItem(
                id = 300,
                title = "Billing & Cashier",
                iconRes = R.drawable.biling,
                isHeader = true,
                children = listOf(
                    DrawerMenuItem(
                        id = 7,
                        title = "Billing Screen",
                        iconRes = R.drawable.biling,
                        fragmentClass = CashierBillingFragment::class.java,
                        parentHeaderId = 300
                    )
                )
            ),

            // 5. Management & Reports (Parent Header)
            DrawerMenuItem(
                id = 400,
                title = "Management & Reports",
                iconRes = R.drawable.ic_settings_24dp,
                isHeader = true,
                children = listOf(
                    DrawerMenuItem(
                        id = 9,
                        title = "Menu Management",
                        iconRes = R.drawable.ic_menu_24dp,
                        fragmentClass = MenuManagementFragment::class.java,
                        parentHeaderId = 400
                    ),
                    DrawerMenuItem(
                        id = 10,
                        title = "Staff Management",
                        iconRes = R.drawable.ic_staffs_24dp,
                        fragmentClass = StaffManagementFragment::class.java,
                        parentHeaderId = 400
                    ),
                    DrawerMenuItem(
                        id = 11,
                        title = "Reports & Analytics",
                        iconRes = R.drawable.ic_sales_report_24dp,
                        fragmentClass = ReportsAnalyticsFragment::class.java,
                        parentHeaderId = 400
                    ),
                    DrawerMenuItem(
                        id = 12,
                        title = "Customers",
                        iconRes = R.drawable.ic_person_24dp,
                        fragmentClass = CustomerManagementFragment::class.java,
                        parentHeaderId = 400
                    ),
                    DrawerMenuItem(
                        id = 13,
                        title = "Offers & Discounts",
                        iconRes = R.drawable.ic_discount_24dp,
                        fragmentClass = null,
                        parentHeaderId = 400
                    )
                )
            ),

            // 6. Account & Settings (Parent Header)
            DrawerMenuItem(
                id = 500,
                title = "Account & Settings",
                iconRes = R.drawable.ic_settings_24dp,
                isHeader = true,
                children = listOf(
                    DrawerMenuItem(
                        id = 17,
                        title = "Subscription & Plans",
                        iconRes = R.drawable.ic_card_payment_24dp,
                        fragmentClass = SubscriptionPlansFragment::class.java,
                        parentHeaderId = 500
                    ),
                    DrawerMenuItem(
                        id = 14,
                        title = "Notifications",
                        iconRes = R.drawable.ic_notifications_24dp,
                        badgeCount = unreadCount,
                        fragmentClass = NotificationFragment::class.java,
                        parentHeaderId = 500
                    ),
                    DrawerMenuItem(
                        id = 15,
                        title = "Settings",
                        iconRes = R.drawable.ic_settings_24dp,
                        fragmentClass = ManagerSettingsFragment::class.java,
                        parentHeaderId = 500
                    )
                )
            ),

            // 7. Logout (Single item)
            DrawerMenuItem(
                id = 16,
                title = "Logout",
                iconRes = R.drawable.ic_logout_24dp,
                isLogout = true,
                isHeader = false
            )
        )
    }
}
