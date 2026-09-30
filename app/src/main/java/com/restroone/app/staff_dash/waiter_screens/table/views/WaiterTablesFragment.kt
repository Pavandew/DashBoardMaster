package com.restroone.app.staff_dash.waiter_screens.table.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.restroone.app.R
import com.restroone.app.databinding.FragmentWaiterTablesBinding
import com.restroone.app.staff_dash.utils.TableDialogHelper
import com.restroone.app.utils.NavigationUtils
import com.restroone.app.utils.SessionManager
import com.restroone.app.master_dash.utils.SearchQueryManager
import com.restroone.app.staff_dash.waiter_screens.WaiterHomeActivity
import com.restroone.app.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
import com.restroone.app.staff_dash.waiter_screens.table.adapter.TableCardsAdapter
import com.restroone.app.staff_dash.waiter_screens.table.models.TableCardData
import androidx.fragment.app.activityViewModels
import com.restroone.app.staff_dash.waiter_screens.table.repo.WaiterTableRepository
import com.restroone.app.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import com.restroone.app.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import com.restroone.app.staff_dash.waiter_screens.table.viewModels.WaiterTableViewModel
import com.restroone.app.staff_dash.waiter_screens.table.models.TableStatus
import com.restroone.app.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WaiterTablesFragment : Fragment() {
    companion object {
        private const val TAG = "Table_Flow_Debug"
    }

    private var _binding: FragmentWaiterTablesBinding? = null
    private val binding get() = _binding!!

    private val sessionManager by lazy { SessionManager(requireContext()) }
    
    private val viewModel: WaiterTableViewModel by viewModels {
        WaiterTableViewModel.TableViewModelFactory(WaiterTableRepository())
    }

    // Shared ViewModel to store customer info before navigation
    private val orderTakingViewModel: OrderTakingViewModel by activityViewModels {
        OrderTakingViewModel.OrderViewModelFactory(OrderTakingRepository())
    }

    private lateinit var tableAdapter: TableCardsAdapter
    private lateinit var floorAdapter: FloorChipsAdapter
    private var searchManager: SearchQueryManager<TableCardData>? = null

    private val currentSearchList = mutableListOf<TableCardData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "WaiterTablesFragment onCreateView layout inflation initiated.")
        _binding = FragmentWaiterTablesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "📱 WaiterTablesFragment onViewCreated lifecycle hook reached.")

        val managerId = sessionManager.getUid()
        val managerName = sessionManager.getUserName() ?: "Unknown Manager"
        Log.d(TAG, "WaiterTablesFragment Session Context: [ManagerName: $managerName | ID: $managerId]")

        setupToolbar()
        setUpRecyclerView()
        setupSearchEngine()
        setupSwipeRefreshAndRetry(managerId)
        observeViewModelData()
        setupFloorChips()

        // Load data on startup
        viewModel.loadDashboardData(managerId)
    }

    private fun setupSwipeRefreshAndRetry(managerId: String) {
        binding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "📱 WaiterTablesFragment Swipe-to-refresh triggered.")
            viewModel.forceRefresh(managerId)
        }

        binding.includeErrorState.btnErrorRetry.setOnClickListener {
            Log.d(TAG, "📱 WaiterTablesFragment Retry button clicked.")
            binding.includeErrorState.layoutErrorContainer.visibility = View.GONE
            viewModel.forceRefresh(managerId)
        }
    }

    override fun onStart() {
        super.onStart()
        Log.v(TAG, "📱 WaiterTablesFragment onStart lifecycle visibility check triggered.")
        (activity as? WaiterHomeActivity)?.showBottomNavigation()
        (activity as? com.restroone.app.staff_dash.billing_screens.CashierHomeActivity)?.showBottomNavigation()
    }

    private fun setupToolbar() {
        val toolbar = binding.waiterTablesToolbar
        toolbar.tvToolbarTitle.text = getString(R.string.title_manage_tables)
        toolbar.llSubtitleContainer.visibility = View.GONE
        
        if (parentFragmentManager.backStackEntryCount > 0) {
            toolbar.toolbarImgMenu.visibility = View.VISIBLE
            toolbar.toolbarImgMenu.setImageResource(R.drawable.ic_arrow_back_24dp)
            toolbar.toolbarImgMenu.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
        } else {
            toolbar.toolbarImgMenu.visibility = View.GONE
        }
    }

    private fun setUpRecyclerView() {
        tableAdapter = TableCardsAdapter { table ->
            Log.d(TAG, "📱 WaiterTablesFragment Card Item selected: Table ${table.tableId}")
            navigateToOrderTaking(table)
        }
        binding.rvTableCards.adapter = tableAdapter
    }

    private fun setupFloorChips() {
        floorAdapter = FloorChipsAdapter { selectedChip ->
            Log.d(TAG, "📱 WaiterTablesFragment Floor chip clicked: Title = '${selectedChip.name}', ID = '${selectedChip.id}'")
            viewModel.setFloorFilter(selectedChip.id)
            binding.rvTableCards.post {
                binding.rvTableCards.scrollToPosition(0)
                binding.appBarLayout.setExpanded(true, true)
            }
        }
        binding.rvFloorChips.adapter = floorAdapter
    }

    private fun observeViewModelData() {
        Log.d(TAG, "📱 WaiterTablesFragment Observing Unified UiState flow...")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var lastAutoScrolledFloorId: String? = null

                viewModel.uiState.collect { state ->
                    // 1. Loading & Swipe Refresh indicators
                    binding.pbLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = state.isRefreshing

                    // 2. Error vs Content visibility
                    if (state.errorMessage != null) {
                        binding.rvTableCards.visibility = View.GONE
                        binding.includeErrorState.layoutErrorContainer.visibility = View.VISIBLE
                        binding.includeErrorState.tvErrorStateMessage.text = state.errorMessage
                    } else {
                        binding.includeErrorState.layoutErrorContainer.visibility = View.GONE
                        binding.rvTableCards.visibility = View.VISIBLE

                        tableAdapter.updateList(state.tables) {
                            binding.rvTableCards.post {
                                binding.rvTableCards.scrollToPosition(0)
                                binding.appBarLayout.setExpanded(true, false)
                            }
                        }
                        currentSearchList.clear()
                        currentSearchList.addAll(state.tables)

                        if (binding.searchBar.etSearchOrder.text.isNotEmpty()) {
                            searchManager?.refreshSearch()
                        }
                    }

                    // 3. Floors Chip List & Auto-Scroll
                    if (state.floors.isNotEmpty()) {
                        floorAdapter.submitList(state.floors)

                        val currentSelectedId = state.floors.find { it.isSelected }?.id
                        if (currentSelectedId != null && currentSelectedId != lastAutoScrolledFloorId) {
                            val selectedPos = state.floors.indexOfFirst { it.id == currentSelectedId }
                            if (selectedPos != -1) {
                                binding.rvFloorChips.post {
                                    binding.rvFloorChips.smoothScrollToPosition(selectedPos)
                                }
                            }
                            lastAutoScrolledFloorId = currentSelectedId
                        }
                    }
                }
            }
        }
    }

    private fun setupSearchEngine() {
        searchManager?.removeListener()
        searchManager = SearchQueryManager(
            searchEditText = binding.searchBar.etSearchOrder,
            originalList = currentSearchList,
            onResultFiltered = { filteredList ->
                Log.d(TAG, "📱 [SEARCH MANAGER] Filter constraint matched ${filteredList.size} matching table item results.")
                tableAdapter.updateList(filteredList)
            },
            filterRule = { table, query ->
                table.tableName.contains(query, ignoreCase = true) || table.tableId.contains(query, ignoreCase = true)
            }
        )
    }

    private fun navigateToOrderTaking(table: TableCardData) {
        when (table.status) {
            TableStatus.FREE -> showCustomerInfoDialog(table)
            TableStatus.RESERVED -> showReservedTableDialog(table)
            else -> proceedToOrderTaking(table)
        }
    }

    private fun showCustomerInfoDialog(table: TableCardData) {
        val managerId = sessionManager.getUid()
        TableDialogHelper.showCustomerInfoDialog(
            context = requireContext(),
            inflater = layoutInflater,
            onStartOrder = { name, phone ->
                orderTakingViewModel.setCustomerDetails(name, phone, "DINE_IN")
                if (name.isNotEmpty()) {
                    viewModel.updateTableStatus(managerId, table.floorId, table.tableId, TableStatus.OCCUPIED, name)
                }
                proceedToOrderTaking(table)
            },
            onReserve = { name ->
                viewModel.updateTableStatus(managerId, table.floorId, table.tableId, TableStatus.RESERVED, name)
                Toast.makeText(requireContext(), "Table ${table.tableName} Reserved for $name", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showReservedTableDialog(table: TableCardData) {
        val managerId = sessionManager.getUid()
        TableDialogHelper.showReservedTableActionsDialog(
            context = requireContext(),
            inflater = layoutInflater,
            table = table,
            onStartOrder = {
                viewModel.updateTableStatus(managerId, table.floorId, table.tableId, TableStatus.OCCUPIED, table.customerName)
                proceedToOrderTaking(table)
            },
            onRelease = {
                viewModel.updateTableStatus(managerId, table.floorId, table.tableId, TableStatus.FREE)
                Toast.makeText(requireContext(), "Reservation Released", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun proceedToOrderTaking(table: TableCardData) {
        // HIDE IMMEDIATELY to prevent flicker during fragment transition
        (activity as? WaiterHomeActivity)?.hideBottomNavigation()

        val orderTakingFragment = WaiterOrderTakingFragment().apply {
            arguments = Bundle().apply {
                putString("tableId", table.tableId)
                putString("tableName", table.tableName)
                putString("floorId", table.floorId)
                putInt("totalSeats", table.totalSeats)
                putString("status", table.status.name)
            }
        }
        val containerId = NavigationUtils.getHostContainerId(activity)
        if (containerId != 0) {
            parentFragmentManager.beginTransaction().apply {
                replace(containerId, orderTakingFragment, "WaiterOrderTakingFragment")
                addToBackStack("WaiterOrderTakingFragment")
                commit()
            }
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "📱 WaiterTablesFragment onDestroyView triggered. Cleaning up search engine components.")
        searchManager?.removeListener()
        super.onDestroyView()
        _binding = null
    }
}