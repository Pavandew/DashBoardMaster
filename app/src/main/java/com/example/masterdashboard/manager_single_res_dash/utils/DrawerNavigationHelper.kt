package com.example.masterdashboard.manager_single_res_dash.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.SingleResOwnerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.adapter.DrawerMenuAdapter
import com.example.masterdashboard.manager_single_res_dash.models.DrawerMenuItem
import com.example.masterdashboard.manager_single_res_dash.viewModel.DrawerViewModel
import com.example.masterdashboard.subscription.models.SubscriptionStatus
import com.example.masterdashboard.subscription.views.SubscriptionPlansFragment
import com.example.masterdashboard.utils.LogoutManager
import com.example.masterdashboard.utils.NavigationUtils
import com.example.masterdashboard.utils.SessionManager
import coil.load
import kotlinx.coroutines.launch

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

    private val logoutManager by lazy { LogoutManager(context) }
    private val sessionManager by lazy { SessionManager(context) }

    private val viewModel: DrawerViewModel by lazy {
        ViewModelProvider(fragment)[DrawerViewModel::class.java]
    }

    private var menuAdapter: DrawerMenuAdapter? = null
    private var menuItems: MutableList<DrawerMenuItem> = mutableListOf()

    init {
        observeViewModelState()
    }

    private fun observeViewModelState() {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d(TAG, "DrawerUiState emitted -> Name='${state.userName}', Role='${state.displayRole}', DaysLeft=${state.trialDaysRemaining}")
                    renderDrawerState(state)
                }
            }
        }
    }

    /**
     * Handles routing for any given sidebar drawer item click event
     */
    fun handleNavigation(item: DrawerMenuItem) {
        if (item.isLogout) {
            logoutManager.showLogoutConfirmation()
            return
        }

        if (item.id == 13 || (item.fragmentClass == null && item.activityClass == null)) {
            Log.i(TAG, "Clicked placeholder menu item: ${item.title}")
            Toast.makeText(context, "🎁 ${item.title} feature coming soon!", Toast.LENGTH_SHORT).show()
            return
        }

        item.activityClass?.let { activityClass ->
            Log.i(TAG, "Navigating to Activity: ${activityClass.simpleName}")
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

        item.fragmentClass?.let { fragmentClass ->
            val currentFragment = activity?.supportFragmentManager?.findFragmentById(targetContainer)
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
        viewModel.loadInitialData(restaurantName)
    }

    /**
     * Initializes the side navigation drawer with dynamic menu items.
     */
    fun initDrawerMenu() {
        val currentActivity = activity ?: return

        val (drawerLayout, navigationView) = when (currentActivity) {
            is ManagerHomeActivity -> currentActivity.binding.drawerLayout to currentActivity.binding.navigationView
            is SingleResOwnerHomeActivity -> currentActivity.activityBinding.drawerLayout to currentActivity.activityBinding.navigationView
            else -> null to null
        }

        if (drawerLayout == null || navigationView == null) return

        menuItems = viewModel.uiState.value.menuItems.toMutableList()

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

        renderDrawerState(viewModel.uiState.value)
    }

    private fun renderDrawerState(state: com.example.masterdashboard.manager_single_res_dash.uistate.DrawerUiState) {
        val currentActivity = activity ?: return
        val navigationView = when (currentActivity) {
            is ManagerHomeActivity -> currentActivity.binding.navigationView
            is SingleResOwnerHomeActivity -> currentActivity.activityBinding.navigationView
            else -> null
        } ?: return

        val nameTv = navigationView.findViewById<TextView>(R.id.drawerProfileName)
        val roleTv = navigationView.findViewById<TextView>(R.id.drawerProfileRole)
        val trialBadgeTv = navigationView.findViewById<TextView>(R.id.tvDrawerTrialBadge)

        val profileImg = navigationView.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.drawerProfileImage)
            ?: navigationView.findViewById<android.widget.ImageView>(R.id.imgProfile)

        val logoUrl = sessionManager.getRestaurantLogoUrl()
        if (!logoUrl.isNullOrEmpty() && profileImg != null) {
            profileImg.load(logoUrl) {
                crossfade(true)
                placeholder(R.drawable.person)
                error(R.drawable.person)
            }
        } else {
            profileImg?.setImageResource(R.drawable.person)
        }

        nameTv?.text = state.userName

        if (state.restaurantName.isNotEmpty()) {
            roleTv?.text = "${state.displayRole} • ${state.restaurantName}"
        } else {
            roleTv?.text = state.displayRole
        }

        if (trialBadgeTv != null) {
            when {
                state.subscriptionStatus == SubscriptionStatus.EXPIRED || state.trialDaysRemaining <= 0 -> {
                    trialBadgeTv.text = "⚠️ Free Trial Expired"
                    trialBadgeTv.setBackgroundResource(R.drawable.bg_expired_badge)
                    trialBadgeTv.setTextColor(android.graphics.Color.parseColor("#EF5350"))
                }
                state.subscriptionStatus == SubscriptionStatus.TRIAL -> {
                    trialBadgeTv.text = "👑 ${state.trialDaysRemaining} Days Trial Left"
                    trialBadgeTv.setBackgroundResource(R.drawable.bg_trial_badge)
                    trialBadgeTv.setTextColor(android.graphics.Color.parseColor("#FFD54F"))
                }
                else -> {
                    trialBadgeTv.text = "⭐ PRO Member"
                    trialBadgeTv.setBackgroundResource(R.drawable.bg_pro_badge)
                    trialBadgeTv.setTextColor(android.graphics.Color.parseColor("#D8B4FE"))
                }
            }

            trialBadgeTv.setOnClickListener {
                handleNavigation(
                    DrawerMenuItem(
                        17,
                        "Subscription & Plans",
                        R.drawable.ic_card_payment_24dp,
                        fragmentClass = SubscriptionPlansFragment::class.java
                    )
                )
                val drawerLayout = when (currentActivity) {
                    is ManagerHomeActivity -> currentActivity.binding.drawerLayout
                    is SingleResOwnerHomeActivity -> currentActivity.activityBinding.drawerLayout
                    else -> null
                }
                drawerLayout?.closeDrawer(GravityCompat.START)
            }
        }

        if (state.menuItems.isNotEmpty()) {
            menuItems.clear()
            menuItems.addAll(state.menuItems)
            menuAdapter?.updateMenuItems(menuItems)
        }
    }

    /**
     * Cleanup resources if needed
     */
    fun destroy() {
        Log.d(TAG, "DrawerNavigationHelper destroy called")
    }
}
