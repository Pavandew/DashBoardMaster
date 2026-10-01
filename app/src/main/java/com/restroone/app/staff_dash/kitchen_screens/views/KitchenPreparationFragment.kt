package com.restroone.app.staff_dash.kitchen_screens.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.restroone.app.R
import com.restroone.app.databinding.FragmentKitchenPreparationBinding
import com.restroone.app.utils.NavigationUtils
import com.restroone.app.utils.SessionManager
import com.restroone.app.staff_dash.kitchen_screens.adapter.KitchenWorkstationAdapter
import com.restroone.app.staff_dash.kitchen_screens.uistate.KitchenOrderUiState
import com.restroone.app.staff_dash.kitchen_screens.viewModel.KitchenOrderViewModel
import com.restroone.app.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
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
        
        val managerId = sessionManager.getUid()
        setupSwipeRefreshAndRetry(managerId)
        observeUiState()

        viewModel.startListeningOrders(managerId)
        viewModel.setWorkstationContext(true)
    }

    private fun setupSwipeRefreshAndRetry(managerId: String) {
        binding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "Swipe-to-refresh triggered on Kitchen Workstation")
            viewModel.forceRefresh(managerId)
        }

        binding.includeErrorState.btnErrorRetry.setOnClickListener {
            Log.d(TAG, "Retry button clicked on Kitchen Workstation")
            binding.includeErrorState.layoutErrorContainer.visibility = View.GONE
            viewModel.forceRefresh(managerId)
        }
    }

    private fun setupToolbar() {
        binding.kitchenToolbar.apply {
            tvToolbarTitle.text = getString(R.string.title_cooking_workstation)
            toolbarImgNotification.visibility = View.VISIBLE
            toolbarImgNotification.setBackgroundResource(R.drawable.ic_history_24dp)
//            toolbarImgNotification.setOnClickListener {
//                val containerId = NavigationUtils.getHostContainerId(activity)
//                if (containerId != 0) {
//                    parentFragmentManager.beginTransaction()
//                        .replace(containerId, KitchenHistoryFragment())
//                        .addToBackStack(null)
//                        .commit()
//                }
//            }

            if (parentFragmentManager.backStackEntryCount > 0) {
                toolbarImgMenu.visibility = View.VISIBLE
                toolbarImgMenu.setImageResource(R.drawable.ic_arrow_back_24dp)
                toolbarImgMenu.setOnClickListener {
                    parentFragmentManager.popBackStack()
                }
            } else {
                toolbarImgMenu.visibility = View.GONE
            }
        }
    }

    private fun setupChipsRecyclerView() {
        filterAdapter = FloorChipsAdapter { chip ->
            // Extract the original name from label "Name (Count)"
            val originalName = chip.name.substringBefore(" (").trim()
            Log.d(TAG, "setupChipsRecyclerView: Status filter changed to: [$originalName]")
            viewModel.setStatusFilter(originalName)
        }
        binding.rvOrderFilterChips.adapter = filterAdapter
    }

    private fun setupOrdersRecyclerView() {
        workstationAdapter = KitchenWorkstationAdapter { selectedOrder ->
            val detailFragment = KitchenPreparationDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("ORDER_DATA_KEY", selectedOrder)
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
        binding.rvInprogressOrders.adapter = workstationAdapter
    }

    private fun setupSearch() {
        binding.searchBar.etSearchOrder.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private var lastAutoScrolledStatusId: String? = null

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.pbKitchenLoading.visibility = if (state.isLoading && !state.isRefreshing) View.VISIBLE else View.GONE
                binding.swipeRefresh.isRefreshing = state.isRefreshing

                if (state.errorMessage != null) {
                    binding.rvInprogressOrders.visibility = View.GONE
                    binding.includeEmptyState.layoutEmptyContainer.visibility = View.GONE
                    binding.includeErrorState.layoutErrorContainer.visibility = View.VISIBLE
                    binding.includeErrorState.tvErrorStateMessage.text = state.errorMessage
                } else {
                    binding.includeErrorState.layoutErrorContainer.visibility = View.GONE

                    workstationAdapter.submitList(state.orders)
                    filterAdapter.submitList(state.filters)

                    // AUTO-SCROLL: Only scroll to the selected chip if it's different from the last scrolled one
                    val currentSelectedId = state.filters.find { it.isSelected }?.id
                    if (currentSelectedId != null && currentSelectedId != lastAutoScrolledStatusId) {
                        val selectedPos = state.filters.indexOfFirst { it.id == currentSelectedId }
                        if (selectedPos != -1) {
                            binding.rvOrderFilterChips.post {
                                binding.rvOrderFilterChips.smoothScrollToPosition(selectedPos)
                            }
                        }
                        lastAutoScrolledStatusId = currentSelectedId
                    }

                    // Show or hide empty state based on list size
                    if (state.orders.isEmpty()) {
                        binding.rvInprogressOrders.visibility = View.GONE
                        binding.includeEmptyState.layoutEmptyContainer.visibility = View.VISIBLE
                        binding.includeEmptyState.tvEmptyStateTitle.text = "Workstation Clear"
                        binding.includeEmptyState.tvEmptyStateMessage.text = "There are no orders currently being prepared in the kitchen."
                    } else {
                        binding.includeEmptyState.layoutEmptyContainer.visibility = View.GONE
                        binding.rvInprogressOrders.visibility = View.VISIBLE
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