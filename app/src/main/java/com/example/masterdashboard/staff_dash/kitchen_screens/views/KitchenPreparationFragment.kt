package com.example.masterdashboard.staff_dash.kitchen_screens.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentKitchenPreparationBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenFilterChipAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenWorkstationAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderUiState
import com.example.masterdashboard.staff_dash.kitchen_screens.viewModel.KitchenOrderViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KitchenPreparationFragment : Fragment(R.layout.fragment_kitchen_preparation) {

    companion object {
        private const val TAG = "KitchenPreparationFragment"
    }

    private var _binding: FragmentKitchenPreparationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: KitchenOrderViewModel by viewModels()

    private lateinit var chipAdapter: KitchenFilterChipAdapter
    private lateinit var workstationAdapter: KitchenWorkstationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentKitchenPreparationBinding.bind(view)

        Log.i(TAG, "onViewCreated: Active Workstation screen opened.")

        setupChipsRecyclerView()
        setupOrdersRecyclerView()
        observeUiState()

        // Set the active workflow filter criteria to match the requested workstation view ("Preparing")
        viewModel.setStatusFilter("Preparing")
    }

    private fun setupChipsRecyclerView() {
        val workstationSubCategories = listOf("All", "Veg", "Non-veg")
        chipAdapter = KitchenFilterChipAdapter(workstationSubCategories) { selectedStatus ->
            Log.d(TAG, "setupChipsRecyclerView: Chef shifted workstation sub-filter matrix to category: [$selectedStatus]")
            viewModel.setStatusFilter(selectedStatus)
        }
        binding.rvOrderFilterChips.adapter = chipAdapter
    }

    private fun setupOrdersRecyclerView() {
        workstationAdapter = KitchenWorkstationAdapter { selectedOrder ->
            Log.i(TAG, "setupOrdersRecyclerView: Order selected: #${selectedOrder.orderId}. Packing data payload to route to Detail Inspector screen.")

            val detailFragment = KitchenOrderDetailFragment().apply {
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

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is KitchenOrderUiState.Loading -> {
                        Log.d(TAG, "observeUiState: Pipeline data transfer ongoing from repository layer...")
                    }
                    is KitchenOrderUiState.Success -> {
                        Log.i(TAG, "observeUiState: Workflow data received from Firebase. Workstation active display row count: ${state.orders.size}")
                        workstationAdapter.submitList(state.orders)
                    }
                    is KitchenOrderUiState.Error -> {
                        Log.e(TAG, "observeUiState: Critical error transmitted directly from Firestore connection flow channel", state.exception)
                        Toast.makeText(context, "Error: ${state.exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Clearing view binding reference map configurations to prevent memory leaks.")
        _binding = null
    }
}