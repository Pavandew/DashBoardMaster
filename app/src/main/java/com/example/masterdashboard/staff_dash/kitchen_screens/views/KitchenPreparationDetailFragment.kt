package com.example.masterdashboard.staff_dash.kitchen_screens.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentKitchenPreparationDetailBinding // Using your main container layout binding
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenPreparationDetailAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderDetailUiState
import com.example.masterdashboard.staff_dash.kitchen_screens.viewModel.KitchenPreparationDetailViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class KitchenPreparationDetailFragment : Fragment(R.layout.fragment_kitchen_preparation_detail) {

    companion object {
        private const val TAG = "KitchenPrepDetailFrag"
    }

    private var _binding: FragmentKitchenPreparationDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: KitchenPreparationDetailViewModel by viewModels()
    private lateinit var preparationDetailAdapter: KitchenPreparationDetailAdapter
    private var currentOrderId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentKitchenPreparationDetailBinding.bind(view)

        // 1. Unpack full data class instance pushed dynamically from workstations list stream
        val sharedOrderData = arguments?.getSerializable("ORDER_DATA_KEY") as? KitchenOrderDetailData

        if (sharedOrderData == null) {
            Log.e(TAG, "onViewCreated: Aborting because shared workstation order object is missing.")
            Toast.makeText(context, "Error: Invalid Order Reference Data", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        currentOrderId = sharedOrderData.orderId
        Log.i(TAG, "onViewCreated: Workstation preparation sheet mounted for Order Reference ID: [$currentOrderId]")

        setupRecyclerView()
        setupClickListeners()
        observeUiState()

        // Populate baseline fields instantly with zero loading delay metrics
        populateStaticLayoutViews(sharedOrderData)

        // 2. Attach live data snapshot watcher channels on remote database document path
        viewModel.loadOrderDetails(currentOrderId)
    }

    private fun setupRecyclerView() {
        preparationDetailAdapter = KitchenPreparationDetailAdapter()
        binding.rvDetailItemsList.adapter = preparationDetailAdapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Tapping this completes preparation, moving the ticket forward to the pick-up counters
        binding.btnFinishReady.setOnClickListener {
            Log.i(TAG, "setupClickListeners: Mark to serve action invoked for order ID: $currentOrderId")
            viewModel.finalizeOrderToServe(currentOrderId)
        }
    }

    private fun populateStaticLayoutViews(data: KitchenOrderDetailData) {
        binding.tvDetailTitle.text = "Order #${data.orderId.takeLast(4).uppercase()}"
        binding.tvTableNo.text = data.tableName.ifEmpty { "Table N/A" }

        if (data.timestamp != null) {
            val durationMillis = System.currentTimeMillis() - data.timestamp.toDate().time
            val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
            binding.tvItemCount.text = "Elapsed time: $elapsedMinutes min"
        } else {
            binding.tvItemCount.text = "Elapsed time: 0 min"
        }
    }

    private fun observeUiState() {
        // Collect real-time tab filtered item submanifest rows arrays
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.detailUiState.collectLatest { state ->
                when (state) {
                    is KitchenOrderDetailUiState.Loading -> {
                        // Data is preloaded via argument parameters
                    }
                    is KitchenOrderDetailUiState.Success -> {
                        Log.i(TAG, "observeUiState: Real-time update list payload pulled. Display items list count: ${state.orderDetails.items.size}")
                        populateStaticLayoutViews(state.orderDetails)
                        preparationDetailAdapter.submitList(state.orderDetails.items)
                    }
                    is KitchenOrderDetailUiState.Error -> {
                        Log.e(TAG, "observeUiState: Real-time update extraction channel failed.")
                    }
                }
            }
        }

        // Catch database state update mutation transactions results
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statusUpdateAction.collectLatest { result ->
                result?.onSuccess { statusState ->
                    Log.i(TAG, "observeUiState: Database status updated to '$statusState'. Returning back to lines stream view.")
                    Toast.makeText(context, "Order is Ready! Waiter alert transmitted.", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusActionToken()
                    parentFragmentManager.popBackStack()
                }?.onFailure { exception ->
                    Toast.makeText(context, "Operation failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusActionToken()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Cleaning view binder context links layer layout models.")
        _binding = null
    }
}