package com.example.masterdashboard.staff_dash.home.order

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.masterdashboard.databinding.FragmentStaffOrdersBinding
import com.example.masterdashboard.staff_dash.home.StaffHomeActivity

class StaffOrdersFragment : Fragment() {

    private var _binding: FragmentStaffOrdersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
    }

    private fun setupToolbar() {
        binding.staffOrdersToolbar.toolbarCenterTvTitle.text = "Orders"
        binding.staffOrdersToolbar.toolbarImgMenu.setOnClickListener {
            (activity as? StaffHomeActivity)?.openDrawer()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
