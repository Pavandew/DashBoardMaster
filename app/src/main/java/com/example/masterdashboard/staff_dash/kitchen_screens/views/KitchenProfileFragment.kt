package com.example.masterdashboard.staff_dash.kitchen_screens.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentKitchenProfileBinding
import com.example.masterdashboard.login.utils.LogoutManager

class KitchenProfileFragment : Fragment(R.layout.fragment_kitchen_profile) {

    companion object{
        const val TAG = "KitchenProfileFragment"
    }
    private var _binding: FragmentKitchenProfileBinding? = null
    private val binding get() = _binding!!

    // Lazy load the central logout manager safely instance using fragment context
    private val logoutManager by lazy { LogoutManager(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentKitchenProfileBinding.bind(view)

        // Set listener callback to trigger universal logout popup wrapper
        binding.btnKitchenLogout.setOnClickListener {
            logoutManager.showLogoutConfirmation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean out memory references to safeguard against fragment state memory leakage leaks
        _binding = null
    }
}