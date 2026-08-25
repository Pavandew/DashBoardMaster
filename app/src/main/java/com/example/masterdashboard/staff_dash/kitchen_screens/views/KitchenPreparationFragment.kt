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
import com.example.masterdashboard.utils.NavigationUtils
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenWorkstationAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderUiState
import com.example.masterdashboard.staff_dash.kitchen_screens.viewModel.KitchenOrderViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
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
    }

    private fun setupToolbar() {
        binding.kitchenToolbar.apply {
            tvToolbarTitle.text = getString(R.string.title_cooking_workstation)
            toolbarImgNotification.visibility = View.VISIBLE
            toolbarImgNotification.setBackgroundResource(R.drawable.ic_history_24dp)
            toolbarImgNotification.setOnClickListener {
                val containerId = NavigationUtils.getHostContainerId(activity)
                if (containerId != 0) {
                    parentFragmentManager.beginTransaction()
                        .replace(containerId, KitchenHistoryFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }

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
                when (state) {
                    is KitchenOrderUiState.Loading -> {
                        binding.pbKitchenLoading.visibility = View.VISIBLE
                    }
                    is KitchenOrderUiState.Success -> {
                        binding.pbKitchenLoading.visibility = View.GONE
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
                            binding.layoutEmptyState.visibility = View.VISIBLE
                        } else {
                            binding.rvInprogressOrders.visibility = View.VISIBLE
                            binding.layoutEmptyState.visibility = View.GONE
                        }
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