package com.example.masterdashboard.staff_dash.kitchen_screens.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentKitchenPreparationBinding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenWorkstationAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderUiState
import com.example.masterdashboard.staff_dash.kitchen_screens.viewModel.KitchenOrderViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KitchenPreparationFragment : Fragment(R.layout.fragment_kitchen_preparation) {

    companion object {
        private const val TAG = "KitchenPreparationFragment"
    }

    private var _binding: FragmentKitchenPreparationBinding? = null
    private val binding get() = _binding!!

    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val viewModel: KitchenOrderViewModel by viewModels()

    private lateinit var filterAdapter: FloorChipsAdapter
    private lateinit var workstationAdapter: KitchenWorkstationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentKitchenPreparationBinding.bind(view)

        Log.i(TAG, "onViewCreated: Active Workstation screen opened.")

        setupToolbar()
        setupChipsRecyclerView()
        setupOrdersRecyclerView()
        setupSearch()
        observeUiState()

        val managerId = sessionManager.getUid()
        viewModel.startListeningOrders(managerId)
        viewModel.setWorkstationContext(true)
        
        // Initial filter for workstation: Show "All" accepted orders (Preparing/Ready) by default
        viewModel.setStatusFilter("All")
    }

    private fun setupToolbar() {
        binding.kitchenToolbar.tvToolbarTitle.text = "Kitchen Preparation Screen"
        binding.kitchenToolbar.toolbarImgMenu.visibility = View.GONE
        binding.kitchenToolbar.toolbarImgNotification.visibility = View.VISIBLE
        binding.kitchenToolbar.toolbarImgNotification.setBackgroundResource(R.drawable.ic_history_24dp)
        binding.kitchenToolbar.toolbarImgNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.kitchen_fragment_container, KitchenHistoryFragment())
                .addToBackStack(null)
                .commit()
        }

    }

    private fun setupChipsRecyclerView() {
        filterAdapter = FloorChipsAdapter { chip ->
            Log.d(TAG, "setupChipsRecyclerView: Status filter changed to: [${chip.name}]")
            viewModel.setStatusFilter(chip.name)
            
            val updatedList = filterAdapter.currentList.map {
                it.copy(isSelected = it.id == chip.id)
            }
            filterAdapter.submitList(updatedList)
        }
        binding.rvOrderFilterChips.adapter = filterAdapter

        val statusFilters = listOf(
            TableFilterData("1", "All", true),
            TableFilterData("2", "Preparing", false),
            TableFilterData("3", "Ready", false),
            TableFilterData("4", "Completed", false)
        )
        filterAdapter.submitList(statusFilters)
    }

    private fun setupOrdersRecyclerView() {
        workstationAdapter = KitchenWorkstationAdapter { selectedOrder ->
            val detailFragment = KitchenPreparationDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("ORDER_DATA_KEY", selectedOrder)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.kitchen_fragment_container, detailFragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvInprogressOrders.adapter = workstationAdapter
    }

    private fun setupSearch() {
        binding.searchBar.etSearchOrder.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is KitchenOrderUiState.Loading -> {
                        binding.pbKitchenLoading.visibility = View.VISIBLE
                    }
                    is KitchenOrderUiState.Success -> {
                        binding.pbKitchenLoading.visibility = View.GONE
                        workstationAdapter.submitList(state.orders)
                    }
                    is KitchenOrderUiState.Error -> {
                        binding.pbKitchenLoading.visibility = View.GONE
                        Toast.makeText(context, "Error: ${state.exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}