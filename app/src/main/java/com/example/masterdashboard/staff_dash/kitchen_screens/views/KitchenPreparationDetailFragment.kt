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
        binding.tvDetailTitle.text = "Order #${data.orderId.takeLast(4).uppercase()}"
        binding.tvTableNo.text = data.tableName.ifEmpty { "Table N/A" }
        binding.tvDineIn.text = " • ${data.orderType}"

        // 1. Dynamic Status Badge Styling
        val normalizedStatus = data.status.lowercase().trim()
        binding.tvStatusBadge.text = data.status
        when (normalizedStatus) {
            "preparing" -> {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_preparing)
                binding.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#92400E"))
            }
            "ready" -> {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_ready)
                binding.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#15803D"))
            }
            "completed" -> {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active)
                binding.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#15803D"))
            }
            else -> {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active)
                binding.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#374151"))
            }
        }

        // 2. Button Visibility: Hide "Ready to Serve" button if status is already "Ready" or "Completed"
        if (normalizedStatus == "ready" || normalizedStatus == "completed") {
            binding.btnFinishReady.visibility = View.GONE
        } else {
            binding.btnFinishReady.visibility = View.VISIBLE
        }
        
        // Time formatting
        val ts = data.timestamp
        if (ts != null) {
            val durationMillis = System.currentTimeMillis() - ts.toDate().time
            val min = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
            val hr = TimeUnit.MILLISECONDS.toHours(durationMillis)
            val dy = TimeUnit.MILLISECONDS.toDays(durationMillis)

            val timeText = when {
                dy > 0 -> "$dy d ago"
                hr > 0 -> "$hr h ago"
                else -> "$min min ago"
            }
            binding.tvItemCount.text = "Elapsed time: $timeText"
        } else {
            binding.tvItemCount.text = "Just now"
        }
        
        binding.tvItemToPrepare.text = "Items to Prepare (${data.items.size})"
        
        preparationDetailAdapter.updateOrderStatusContext(data.status)
        preparationDetailAdapter.submitList(data.items)
        
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