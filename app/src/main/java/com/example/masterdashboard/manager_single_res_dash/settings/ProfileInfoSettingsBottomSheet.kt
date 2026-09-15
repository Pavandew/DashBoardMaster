package com.example.masterdashboard.manager_single_res_dash.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.masterdashboard.databinding.BottomSheetProfileInfoBinding
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProfileInfoSettingsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ProfileInfoSettingsBottomSheet"
    }

    private var _binding: BottomSheetProfileInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

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
        val restaurantId = sessionManager.getRestaurantId().ifEmpty { sessionManager.getUid() }

        binding.tvProfileName.text = userName
        binding.tvProfileRole.text = displayRole
        binding.tvProfileMobile.text = if (mobile.startsWith("+91")) mobile else "+91 $mobile"
        binding.tvProfileRestaurant.text = restaurantName
        binding.tvProfileUid.text = restaurantId
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
