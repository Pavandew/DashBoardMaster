package com.example.masterdashboard.manager_single_res_dash.home.views

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels // ✅ Added standard single viewmodel scoping ktx import
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentStaffManagementBinding
import com.example.masterdashboard.manager_single_res_dash.home.adapter.StaffManagementAdapter
import com.example.masterdashboard.manager_single_res_dash.home.models.StaffDataModel
import com.example.masterdashboard.manager_single_res_dash.home.uistate.StaffListUiState
import com.example.masterdashboard.manager_single_res_dash.home.viewModel.StaffFormViewModel
import com.example.masterdashboard.manager_single_res_dash.home.viewModel.StaffManagementViewModel // ✅ Swapped to new ViewModel target location
import com.example.masterdashboard.master_dash.home.SearchQueryManager
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.MenuDialogHelper
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import kotlin.getValue

class StaffManagementFragment : Fragment() {

    companion object {
        private const val TAG = "StaffManagementFragment"
    }

    private var _binding: FragmentStaffManagementBinding? = null
    private val binding get() = _binding!!

    // Switch invocation scope over to your single isolated context tracking view model lifecycle container
    private val viewModel: StaffManagementViewModel by viewModels()
    private val staffViewModel: StaffFormViewModel by activityViewModels()
    private lateinit var staffAdapter: StaffManagementAdapter
    private var searchManager: SearchQueryManager<StaffDataModel>? = null

    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val userRole by lazy { sessionManager.getRole() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: StaffManagementFragment Opened")

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) {
            staffViewModel.clearFormData()
            parentFragmentManager.popBackStack()
        }

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
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvStaffList.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }
                        is StaffListUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvStaffList.visibility = View.VISIBLE
                            binding.tvEmptyState.visibility = View.GONE
                            Log.d(TAG, "🟢 Live data successfully synchronized. Populating list.")

                            val fullList = state.list
                            staffAdapter.updateData(fullList)
                            initSearchManager(fullList)
                        }
                        is StaffListUiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvStaffList.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.VISIBLE
                            Toast.makeText(requireContext(), "No staff profiles found.", Toast.LENGTH_SHORT).show()
                            staffAdapter.updateData(emptyList())
                        }
                        is StaffListUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvStaffList.visibility = View.VISIBLE
                            binding.tvEmptyState.visibility = View.GONE
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
            staffViewModel.clearFormData()
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
        staffAdapter = StaffManagementAdapter(
            onCardLongClick = { staff ->
                showDeleteConfirmationPopup(staff)
            },
            onCardClick = { staff ->
                openStaffDetailView(staff)
            }
        )
        binding.rvStaffList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = staffAdapter
            setHasFixedSize(true)
        }
    }

    private fun openStaffDetailView(staff: StaffDataModel) {
        val detailFragment = StaffDetailFragment().apply {
            arguments = Bundle().apply {
                putString("STAFF_DOCUMENT_ID", staff.id)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.manager_fragmentContainer, detailFragment)
            .addToBackStack("staff_detail_view")
            .commit()
    }
    private fun setupOnClick() {
        binding.fabAddStaffBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.manager_fragmentContainer, AddStaffFragment())
                .addToBackStack(AppConstants.BACKSTACK_ADD_STAFF)
                .commit()
        }
    }

    private fun showDeleteConfirmationPopup(staff: StaffDataModel) {
        val ownerUid = sessionManager.getUid()

        // Clean & Reusable Utility Call!
        MenuDialogHelper.showDeleteConfirmation(
            context = requireContext(),
            title = "Remove Staff Member?",
            message = "Are you sure you want to permanently remove \"${staff.staffName}\" from the staff list? This action cannot be undone.",
            onConfirm = {
                if (ownerUid.isNotEmpty() && staff.id.isNotEmpty()) {
                    // OPTIMISTIC UI FIX: Create a temporary list excluding the deleted item
                    val currentList = staffAdapter.currentList.toMutableList()
                    val indexToRemove = currentList.indexOfFirst { it.id == staff.id }

                    if (indexToRemove != -1) {
                        currentList.removeAt(indexToRemove)
                        Log.d(TAG, "Optimistic UI: Instantly sliding '${staff.staffName}' out of active view memory layout.")
                        // Submit the reduced list immediately so it vanishes from the UI instantly
                        staffAdapter.submitList(currentList)
                    }

                    // Execute the explicit Firebase cloud transaction task
                    viewModel.deleteStaffMember(ownerUid, staff.id, staff.staffName)
                    Toast.makeText(requireContext(), "${staff.staffName} removed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun initSearchManager(fullList: List<StaffDataModel>) {
        searchManager?.removeListener()
        searchManager = SearchQueryManager(
            searchEditText = binding.staffMmSearchBar.etSearch,
            originalList = fullList,
            onResultFiltered = { filteredList ->
                staffAdapter.updateData(filteredList)
            },
            filterRule = { staff, query ->
                staff.staffName.contains(query, ignoreCase = true) ||
                        staff.role.contains(query, ignoreCase = true)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchManager?.removeListener()
        _binding = null
    }
}