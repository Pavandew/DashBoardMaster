package com.restroone.app.manager_single_res_dash.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.restroone.app.databinding.BottomSheetProfileInfoBinding
import com.restroone.app.subscription.models.SubscriptionStatus
import com.restroone.app.subscription.repo.SubscriptionRepository
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ProfileInfoSettingsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ProfileInfoSettingsBottomSheet"
    }

    private var _binding: BottomSheetProfileInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var subscriptionRepository: SubscriptionRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetProfileInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        subscriptionRepository = SubscriptionRepository(requireContext())

        populateProfileDetails()

        binding.btnCloseProfile.setOnClickListener {
            dismiss()
        }
    }

    private fun populateProfileDetails() {
        val userName = sessionManager.getUserName() ?: "User"
        val rawRole = sessionManager.getRole()
        val displayRole = when (rawRole) {
            AppConstants.ROLE_OWNER_SINGLE -> "Single Restaurant Owner"
            AppConstants.ROLE_MANAGER -> "Restaurant Manager"
            else -> rawRole.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }

        val mobile = sessionManager.getPhone() ?: "N/A"
        val restaurantName = sessionManager.getRestaurantName().ifEmpty { "Master Restaurant" }

        binding.tvProfileName.text = userName
        binding.tvProfileRole.text = displayRole
        binding.tvProfileMobile.text = if (mobile.startsWith("+91")) mobile else "+91 $mobile"
        binding.tvProfileRestaurant.text = restaurantName

        lifecycleScope.launch {
            try {
                val subInfo = subscriptionRepository.getUserSubscriptionInfo()
                val statusText = when (subInfo.status) {
                    SubscriptionStatus.ACTIVE_YEARLY -> "⭐ Yearly Owner Pass (Valid till ${subInfo.getFormattedExpiryDate()})"
                    SubscriptionStatus.ACTIVE_MONTHLY -> "⭐ Monthly Owner Pass (Valid till ${subInfo.getFormattedExpiryDate()})"
                    SubscriptionStatus.TRIAL -> if (subInfo.trialDaysRemaining > 0) "👑 ${subInfo.trialDaysRemaining} Days Free Trial Left" else "⚠️ Free Trial Expired"
                    SubscriptionStatus.EXPIRED -> "⚠️ Subscription Expired"
                    SubscriptionStatus.FREE_BASE -> "🎁 Free Starter Plan"
                }
                binding.tvProfileSubscription.text = statusText
            } catch (e: Exception) {
                binding.tvProfileSubscription.text = "👑 Free Trial Active"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
