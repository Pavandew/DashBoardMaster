package com.example.masterdashboard.manager_single_res_dash.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentManagerSettingsBinding
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.SingleResOwnerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.adapter.SettingsAdapter
import com.example.masterdashboard.manager_single_res_dash.models.SettingsCategory
import com.example.masterdashboard.manager_single_res_dash.models.SettingsOption
import com.example.masterdashboard.manager_single_res_dash.repo.RestaurantDetailsRepository
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.LogoutManager
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

class ManagerSettingsFragment : Fragment() {

    companion object {
        private const val TAG = "ManagerSettingsFragment"
    }

    private var _binding: FragmentManagerSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var logoutManager: LogoutManager
    private lateinit var settingsAdapter: SettingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: ManagerSettingsFragment opened")

        sessionManager = SessionManager(requireContext())
        logoutManager = LogoutManager(requireContext())

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        val toolbar = binding.settingsToolbar

        toolbar.btnBack.setOnClickListener {
            Log.d(TAG, "Action: Back button clicked")
            parentFragmentManager.popBackStack()
        }

        toolbar.btnDrawerMenu.setOnClickListener {
            Log.d(TAG, "Action: Navigation drawer button clicked")
            when (val currentActivity = activity) {
                is ManagerHomeActivity -> currentActivity.openNavigationDrawer()
                is SingleResOwnerHomeActivity -> currentActivity.openNavigationDrawer()
            }
        }
    }

    private fun setupRecyclerView() {
        val categories = getSettingsCategories()
        settingsAdapter = SettingsAdapter(categories) { selectedOption ->
            handleOptionClick(selectedOption)
        }

        binding.rvSettingsCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingsAdapter
        }
    }

    private fun handleOptionClick(option: SettingsOption) {
        Log.d(TAG, "Selected Option: ${option.title} (ID: ${option.id})")
        when (option.id) {
            "res_details" -> openRegistrationEditStep(1)
            "documents" -> openRegistrationEditStep(2)
            "tax_config" -> openRegistrationEditStep(4)
            "operating_hours" -> openRegistrationEditStep(5)
            "payment_methods" -> {
                val upiSheet = UpiPaymentSettingsBottomSheet()
                upiSheet.show(childFragmentManager, UpiPaymentSettingsBottomSheet.TAG)
            }
            "service_charge" -> {
                val serviceChargeSheet = ServiceChargeSettingsBottomSheet()
                serviceChargeSheet.show(childFragmentManager, ServiceChargeSettingsBottomSheet.TAG)
            }
            "logout" -> logoutManager.showLogoutConfirmation()
            else -> Toast.makeText(requireContext(), "${option.title} clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openRegistrationEditStep(step: Int) {
        Log.i(TAG, "Action: Navigating to Registration Edit Mode at Step $step")
        val ownerUid = sessionManager.getUid()

        if (ownerUid.isEmpty()) {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        val draft = sessionManager.getRegistrationDraft()
        if (draft != null) {
            launchRegistrationActivity(step)
        } else {
            lifecycleScope.launch {
                try {
                    val repository = RestaurantDetailsRepository()
                    val result = repository.getRestaurantDetails(ownerUid)
                    val details = result.getOrNull()
                    if (details != null) {
                        sessionManager.saveRegistrationDraft(details)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pre-fetch restaurant details for edit mode", e)
                } finally {
                    launchRegistrationActivity(step)
                }
            }
        }
    }

    private fun launchRegistrationActivity(step: Int) {
        val intent = Intent(requireContext(), SingleResOwnerHomeActivity::class.java).apply {
            putExtra(AppConstants.EXTRA_EDIT_MODE, true)
            putExtra(AppConstants.EXTRA_START_STEP, step)
        }
        startActivity(intent)
    }

    private fun getSettingsCategories(): List<SettingsCategory> {
        val userName = sessionManager.getUserName() ?: "User"
        val rawRole = sessionManager.getRole()
        val displayRole = when (rawRole) {
            AppConstants.ROLE_OWNER_SINGLE -> "Single Restaurant Owner"
            AppConstants.ROLE_MANAGER -> "Restaurant Manager"
            else -> rawRole.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }

        return listOf(
            SettingsCategory(
                id = "restaurant_profile",
                title = "Restaurant & Business Profile",
                subtitle = "Basic info, operating hours and documents",
                iconRes = R.drawable.ic_restaurant_24dp,
                iconTintRes = R.color.mid_app_color,
                options = listOf(
                    SettingsOption(
                        id = "res_details",
                        title = "Restaurant Details & Business Info",
                        subtitle = "Name, address, contact & registration data",
                        iconRes = R.drawable.ic_edit_24dp
                    ),
                    SettingsOption(
                        id = "operating_hours",
                        title = "Operating Hours & Working Days",
                        subtitle = "Opening & closing timings per day",
                        iconRes = R.drawable.ic_nest_clock_24dp
                    ),
                    SettingsOption(
                        id = "documents",
                        title = "Licenses & Legal Documents",
                        subtitle = "FSSAI, GSTIN & business permits",
                        iconRes = R.drawable.ic_badge_24dp
                    )
                )
            ),
            SettingsCategory(
                id = "pos_billing",
                title = "POS & Billing Settings",
                subtitle = "Taxes, service charges & receipt customization",
                iconRes = R.drawable.ic_payments_24dp,
                iconTintRes = R.color.accent_blue,
                options = listOf(
                    SettingsOption(
                        id = "tax_config",
                        title = "Tax Configuration (GST / VAT)",
                        subtitle = "Set CGST/SGST rates & inclusive/exclusive rules",
                        iconRes = R.drawable.ic_discount_24dp
                    ),
                    SettingsOption(
                        id = "service_charge",
                        title = "Service Charge Settings",
                        subtitle = "Default service charge % & enable toggle",
                        iconRes = R.drawable.ic_add_circle_24dp
                    ),
                    SettingsOption(
                        id = "receipt_layout",
                        title = "Receipt & Bill Layout",
                        subtitle = "Custom bill header, footer text & logo options",
                        iconRes = R.drawable.ic_sales_report_24dp
                    ),
                    SettingsOption(
                        id = "payment_methods",
                        title = "Payment Gateways & Modes",
                        subtitle = "Cash, UPI, Card and Split payment toggles",
                        iconRes = R.drawable.ic_card_payment_24dp
                    )
                )
            ),
            SettingsCategory(
                id = "kitchen_rules",
                title = "Kitchen & Order Rules",
                subtitle = "Cancellation and security PIN settings",
                iconRes = R.drawable.ic_chef_24dp,
                iconTintRes = R.color.accent_orange,
                options = listOf(
                    SettingsOption(
                        id = "manager_pin",
                        title = "Order Cancellation & Manager PIN",
                        subtitle = "Require Manager PIN for item voids or discounts",
                        iconRes = R.drawable.ic_lock_24dp
                    )
                )
            ),
            SettingsCategory(
                id = "account_support",
                title = "Account & Support",
                subtitle = "User credentials, password & logout",
                iconRes = R.drawable.ic_person_24dp,
                iconTintRes = R.color.accent_purple,
                options = listOf(
                    SettingsOption(
                        id = "profile_info",
                        title = "Profile Information",
                        subtitle = "$userName • $displayRole",
                        iconRes = R.drawable.ic_person_24dp
                    ),
                    SettingsOption(
                        id = "change_password",
                        title = "Change Password",
                        subtitle = "Update your account login password",
                        iconRes = R.drawable.ic_vpn_key_24dp
                    ),
                    SettingsOption(
                        id = "app_version",
                        title = "App Version & Help Support",
                        subtitle = "Master Dashboard v1.0.0 • Contact support",
                        iconRes = R.drawable.ic_shield_24dp
                    ),
                    SettingsOption(
                        id = "logout",
                        title = "Logout Account",
                        subtitle = "Sign out securely from Master Dashboard",
                        iconRes = R.drawable.ic_logout_24dp,
                        isDestructive = true
                    )
                )
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
