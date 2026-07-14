package com.example.masterdashboard.staff_dash.kitchen_screens.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentKitchenOrderBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.viewModel.KitchenOrderViewModel
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenFilterChipAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenOrderStreamAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderUiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KitchenOrderFragment : Fragment(R.layout.fragment_kitchen_order) {

    companion object {
        private const val TAG = "KitchenOrderFragment"
    }

    private var _binding: FragmentKitchenOrderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: KitchenOrderViewModel by viewModels()
    private lateinit var orderAdapter: KitchenOrderStreamAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentKitchenOrderBinding.bind(view)

        Log.i(TAG, "onViewCreated: KitchenOrderFragment screen opened and layout elements initialized successfully.")

        setupChipsRecyclerView()
        setupOrdersRecyclerView()
        observeUiState()
    }

    private fun setupChipsRecyclerView() {
        val statusCategories = listOf("All", "New", "Preparing", "Ready", "Completed")
        val chipAdapter = KitchenFilterChipAdapter(statusCategories) { selectedStatus ->
            Log.d(TAG, "setupChipsRecyclerView: Chef changed active display layout filter matrix to category: [$selectedStatus]")
            viewModel.setStatusFilter(selectedStatus)
        }
        binding.rvOrderFilterChips.adapter = chipAdapter
    }
    private fun setupOrdersRecyclerView() {
        // orderAdapter should be bound to use List<KitchenOrderDetailData>
        orderAdapter = KitchenOrderStreamAdapter { clickedOrderData ->
            Log.i(TAG, "setupOrdersRecyclerView: Passing full object data for Table [${clickedOrderData.tableName}] to next screen.")

            val detailFragment = KitchenOrderDetailFragment().apply {
                arguments = Bundle().apply {
                    // Pass the whole data class object instantly using putSerializable
                    putSerializable("ORDER_DATA_KEY", clickedOrderData)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.kitchen_fragment_container, detailFragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvKitchenOrdersStream.adapter = orderAdapter
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is KitchenOrderUiState.Loading -> {
                        Log.d(TAG, "observeUiState: Real-time orders stream log collection loading...")
                    }
                    is KitchenOrderUiState.Success -> {
                        Log.i(TAG, "observeUiState: Real-time order log data fetched from Firebase. Total tickets parsed in list stream: ${state.orders.size}")
                        orderAdapter.submitList(state.orders)
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
        Log.d(TAG, "onDestroyView: Cleaning layout references to eliminate potential fragment memory leakage leaks.")
        _binding = null
    }
}