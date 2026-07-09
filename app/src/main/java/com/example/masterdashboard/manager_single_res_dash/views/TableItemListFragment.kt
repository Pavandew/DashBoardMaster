package com.example.masterdashboard.manager_single_res_dash.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentTableItemListBinding
import com.example.masterdashboard.manager_single_res_dash.models.TableData
import com.example.masterdashboard.manager_single_res_dash.uistate.TableItemUiState
import com.example.masterdashboard.manager_single_res_dash.viewModel.TableItemListViewModel
import com.example.masterdashboard.manager_single_res_dash.table_management.adapter.AddTableListAdapter
import com.example.masterdashboard.login.utils.AppConstants
import com.example.masterdashboard.login.utils.MenuDialogHelper
import com.example.masterdashboard.login.utils.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TableItemListFragment : Fragment() {

    companion object {
        private const val TAG = "TableItemListFragment ----> "
    }

    private var _binding: FragmentTableItemListBinding? = null
    private val binding get() = _binding!!

    // Decoupled architecture tier initialization [cite: 594]
    private val viewModel: TableItemListViewModel by activityViewModels()
    private lateinit var tableAdapter: AddTableListAdapter

    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val userRole by lazy { sessionManager.getRole() }

    private var floorId: String = ""
    private var floorName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            floorId = it.getString("FLOOR_ID", "")
            floorName = it.getString("FLOOR_NAME", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTableItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Tables Workspace Hybrid Grid View initialized for floor reference: $floorName")

        setupToolbar()
        setupRecyclerView()
        setupRoleBasedAccess()
        setupPermissionAndActions()

        val ownerUid = sessionManager.getUid()
        if (ownerUid.isNotEmpty() && floorId.isNotEmpty()) {
            viewModel.observeTables(ownerUid, floorId) // Invoking separate observer block [cite: 594]
        }
        observeTablesChanges()
    }

    private fun setupToolbar() {
        val toolbar = binding.tableItemToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        toolbar.tvToolbarTitle.text = "$floorName - Tables"
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        tableAdapter = AddTableListAdapter(
            onTableClick = { selectedTable ->
                Log.d(TAG, "Table Row Clicked: ${selectedTable.tableName} Status: ${selectedTable.status}")
            },
            onTableLongClick = { targetTableItem ->
                // RBAC SECURITY GATE: Blocks staff profile definitions from executing deletions [cite: 658]
                if (userRole != AppConstants.ROLE_STAFF) {
                    showTableDeletePopup(targetTableItem)
                } else {
                    Log.w(TAG, "RBAC Access Blocked: STAFF profiles are unauthorized to delete layout table units. [cite: 658]")
                    Toast.makeText(requireContext(), "Access Denied: Staff cannot delete tables.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.rvTablesGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), 3) // Dynamic 3-column auto-wrap layout [cite: 658]
            adapter = tableAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupRoleBasedAccess() {
        // Strict adherence to project rules: Hide table addition trigger completely for STAFF profile roles [cite: 594]
        if (userRole == AppConstants.ROLE_STAFF) {
            Log.w(TAG, "RBAC Security applied: hiding table creation buttons for STAFF role profiles. [cite: 594]")
            binding.btnAddNewTableAction.visibility = View.GONE
        }
    }

    private fun setupPermissionAndActions() {
        binding.btnAddNewTableAction.setOnClickListener {
            Log.d(TAG, "🔘 '+ Add New Table' button clicked. Navigating to AddTableFragment. [cite: 594]")

            val addTableFragment = AddTableFragment().apply {
                arguments = Bundle().apply {
                    putString("PRE_SELECTED_FLOOR_ID", floorId)
                    putString("PRE_SELECTED_FLOOR_NAME", floorName)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.manager_fragmentContainer, addTableFragment)
                .addToBackStack(null)
                .commit()
        }
    }
    private fun observeTablesChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tableItemUiState.collectLatest { state ->
                    // OBSERVER LOGGING: Logs state transitions cleanly [cite: 1511]
                    Log.d(TAG, "observeTablesChanges flow update received. State type: ${state::class.simpleName}")

                    when (state) {
                        is TableItemUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvTablesGrid.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }
                        is TableItemUiState.Success -> {
                            // FIXED VISIBILITY LOGIC: If full screen was hidden by the Loading branch,
                            // we force both the progress bar AND the grid to remain visible simultaneously!
                            binding.rvTablesGrid.visibility = View.VISIBLE
                            binding.tvEmptyState.visibility = View.GONE

                            if (state.isRefreshing) {
                                Log.d(TAG, "Background sync active: revealing progress bar loader widget.")
                                binding.progressBar.visibility = View.VISIBLE
                            } else {
                                Log.d(TAG, "Background sync finished: hiding progress bar loader widget.")
                                binding.progressBar.visibility = View.GONE
                            }

                            // Map metric totals from collection analysis payload values to UI widgets instantly [cite: 1545]
                            binding.tvTotalTablesCount.text = state.totalCount.toString()
//                            binding.tvAvailableCount.text = state.availableCount.toString()
//                            binding.tvOccupiedCount.text = state.occupiedCount.toString()

                            Log.i(TAG, "🎯 Snapshot list pushed to adapter. Element count: ${state.tables.size}")

                            // FIX ADDING DELAY OPTIMIZATION: Passing an inline trailing lambda callback [cite: 1506]
                            tableAdapter.submitList(state.tables) {
                                Log.d(TAG, "✅ ListAdapter completed layout updates pass for ${state.tables.size} tables.")
                                if (state.tables.isNotEmpty()) {
                                    binding.rvTablesGrid.post {
                                        // Forces the framework to invalidate container cache layouts [cite: 1506]
                                        binding.rvTablesGrid.invalidateItemDecorations()
                                    }
                                }
                            }
                        }
                        is TableItemUiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvTablesGrid.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.tvTotalTablesCount.text = "0"
//                            binding.tvAvailableCount.text = "0"
//                            binding.tvOccupiedCount.text = "0"

                            Log.w(TAG, "Empty state collector triggered. Clearing list container adapter.")
                            tableAdapter.submitList(emptyList())
                        }
                        is TableItemUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                            Log.e(TAG, "State Flow Error received: ${state.message}")
                            Toast.makeText(requireContext(), "Synchronization error: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Triggers your TableViewModel single table document deletion function.
     */
    private fun showTableDeletePopup(table: TableData) {
        val ownerUid = sessionManager.getUid()

        MenuDialogHelper.showDeleteConfirmation(
            context = requireContext(),
            title = "Delete Table?",
            message = "Are you sure you want to permanently remove \"${table.tableName}\" from this floor workspace layout?",
            onConfirm = {
                // DELETE LOGGING: Track local optimistic modifications before background operations execute [cite: 612, 622]
                Log.i(TAG, "🗑️ Deletion confirmed for table: '${table.tableName}' (ID: ${table.tableId})")

                // OPTIMISTIC UI FIX: Create a temporary list excluding the deleted item [cite: 624, 656]
                val currentList = tableAdapter.currentList.toMutableList()
                val indexToRemove = currentList.indexOfFirst { it.tableId == table.tableId }

                if (indexToRemove != -1) {
                    currentList.removeAt(indexToRemove)
                    Log.d(TAG, "Optimistic UI: Instantly sliding '${table.tableName}' out of active view memory layout.")
                    // Submit the reduced list immediately so it vanishes from the UI instantly [cite: 624, 656]
                    tableAdapter.submitList(currentList)
                }

                // Invoking the single table deletion method from TableItemListViewModel [cite: 594]
                Log.d(TAG, "Dispatching background transactional table deletion request downstream to ViewModel.")
                viewModel.deleteTableItem(
                    ownerUid = ownerUid,
                    floorId = floorId,      // Extracted from Fragment arguments [cite: 594]
                    tableId = table.tableId,
                    tableName = table.tableName
                )

                Toast.makeText(requireContext(), "Table \"${table.tableName}\" removed successfully.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}