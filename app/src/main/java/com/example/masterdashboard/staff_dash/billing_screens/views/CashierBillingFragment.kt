package com.example.masterdashboard.staff_dash.billing_screens.views

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
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentCashierBillingBinding
import com.example.masterdashboard.utils.NavigationUtils
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.staff_dash.billing_screens.adapter.CashierBillingAdapter
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.example.masterdashboard.staff_dash.billing_screens.uiState.CashierBillingUiState
import com.example.masterdashboard.staff_dash.billing_screens.viewmodel.CashierBillingViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
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

        setupToolbar()
        setupAdapters()
        setupSearch()
        observeViewModel()

        val managerId = sessionManager.getUid()
        Log.d(TAG, "Fetching billing orders for Manager ID: $managerId")
        viewModel.startListeningOrders(managerId)
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
                    when (state) {
                        is CashierBillingUiState.Loading -> {
                            Log.d(TAG, "UI State -> Loading")
                            binding.pbBillingLoading.visibility = View.VISIBLE
                        }
                        is CashierBillingUiState.Success -> {
                            Log.d(TAG, "UI State -> Success: Received ${state.orders.size} orders")
                            binding.pbBillingLoading.visibility = View.GONE
                            ordersAdapter.submitList(state.orders)
                            filterAdapter.submitList(state.filters)
                            
                            // Removed automatic smoothScrollToPosition to prevent "All" chip from being pushed away
                        }
                        is CashierBillingUiState.Error -> {
                            Log.e(TAG, "UI State -> Error: ${state.message}")
                            binding.pbBillingLoading.visibility = View.GONE
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        }
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
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}