package com.example.masterdashboard.staff_dash.waiter_screens.table.views

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
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentWaiterTablesBinding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.master_dash.utils.SearchQueryManager
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.TableCardsAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableCardData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
import androidx.fragment.app.activityViewModels
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.WaiterTableRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.WaiterTableViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.OrderTakingFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.EditText
import com.google.android.material.button.MaterialButton
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableStatus
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
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
        observeViewModelData()
        setupFloorChips()

        // FIX: Instead of manual fetch triggers, set the managerId state flow to fire both real-time streams
        viewModel.loadDashboardData(managerId)
    }

    override fun onStart() {
        super.onStart()
        Log.v(TAG, "📱 WaiterTablesFragment onStart lifecycle visibility check triggered.")
        (activity as? WaiterHomeActivity)?.showBottomNavigation()
    }

    private fun setupToolbar() {
        val toolbar = binding.waiterTablesToolbar
        toolbar.tvToolbarTitle.text = getString(R.string.tables)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgMenu.setImageResource(R.drawable.ic_menu_24dp)
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

            val updatedList = floorAdapter.currentList.map {
                it.copy(isSelected = it.id == selectedChip.id)
            }
            floorAdapter.submitList(updatedList)
            filterTablesByFloor(selectedChip)
        }
        binding.rvFloorChips.adapter = floorAdapter
    }

    private fun filterTablesByFloor(selectedChip: TableFilterData) {
        val masterList = viewModel.originalTableList
        Log.d(TAG, "📱 WaiterTablesFragment Running table dataset filter operation against base list size: ${masterList.size}")

        val filteredList = if (selectedChip.id == "ALL_FLOORS") {
            Log.v(TAG, "📱 WaiterTablesFragment 'All' filter matching selected. Resetting constraints.")
            masterList
        } else {
            val res = masterList.filter { it.floorId == selectedChip.id }
            Log.v(TAG, "📱 WaiterTablesFragment Specific constraint matching applied. Found ${res.size} tables matched with Floor ID: '${selectedChip.id}'")
            res
        }

        currentSearchList.clear()
        currentSearchList.addAll(filteredList)
        tableAdapter.updateList(filteredList)
    }

    private fun observeViewModelData() {
        Log.d(TAG, "📱 WaiterTablesFragment Hooking up UI flows to coroutine repeatOnLifecycle collectors...")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Collect Live Tables
                launch {
                    viewModel.tableState.collect { resource ->
                        when (resource) {
                            is ResourceUiState.Loading -> {
                                Log.d(TAG, "📱 WaiterTablesFragment UI Collector received: [ResourceUiState.Loading] ➔ Displaying ProgressIndicator.")
                                binding.pbLoading.visibility = View.VISIBLE
                                binding.rvTableCards.visibility = View.GONE
                            }
                            is ResourceUiState.Success -> {
                                Log.i(TAG, "📱 WaiterTablesFragment UI Collector received: [ResourceUiState.Success] ➔ Populating ${resource.data.size} cards to Adapter.")
                                binding.pbLoading.visibility = View.GONE
                                binding.rvTableCards.visibility = View.VISIBLE

                                tableAdapter.updateList(resource.data)
                                currentSearchList.clear()
                                currentSearchList.addAll(viewModel.originalTableList)
                            }
                            is ResourceUiState.Error -> {
                                Log.e(TAG, "📱 WaiterTablesFragment UI Collector received: [ResourceUiState.Error] ➔ Reason: ${resource.message}")
                                binding.pbLoading.visibility = View.GONE
                                binding.rvTableCards.visibility = View.VISIBLE
                                Toast.makeText(context, resource.message, Toast.LENGTH_LONG).show()
                            }
                            else -> {}
                        }
                    }
                }

                // Collect Live Dynamic Floors
                launch {
                    viewModel.floorState.collectLatest { dynamicFloors ->
                        Log.i(TAG, "📱 WaiterTablesFragment UI Collector received floor list change event. Submitting ${dynamicFloors.size} elements to Chip Layout.")
                        if (dynamicFloors.isNotEmpty()) {
                            floorAdapter.submitList(dynamicFloors)
                        }
                    }
                }
            }
        }
    }

    private fun setupSearchEngine() {
        searchManager?.removeListener()
        searchManager = SearchQueryManager(
            searchEditText = binding.etSearchTable,
            originalList = currentSearchList,
            onResultFiltered = { filteredList ->
                Log.d(TAG, "📱 [SEARCH MANAGER] Filter constraint matched ${filteredList.size} matching table item results.")
                tableAdapter.updateList(filteredList)
            },
            filterRule = { table, query ->
                table.tableId.contains(query, ignoreCase = true)
            }
        )
    }

    private fun navigateToOrderTaking(table: TableCardData) {
        if (table.status == TableStatus.FREE) {
            showCustomerInfoDialog(table)
        } else {
            proceedToOrderTaking(table)
        }
    }

    private fun showCustomerInfoDialog(table: TableCardData) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_customer_info, null)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogCustomerName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etDialogPhoneNumber)
        val btnSkip = dialogView.findViewById<MaterialButton>(R.id.btnDialogSkip)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnDialogConfirm)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnSkip.setOnClickListener {
            orderTakingViewModel.setCustomerDetails("", "", "DINE_IN")
            dialog.dismiss()
            proceedToOrderTaking(table)
        }

        btnConfirm.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            orderTakingViewModel.setCustomerDetails(name, phone, "DINE_IN")
            dialog.dismiss()
            proceedToOrderTaking(table)
        }

        dialog.show()
    }

    private fun proceedToOrderTaking(table: TableCardData) {
        val orderTakingFragment = OrderTakingFragment().apply {
            arguments = Bundle().apply {
                putString("tableId", table.tableId)
                putString("tableName", table.tableName)
                putString("floorId", table.floorId)
                putInt("totalSeats", table.totalSeats)
                putString("status", table.status.name)
            }
        }
        parentFragmentManager.beginTransaction().apply {
            replace(this@WaiterTablesFragment.id, orderTakingFragment, "OrderTakingFragment")
            addToBackStack("OrderTakingFragment")
            commit()
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "📱 WaiterTablesFragment onDestroyView triggered. Cleaning up search engine components.")
        searchManager?.removeListener()
        super.onDestroyView()
        _binding = null
    }
}