package com.example.masterdashboard.manager_single_res_dash.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentTableManagementBinding
import com.example.masterdashboard.manager_single_res_dash.models.FloorDataModel
import com.example.masterdashboard.manager_single_res_dash.uistate.TableUiState
import com.example.masterdashboard.manager_single_res_dash.utils.AddFloorBottomSheet
import com.example.masterdashboard.manager_single_res_dash.viewModel.TableManagementViewModel
import com.example.masterdashboard.manager_single_res_dash.table_management.adapter.FloorListAdapter
import com.example.masterdashboard.login.utils.AppConstants
import com.example.masterdashboard.login.utils.MenuDialogHelper
import com.example.masterdashboard.login.utils.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TableManagementFragment : Fragment() {

    companion object {
        private const val TAG = "TableManagementFragment ----> "
    }

    private var _binding: FragmentTableManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TableManagementViewModel by viewModels()
    private lateinit var floorListAdapter: FloorListAdapter

    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val userRole by lazy { sessionManager.getRole() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTableManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: TableManagementFragment Opened")

        setupToolbar()
        setupRecyclerView()
        setupRoleBasedAccess()
        setupPermissionAndAction()

        val ownerUid = sessionManager.getUid()
        if (ownerUid.isNotEmpty()) {
            viewModel.observeFloors(ownerUid)
        }
        observeFloorChanges()
    }

    private fun setupToolbar() {
        val toolbar = binding.tableManageToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        toolbar.tvToolbarTitle.text = "Table Management"
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        floorListAdapter = FloorListAdapter(
            onFloorClick = { selectedFloor ->
                Log.d(TAG, "Selected floor: ${selectedFloor.floorName} -> ID: ${selectedFloor.floorId}")

                val tableItemListFragment = TableItemListFragment().apply {
                    arguments = Bundle().apply {
                        putString("FLOOR_ID", selectedFloor.floorId)
                        putString("FLOOR_NAME", selectedFloor.floorName)
                    }
                }

                Log.i(TAG, "Navigate to TableItemListFragment")

                parentFragmentManager.beginTransaction()
                    .replace(R.id.manager_fragmentContainer, tableItemListFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onItemLongClick = { targetFloorItem ->
                if (userRole != AppConstants.ROLE_STAFF) {
                    showFloorDeletePopup(targetFloorItem)
                } else {
                    Log.w(TAG, "Action Denied: Staff roles are unauthorized to delete configurations.")
                }
            }
        )

        // Unified variable naming to match your layout IDs precisely
        binding.rvTableManagement.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = floorListAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupRoleBasedAccess() {
        binding.btnAddFloor.text = "+ Add New Floor"

        if (userRole == AppConstants.ROLE_STAFF) {
            Log.i(TAG, "Staff member role detected. Applying floor view restrictions.")
            binding.btnAddFloor.visibility = View.GONE
        }
    }

    private fun setupPermissionAndAction() {
        binding.btnAddFloor.text = "+ Add New Floor"

        binding.btnAddFloor.setOnClickListener {
            Log.d(TAG, "🔘 '+ Add New Floor' button clicked.")
            val ownerUid = sessionManager.getUid()

            if (ownerUid.isEmpty()) {
                Log.e(TAG, "Access Denied: Empty owner UID token string.")
                Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val addFloorSheet = AddFloorBottomSheet { enteredFloorName ->
                Log.i(TAG, "Bottom sheet callback fired! Input: '$enteredFloorName'")

                val trackingListSize = floorListAdapter.currentList.size

                viewModel.addNewFloors(
                    ownerUid = ownerUid,
                    floorName = enteredFloorName,
                    currentListSize = trackingListSize
                )

                Toast.makeText(requireContext(), "Adding floor: $enteredFloorName", Toast.LENGTH_SHORT).show()
            }

            addFloorSheet.show(childFragmentManager, "ADD_FLOOR_SHEET")
        }
    }

    private fun observeFloorChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.floorUiState.collectLatest { state ->
                    when (state) {
                        is TableUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvTableManagement.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }
                        is TableUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvTableManagement.visibility = View.VISIBLE
                            binding.tvEmptyState.visibility = View.GONE
                            Log.d(TAG, "🟢 Floor synchronization successful: ${state.list.size} entries.")
                            floorListAdapter.submitList(state.list)
                        }
                        is TableUiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvTableManagement.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.VISIBLE
                            floorListAdapter.submitList(emptyList())
                        }
                        is TableUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                            Toast.makeText(requireContext(), "Error linking layouts: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun showFloorDeletePopup(floor: FloorDataModel) {
        val ownerUid = sessionManager.getUid()

        MenuDialogHelper.showDeleteConfirmation(
            context = requireContext(),
            title = "Delete Category?",
            message = "Are you sure you want to delete \"${floor.floorName}\"? This will also clear out all dishes under it.",
            onConfirm = {
                // OPTIMISTIC UI FIX: Create a temporary list excluding the deleted item
                val currentList = floorListAdapter.currentList.toMutableList()
                val indexToRemove = currentList.indexOfFirst { it.floorId == floor.floorId }

                if (indexToRemove != -1) {
                    currentList.removeAt(indexToRemove)
                    Log.d(TAG, "Optimistic UI: Instantly sliding '${floor.floorName}' out of active view memory layout.")
                    // Submit the reduced list immediately so it vanishes from the UI instantly
                    floorListAdapter.submitList(currentList)
                }

                // Trigger your category viewmodel delete function here!
                viewModel.deleteFloorItem(ownerUid, floor.floorId, floor.floorName)

                Toast.makeText(requireContext(), "Category ${floor.floorName} removed", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}