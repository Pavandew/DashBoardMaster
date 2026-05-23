package com.example.masterdashboard.staff_dash.home.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentStaffDashboardBinding
import com.example.masterdashboard.staff_dash.home.StaffHomeActivity

class StaffDashboardFragment : Fragment() {

    private var _binding: FragmentStaffDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = binding.staffDashboardToolbar
        toolbar.toolbarCenterTvTitle.text = "Dashboard"

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}