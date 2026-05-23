package com.example.masterdashboard.staff_dash.home.table

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.masterdashboard.databinding.FragmentStaffTablesBinding
import com.example.masterdashboard.staff_dash.home.StaffHomeActivity

class StaffTablesFragment : Fragment() {

    private var _binding: FragmentStaffTablesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffTablesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
    }

    private fun setupToolbar() {
        binding.staffTablesToolbar.toolbarCenterTvTitle.text = "Tables"
        binding.staffTablesToolbar.toolbarImgMenu.setOnClickListener {
            (activity as? StaffHomeActivity)?.openDrawer()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
