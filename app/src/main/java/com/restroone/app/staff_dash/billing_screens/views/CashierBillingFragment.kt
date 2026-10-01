package com.restroone.app.staff_dash.billing_screens.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.restroone.app.R
import com.restroone.app.databinding.FragmentCashierBillingBinding
import com.restroone.app.utils.NavigationUtils
import com.restroone.app.utils.SessionManager
import com.restroone.app.staff_dash.billing_screens.adapter.CashierBillingAdapter
import com.restroone.app.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.restroone.app.staff_dash.billing_screens.uiState.CashierBillingUiState
import com.restroone.app.staff_dash.billing_screens.viewmodel.CashierBillingViewModel
import com.restroone.app.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
import kotlinx.coroutines.launch

class CashierBillingFragment : Fragment() {

    companion object {
        private const val TAG = "CashierBillingFragment"
    }

    private var _binding: FragmentCashierBillingBinding? = null
    private val binding get() = _binding!!

    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val viewModel: CashierBillingViewModel by viewModels {
        CashierBillingViewModel.Factory()
    }

    private lateinit var ordersAdapter: CashierBillingAdapter
    private lateinit var filterAdapter: FloorChipsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Inflating layout for Billing screen")
        _binding = FragmentCashierBillingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Initializing Billing dashboard")

        val managerId = sessionManager.getUid()

        setupToolbar()
        setupAdapters()
        setupSearch()
        setupSwipeRefreshAndRetry(managerId)
        observeViewModel()

        Log.d(TAG, "Fetching billing orders for Manager ID: $managerId")
        viewModel.startListeningOrders(managerId)
    }

    private fun setupSwipeRefreshAndRetry(managerId: String) {
        binding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "Swipe-to-refresh triggered on Cashier Settlement Center")
            viewModel.forceRefresh(managerId)
        }

        binding.includeErrorState.btnErrorRetry.setOnClickListener {
            Log.d(TAG, "Retry button clicked on Cashier Settlement Center")
            binding.includeErrorState.layoutErrorContainer.visibility = View.GONE
            viewModel.forceRefresh(managerId)
        }
    }

    private fun setupToolbar() {
        binding.billingToolbar.apply {
            tvToolbarTitle.text = getString(R.string.title_settlement_center)
            toolbarImgNotification.visibility = View.GONE

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

    private fun setupAdapters() {
        ordersAdapter = CashierBillingAdapter(
            onGenerateBillClicked = { order ->
                Log.i(TAG, "Order selected for settlement: ${order.orderId} (Table: ${order.tableName})")
                navigateToSettlement(order)
            },
            onConfirmHandoverClicked = { order ->
                Log.i(TAG, "Confirming handover for order: ${order.orderId}")
                viewModel.confirmPickup(order)
            }
        )
        binding.rvCashierBillingList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ordersAdapter
        }

        filterAdapter = FloorChipsAdapter { chip ->
            // Extract the original name from label "Name (Count)"
            val originalName = if (chip.name.contains(" (")) {
                chip.name.substringBefore(" (").trim()
            } else {
                chip.name
            }
            Log.d(TAG, "Filter chip clicked: $originalName")
            viewModel.setFilter(originalName)
        }
        binding.rvCashierFilterChips.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = filterAdapter
        }
    }

    private fun setupSearch() {
        binding.searchBar.etSearchOrder.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            Log.v(TAG, "Searching billing orders with query: '$query'")
            viewModel.searchOrders(query)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.pbBillingLoading.visibility = if (state.isLoading && !state.isRefreshing) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = state.isRefreshing

                    if (state.errorMessage != null) {
                        binding.rvCashierBillingList.visibility = View.GONE
                        binding.includeEmptyState.layoutEmptyContainer.visibility = View.GONE
                        binding.includeErrorState.layoutErrorContainer.visibility = View.VISIBLE
                        binding.includeErrorState.tvErrorStateMessage.text = state.errorMessage
                    } else {
                        binding.includeErrorState.layoutErrorContainer.visibility = View.GONE

                        if (state.orders.isEmpty()) {
                            binding.rvCashierBillingList.visibility = View.GONE
                            binding.includeEmptyState.layoutEmptyContainer.visibility = View.VISIBLE
                            binding.includeEmptyState.tvEmptyStateTitle.text = "No Bills to Settle"
                            binding.includeEmptyState.tvEmptyStateMessage.text = "There are no pending or active bills in Settlement Center currently."
                        } else {
                            binding.includeEmptyState.layoutEmptyContainer.visibility = View.GONE
                            binding.rvCashierBillingList.visibility = View.VISIBLE
                        }

                        ordersAdapter.submitList(state.orders) {
                            if (state.orders.isNotEmpty()) {
                                binding.rvCashierBillingList.post {
                                    binding.rvCashierBillingList.scrollToPosition(0)
                                }
                            }
                        }
                        filterAdapter.submitList(state.filters)
                    }
                }
            }
        }
    }

    private fun navigateToSettlement(order: CashierBillingOrderModel) {
        Log.d(TAG, "Navigating to CashierSettleBillFragment for order ${order.orderId}")
        val settlementFragment = CashierSettleBillFragment.newInstance(order)
        val containerId = NavigationUtils.getHostContainerId(activity)
        if (containerId != 0) {
            parentFragmentManager.beginTransaction()
                .replace(containerId, settlementFragment)
                .addToBackStack("settlement_flow")
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}