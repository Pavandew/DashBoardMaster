package com.example.masterdashboard.staff_dash.kitchen_screens.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentKitchenOrderBinding
import com.example.masterdashboard.utils.NavigationUtils
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenOrderStreamAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderUiState
import com.example.masterdashboard.staff_dash.kitchen_screens.viewModel.KitchenOrderViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KitchenOrderFragment : Fragment(R.layout.fragment_kitchen_order) {

    companion object {
        private const val TAG = "KitchenOrderFragment"
    }

    private var _binding: FragmentKitchenOrderBinding? = null
    private val binding get() = _binding!!

    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val viewModel: KitchenOrderViewModel by viewModels()
    private lateinit var orderAdapter: KitchenOrderStreamAdapter
    private lateinit var filterAdapter: FloorChipsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentKitchenOrderBinding.bind(view)

        Log.i(TAG, "onViewCreated: KitchenOrderFragment screen opened.")

        setupToolbar()
        setupChipsRecyclerView()
        setupOrdersRecyclerView()
        setupSearch()
        observeUiState()

        val managerId = sessionManager.getUid()
        viewModel.startListeningOrders(managerId)
        viewModel.setWorkstationContext(false)
    }

    private fun setupToolbar() {
        binding.toolbarKitchen.title = getString(R.string.title_live_orders)
        
        if (parentFragmentManager.backStackEntryCount > 0) {
            binding.toolbarKitchen.setNavigationIcon(R.drawable.ic_arrow_back_24dp)
            binding.toolbarKitchen.setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        } else {
            binding.toolbarKitchen.navigationIcon = null
        }
        
        // Optional: If you want menu items (like Sound Toggle or Refresh) on the toolbar
        binding.toolbarKitchen.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_refresh -> {
                    val managerId = sessionManager.getUid()
                    viewModel.startListeningOrders(managerId)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupChipsRecyclerView() {
        filterAdapter = FloorChipsAdapter { chip ->
            // Extract the original name from label "Name (Count)"
            val originalName = chip.name.substringBefore(" (").trim()
            Log.d(TAG, "setupChipsRecyclerView: Filter changed to: [$originalName]")
            viewModel.setTypeFilter(originalName)
        }

        binding.rvOrderFilterChips.adapter = filterAdapter
    }

    private fun setupOrdersRecyclerView() {
        orderAdapter = KitchenOrderStreamAdapter { clickedOrderData ->
            val detailFragment = KitchenOrderDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("ORDER_DATA_KEY", clickedOrderData)
                }
            }

            val containerId = NavigationUtils.getHostContainerId(activity)
            if (containerId != 0) {
                parentFragmentManager.beginTransaction()
                    .replace(containerId, detailFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
        binding.rvKitchenOrdersStream.adapter = orderAdapter
    }

    private fun setupSearch() {
        binding.searchBar.etSearchOrder.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private var lastAutoScrolledFilterId: String? = null

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is KitchenOrderUiState.Loading -> {
                            binding.pbKitchenLoading.visibility = View.VISIBLE
                            binding.toolbarKitchen.subtitle = "Updating tickets..."
                        }
                        is KitchenOrderUiState.Success -> {
                            binding.pbKitchenLoading.visibility = View.GONE
                            orderAdapter.submitList(state.orders)
                            filterAdapter.submitList(state.filters)
                            
                            // AUTO-SCROLL: Only scroll to the selected chip if it's different from the last scrolled one
                            val currentSelectedId = state.filters.find { it.isSelected }?.id
                            if (currentSelectedId != null && currentSelectedId != lastAutoScrolledFilterId) {
                                val selectedPos = state.filters.indexOfFirst { it.id == currentSelectedId }
                                if (selectedPos != -1) {
                                    binding.rvOrderFilterChips.post {
                                        binding.rvOrderFilterChips.smoothScrollToPosition(selectedPos)
                                    }
                                }
                                lastAutoScrolledFilterId = currentSelectedId
                            }

                            // Show or hide empty state based on list size
                            if (state.orders.isEmpty()) {
                                binding.rvKitchenOrdersStream.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.VISIBLE
                            } else {
                                binding.rvKitchenOrdersStream.visibility = View.VISIBLE
                                binding.layoutEmptyState.visibility = View.GONE
                            }

                            // Dynamic update of subtitle with live order count
                            val count = state.orders.size
                            binding.toolbarKitchen.subtitle = if (count == 0) {
                                "No active tickets"
                            } else {
                                "$count active tickets to prepare"
                            }
                        }
                        is KitchenOrderUiState.Error -> {
                            binding.pbKitchenLoading.visibility = View.GONE
                            binding.toolbarKitchen.subtitle = "Error loading orders"
                            Toast.makeText(context, "Error: ${state.exception.message}", Toast.LENGTH_LONG).show()
                        }
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