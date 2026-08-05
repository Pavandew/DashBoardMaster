package com.example.masterdashboard.staff_dash.kitchen_screens.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentKitchenOrderDetailBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenDetailItemAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderDetailUiState
import com.example.masterdashboard.staff_dash.kitchen_screens.utils.KitchenRejectionDialogHelper
import com.example.masterdashboard.staff_dash.kitchen_screens.viewModel.KitchenOrderDetailViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class KitchenOrderDetailFragment : Fragment(R.layout.fragment_kitchen_order_detail) {

    companion object {
        private const val TAG = "KitchenOrderDetailFrag"
    }

    private var _binding: FragmentKitchenOrderDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: KitchenOrderDetailViewModel by viewModels()
    private lateinit var detailItemsAdapter: KitchenDetailItemAdapter
    private var currentDocPath: String = ""
    private var currentOrderData: KitchenOrderDetailData? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentKitchenOrderDetailBinding.bind(view)

        val initialOrderData = arguments?.getSerializable("ORDER_DATA_KEY") as? KitchenOrderDetailData

        if (initialOrderData == null) {
            Log.e(TAG, "onViewCreated: Aborting because shared order data object is missing.")
            Toast.makeText(context, "Error: Invalid Order Data", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        currentDocPath = initialOrderData.docPath
        currentOrderData = initialOrderData
        Log.i(TAG, "onViewCreated: Showing Table: ${initialOrderData.tableName} instantly.")

        setupRecyclerView()
        setupClickListeners()
        observeUiState()

        // Populate initially from the passed object
        populateUi(initialOrderData)

        // Connect real-time Firebase listener using the captured docPath
        if (currentDocPath.isNotEmpty()) {
            viewModel.loadOrderDetails(currentDocPath)
        }
    }

    private fun setupRecyclerView() {
        detailItemsAdapter = KitchenDetailItemAdapter()
        binding.rvDetailItemsList.adapter = detailItemsAdapter
    }

    private fun populateUi(data: KitchenOrderDetailData) {
        currentOrderData = data
        binding.tvDetailTitle.text = "Order #${data.orderId.takeLast(4).uppercase()}"
        binding.tvTableNo.text = data.tableName
        
        binding.tvDineIn1.text = " • ${data.orderType}"
        
        val normalizedStatus = data.status.lowercase().trim()
        Log.d(TAG, "populateUi: status='$normalizedStatus', total items=${data.items.size}")

        // Only show NEW items (Delta) if the ticket is currently in the Order Log (New/Pending/Paid)
        val itemsToShow = if (normalizedStatus == "new" || normalizedStatus == "pending" || normalizedStatus == "paid") {
            val filtered = data.items.filter { it.quantity > it.orderedQuantity }
            Log.d(TAG, "populateUi: New items filter: ${filtered.size} items meet (qty > orderedQty) criteria.")
            filtered
        } else {
            // For Workstation (Preparing), show the full list
            Log.d(TAG, "populateUi: Workstation mode - showing all ${data.items.size} items.")
            data.items
        }

        // NEW: If filtered list is empty but original list is not, it means orderedQuantity caught up to quantity.
        // In such cases, if we are in "New" status, we should show at least the latest item.
        val finalItems = if (itemsToShow.isEmpty() && data.items.isNotEmpty()) {
            data.items
        } else {
            itemsToShow
        }

        binding.tvItemCount.text = "${finalItems.size} Items"
        binding.tvMasterNoteText.text = data.specialNotes.ifEmpty { "No custom instructions note." }

        // Set Relative Time
        val ts = data.timestamp
        if (ts != null) {
            val durationMillis = System.currentTimeMillis() - ts.toDate().time
            val min = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(durationMillis)
            val hr = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(durationMillis)
            val dy = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(durationMillis)

            binding.tvTime.text = when {
                dy > 0 -> "$dy d ago"
                hr > 0 -> "$hr h ago"
                else -> "$min min ago"
            }
        } else {
            binding.tvTime.text = "Just now"
        }

        detailItemsAdapter.submitList(finalItems)
        updateButtonVisibilities(data.status)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnAccept.setOnClickListener {
            viewModel.updateTicketStatus(currentDocPath, "Preparing")
        }

        binding.btnReject.setOnClickListener {
            currentOrderData?.let { orderData ->
                val helper = KitchenRejectionDialogHelper(requireContext(), layoutInflater)
                helper.showRejectionDialog(orderData, object : KitchenRejectionDialogHelper.RejectionListener {
                    override fun onFullRejection(reason: String) {
                        viewModel.updateTicketStatus(currentDocPath, "Rejected", reason)
                    }

                    override fun onPartialRejection(remainingItems: List<OrderDetailItem>, reason: String) {
                        viewModel.rejectSpecificItems(currentDocPath, remainingItems, reason)
                    }
                })
            }
        }
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
                        Log.e(TAG, "observeUiState: Snapshot error", state.exception)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statusUpdateAction.collectLatest { result ->
                result?.onSuccess { updatedStatus ->
                    Toast.makeText(context, "Status updated to: $updatedStatus", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusActionToken()
                    parentFragmentManager.popBackStack()
                }?.onFailure { exception ->
                    Toast.makeText(context, "Action failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusActionToken()
                }
            }
        }
    }

    private fun updateButtonVisibilities(status: String) {
        val normalizedStatus = status.lowercase().trim()
        Log.i(TAG, "updateButtonVisibilities: Determining visibility for status: [$normalizedStatus]")
        
        when (normalizedStatus) {
            "new", "pending", "paid" -> {
                binding.layoutActionFooter.visibility = View.VISIBLE
                binding.layoutInitialActions.visibility = View.VISIBLE
                Log.d(TAG, "updateButtonVisibilities: Displaying [Accept/Reject] for new ticket.")
            }
            else -> {
                binding.layoutInitialActions.visibility = View.GONE
                binding.layoutActionFooter.visibility = View.GONE
                Log.w(TAG, "updateButtonVisibilities: Hiding all buttons for finalized status: [$normalizedStatus]")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}