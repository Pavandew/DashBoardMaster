package com.example.masterdashboard.manager_single_res_dash.home.views

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // ✅ Added standard single viewmodel scoping ktx import
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentStaffManagementBinding
import com.example.masterdashboard.manager_single_res_dash.home.adapter.StaffManagementAdapter
import com.example.masterdashboard.manager_single_res_dash.home.uistate.StaffListUiState
import com.example.masterdashboard.manager_single_res_dash.home.viewModel.StaffManagementViewModel // ✅ Swapped to new ViewModel target location
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class StaffManagementFragment : Fragment() {

    companion object {
        private const val TAG = "StaffManagementFragment"
    }

    private var _binding: FragmentStaffManagementBinding? = null
    private val binding get() = _binding!!

    // ✅ Switch invocation scope over to your single isolated context tracking view model lifecycle container
    private val viewModel: StaffManagementViewModel by viewModels()
    private lateinit var staffAdapter: StaffManagementAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: StaffManagementFragment Opened")

        setupToolbar()
        setupSearchBar()
        setupRecyclerView()
        setupOnClick()

        // Fetch values via clean decoupled runtime context
        val ownerUid = SessionManager(requireContext()).getUid()
        if (ownerUid.isNotEmpty()) {
            viewModel.loadStaffMembers(ownerUid)
        }

        observeStaffListChanges()
    }

    private fun observeStaffListChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.staffListState.collect { state ->
                    when (state) {
                        is StaffListUiState.Loading -> {
                            // Optionally turn on layout frame shimmer/progress loading spinners here
                        }
                        is StaffListUiState.Success -> {
                            Log.d(TAG, "🟢 Live data successfully synchronized. Populating list.")

                            // Map incoming array instances up into adapter constructor models layout views
                            // Note: If you updated your adapter to accept StaffDataModel, this connects perfectly.
                            staffAdapter = StaffManagementAdapter(state.list)
                            binding.rvStaffList.adapter = staffAdapter
                        }
                        is StaffListUiState.Empty -> {
                            Toast.makeText(requireContext(), "No staff profiles found.", Toast.LENGTH_SHORT).show()
                            staffAdapter = StaffManagementAdapter(emptyList())
                            binding.rvStaffList.adapter = staffAdapter
                        }
                        is StaffListUiState.Error -> {
                            Toast.makeText(requireContext(), "Error linking records collection: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.SMtoolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        toolbar.tvToolbarTitle.text = getString(R.string.staff_management)
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupSearchBar() {
        val searchBar = binding.staffMmSearchBar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        searchBar.etSearch.setTextColor(whiteColor)
        searchBar.etSearch.setHintTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))

        val textInputLayout = searchBar.root.getChildAt(0) as? TextInputLayout
        textInputLayout?.setStartIconTintList(ColorStateList.valueOf(whiteColor))
    }

    private fun setupRecyclerView() {
        staffAdapter = StaffManagementAdapter(emptyList())
        binding.rvStaffList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = staffAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupOnClick() {
        binding.fabAddStaffBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.manager_fragmentContainer, AddStaffFragment())
                .addToBackStack(AppConstants.BACKSTACK_ADD_STAFF)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}