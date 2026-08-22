package com.example.masterdashboard.staff_dash.kitchen_screens.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentKitchenPreparationDetailBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenPreparationDetailAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderDetailUiState
import com.example.masterdashboard.staff_dash.kitchen_screens.viewModel.KitchenPreparationDetailViewModel
import com.example.masterdashboard.staff_dash.utils.StatusUIUtils
import com.example.masterdashboard.staff_dash.utils.TimeUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KitchenPreparationDetailFragment : Fragment(R.layout.fragment_kitchen_preparation_detail) {

    companion object {
        private const val TAG = "KitchenPrepDetailFrag"
    }

    private var _binding: FragmentKitchenPreparationDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: KitchenPreparationDetailViewModel by viewModels()
    private lateinit var preparationDetailAdapter: KitchenPreparationDetailAdapter
    private var currentDocPath: String = ""
    
    // Tracking selected items for partial preparation
    private val selectedItems = mutableSetOf<OrderDetailItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentKitchenPreparationDetailBinding.bind(view)

        val sharedOrderData = arguments?.getSerializable("ORDER_DATA_KEY") as? KitchenOrderDetailData

        if (sharedOrderData == null) {
            Log.e(TAG, "onViewCreated: Shared workstation order object is missing.")
            Toast.makeText(context, "Error: Invalid Order Data", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        currentDocPath = sharedOrderData.docPath
        Log.i(TAG, "onViewCreated: Workstation details for path: [$currentDocPath]")

        setupRecyclerView()
        setupClickListeners()
        observeUiState()

        // Populate initially
        populateUi(sharedOrderData)

        // Attach live listener
        if (currentDocPath.isNotEmpty()) {
            viewModel.loadOrderDetails(currentDocPath)
        }
    }

    private fun setupRecyclerView() {
        preparationDetailAdapter = KitchenPreparationDetailAdapter { item, isChecked ->
            if (isChecked) selectedItems.add(item) else selectedItems.remove(item)
            updateButtonLabel()
        }
        binding.rvDetailItemsList.adapter = preparationDetailAdapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnFinishReady.setOnClickListener {
            if (currentDocPath.isNotEmpty()) {
                if (selectedItems.isNotEmpty()) {
                    // Mark selected as ready
                    viewModel.markItemsAsReady(currentDocPath, selectedItems.toList())
                    selectedItems.clear()
                } else {
                    // Finalize whole order
                    viewModel.finalizeOrderToServe(currentDocPath)
                }
            }
        }
    }

    private fun updateButtonLabel() {
        if (selectedItems.isNotEmpty()) {
            binding.btnFinishReady.text = "Mark ${selectedItems.size} Selected as Ready"
            binding.btnFinishReady.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // Green for partial
        } else {
            binding.btnFinishReady.text = "Finished / Ready to Serve"
            binding.btnFinishReady.setBackgroundColor(android.graphics.Color.parseColor("#6200EE")) // Original Purple
        }
    }

    private fun populateUi(data: KitchenOrderDetailData) {
        // Simpler ID display logic matching list screen
        val displayId = when {
            data.orderId.contains("-") -> data.orderId.substringAfter("-")
            data.orderId.startsWith("#") -> data.orderId.substring(1)
            else -> {
                val digits = data.orderId.filter { it.isDigit() }
                if (digits.length >= 4) digits.takeLast(4) else data.orderId.takeLast(4)
            }
        }
        
        binding.tvDetailTitle.text = "Order #$displayId"
        binding.tvTableNo.text = data.tableName.ifEmpty { "Table N/A" }
        binding.tvDineIn.text = " • ${data.orderType}"

        // 1. Dynamic Status Badge Styling using centralized Utils
        StatusUIUtils.applyStatusUI(requireContext(), binding.tvStatusBadge, data.status)

        // 2. Button Visibility: Hide "Ready to Serve" button if status is finalized
        val normalizedStatus = data.status.lowercase().trim()
        val finalizedStatuses = setOf("ready", "completed", "handed over", "handover", "served", "paid", "billing", "success")
        
        if (finalizedStatuses.contains(normalizedStatus)) {
            binding.btnFinishReady.visibility = View.GONE
        } else {
            binding.btnFinishReady.visibility = View.VISIBLE
        }
        
        // Set Relative Time using centralized TimeUtils
        binding.tvItemCount.text = TimeUtils.getRelativeTime(data.timestamp)
        
        binding.tvItemToPrepare.text = "Items to Prepare (${data.items.size})"
        
        // Sort items: New items (qty > orderedQty) first, then processed items
        val sortedItems = data.items.sortedWith(compareByDescending<OrderDetailItem> { 
            it.quantity > it.orderedQuantity 
        }.thenBy { it.itemName })

        preparationDetailAdapter.updateOrderStatusContext(data.status)
        preparationDetailAdapter.submitList(sortedItems)
        
        // Reset selection if list changes significantly or refresh happens
        updateButtonLabel()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.detailUiState.collectLatest { state ->
                when (state) {
                    is KitchenOrderDetailUiState.Loading -> {}
                    is KitchenOrderDetailUiState.Success -> {
                        populateUi(state.orderDetails)
                    }
                    is KitchenOrderDetailUiState.Error -> {
                        Log.e(TAG, "observeUiState: Update channel failed.")
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statusUpdateAction.collectLatest { result ->
                result?.onSuccess { statusState ->
                    Toast.makeText(context, statusState, Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusActionToken()
                    
                    if (statusState == "Order Ready") {
                        parentFragmentManager.popBackStack()
                    }
                }?.onFailure { exception ->
                    Toast.makeText(context, "Operation failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusActionToken()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}