package com.example.masterdashboard.staff_dash.home.table.views

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
import androidx.recyclerview.widget.GridLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentStaffTablesBinding
import com.example.masterdashboard.master_dash.home.SearchQueryManager
import com.example.masterdashboard.staff_dash.home.StaffHomeActivity
import com.example.masterdashboard.staff_dash.home.table.adapter.FloorChipsAdapter
import com.example.masterdashboard.staff_dash.home.table.adapter.TableCardsAdapter
import com.example.masterdashboard.staff_dash.home.table.models.TableCardData
import com.example.masterdashboard.staff_dash.home.table.models.TableFilterData
import com.example.masterdashboard.staff_dash.home.table.repo.TableRepository
import com.example.masterdashboard.staff_dash.home.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.home.table.viewModels.TableViewModel
import kotlinx.coroutines.launch

class StaffTablesFragment : Fragment() {
    companion object {
        private const val TAG = "StaffTablesFragment"
    }

    private var _binding: FragmentStaffTablesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TableViewModel by viewModels {
        TableViewModel.TableViewModelFactory(TableRepository())
    }

    private lateinit var tableAdapter: TableCardsAdapter
    private lateinit var floorAdapter: FloorChipsAdapter
    private var searchManager: SearchQueryManager<TableCardData>? = null

    // The active, clean list reference pointer managed by your UI observation cycle
    private val currentSearchList = mutableListOf<TableCardData>()

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
        setUpRecyclerView()
        setupSearchEngine() // Hooks up the listener to 'currentSearchList' immediately on start
        observeViewModelData()
        setupFloorChips()
    }

    // show bottom nav when comes from another fragment
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Restoring Bottom Navigation visibility for main dashboard grid view.")
        (activity as? StaffHomeActivity)?.showBottomNavigation()
    }

    private fun setupToolbar() {
        val toolbar = binding.staffTablesToolbar
        toolbar.tvToolbarTitle.text = getString(R.string.tables)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgMenu.setImageResource(R.drawable.ic_menu_24dp)
        toolbar.toolbarImgMenu.setOnClickListener {
//            (activity as? StaffHomeActivity)?.openDrawer()
        }
    }

    private fun setUpRecyclerView() {
        tableAdapter = TableCardsAdapter { table ->
            navigateToOrderTaking(table)
        }
        binding.rvTableCards.layoutManager = GridLayoutManager(context, 3)
        binding.rvTableCards.adapter = tableAdapter
    }

    private fun setupFloorChips() {
        floorAdapter = FloorChipsAdapter { selectedChip ->
            val updatedList = floorAdapter.currentList.map {
                it.copy(isSelected = it.id == selectedChip.id)
            }
            floorAdapter.submitList(updatedList)
        }
        binding.rvFloorChips.adapter = floorAdapter

        val floors = listOf(
            TableFilterData("1", "All", true),
            TableFilterData("2", "Ground Floor"),
            TableFilterData("3", "First Floor"),
            TableFilterData("4", "Roof Top")
        )
        floorAdapter.submitList(floors)
    }

    private fun observeViewModelData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tableState.collect { resource ->
                    when (resource) {
                        is ResourceUiState.Loading -> {
                            binding.pbLoading.visibility = View.VISIBLE
                            binding.rvTableCards.visibility = View.GONE
                        }

                        is ResourceUiState.Success -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.rvTableCards.visibility = View.VISIBLE

                            tableAdapter.updateList(resource.data)

                            // Keep the search manager pointer updated with pristine data updates
                            currentSearchList.clear()
                            currentSearchList.addAll(viewModel.originalTableList)
                        }

                        // FIXED: Cleaned up to fetch direct message String mapping
                        is ResourceUiState.Error -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.rvTableCards.visibility = View.VISIBLE

                            val errorMessage = resource.message
                            Log.e(TAG, "UI Observation Error: $errorMessage")
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupSearchEngine() {
        searchManager?.removeListener()

        Log.d(TAG, "Initializing search engine listening utility.")

        searchManager = SearchQueryManager(
            searchEditText = binding.etSearchTable,
            originalList = currentSearchList, // FIXED: Pointing to mutable currentSearchList instead of the static initial state list
            onResultFiltered = { filteredList ->
                Log.d(
                    TAG,
                    "Search query updated. ${filteredList.size} tables match the current query."
                )
                tableAdapter.updateList(filteredList)
            },
            filterRule = { table, query ->
                table.tableId.contains(query, ignoreCase = true) ||
                        table.floorId.contains(query, ignoreCase = true)
            }
        )
    }

    private fun navigateToOrderTaking(table: TableCardData) {
        val orderTakingFragment = OrderTakingFragment().apply {
            arguments = Bundle().apply {
                putString("tableId", table.tableId)
                putInt("totalSeats", table.totalSeats)
                putString("status", table.status.name)
            }
        }

        parentFragmentManager.beginTransaction().apply {
            replace(this@StaffTablesFragment.id, orderTakingFragment, "OrderTakingFragment")
            addToBackStack("OrderTakingFragment")
            commit()
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView: Clearing instances and removing TextWatchers.")
        searchManager?.removeListener()
        super.onDestroyView()
        _binding = null
    }
}