package com.example.masterdashboard.staff_dash.waiter_screens.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.masterdashboard.databinding.FragmentStaffProfileBinding
import com.example.masterdashboard.login.utils.LogoutManager

class StaffProfileFragment : Fragment() {

    private var _binding: FragmentStaffProfileBinding? = null
    private val binding get() = _binding!!
    private val logoutManager by lazy { LogoutManager(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()

        binding.waiterLogoutBtn.setOnClickListener {
            logoutManager.showLogoutConfirmation()
        }
    }

    private fun setupToolbar() {
        binding.staffProfileToolbar.tvToolbarTitle.text = "Profile"
        binding.staffProfileToolbar.llSubtitleContainer.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}