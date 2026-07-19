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
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderDetailUiState
import com.example.masterdashboard.staff_dash.kitchen_screens.viewModel.KitchenOrderDetailViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KitchenOrderDetailFragment : Fragment(R.layout.fragment_kitchen_order_detail) {

    companion object {
        private const val TAG = "KitchenOrderDetailFrag"
    }

    private var _binding: FragmentKitchenOrderDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: KitchenOrderDetailViewModel by viewModels()
    private lateinit var detailItemsAdapter: KitchenDetailItemAdapter
    private var currentOrderId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentKitchenOrderDetailBinding.bind(view)

        // 1. Fetch the FULL shared object passed directly from the previous list fragment
        val initialOrderData = arguments?.getSerializable("ORDER_DATA_KEY") as? KitchenOrderDetailData

        if (initialOrderData == null) {
            Log.e(TAG, "onViewCreated: Aborting view creation because passed shared order data object is missing.")
            Toast.makeText(context, "Error: Invalid Order Data", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        currentOrderId = initialOrderData.orderId
        Log.i(TAG, "onViewCreated: Shared data extracted successfully. Showing Table: ${initialOrderData.tableName} instantly.")

        setupRecyclerView()
        setupClickListeners()
        observeUiState()

        // 2. IMMEDIATELY populate the views (Pure white background + black text elements appear with zero loading delay)
        populateUi(initialOrderData)

        // 3. Connect real-time Firebase listener to monitor background updates (e.g., if waiter adds more food)
        Log.d(TAG, "onViewCreated: Connecting background live stream listener to monitor changes on order ID: $currentOrderId")
        viewModel.loadOrderDetails(currentOrderId)
    }

    private fun setupRecyclerView() {
        detailItemsAdapter = KitchenDetailItemAdapter()
        binding.rvDetailItemsList.adapter = detailItemsAdapter
    }

    /**
     * Binds the white/black light theme layout views instantly to our data fields.
     */
    private fun populateUi(data: KitchenOrderDetailData) {
        binding.tvDetailTitle.text = "Order #${data.orderId.takeLast(4).uppercase()}"
        binding.tvTableNo.text = data.tableName
        binding.tvItemCount.text = "${data.items.size} Items"
        binding.tvMasterNoteText.text = data.orderNote.ifEmpty { "No custom instructions note." }

        // 🔄 STATE MACHINE ADAPTER RECONFIGURATION CONTEXT UPDATE LINK
//        detailItemsAdapter.updateOrderStatusContext(data.status)

        // Submit the items down to your row list views manifest rows canvas
        detailItemsAdapter.submitList(data.items)
        updateButtonVisibilities(data.status)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            Log.d(TAG, "setupClickListeners: Return to stream operation invoked via click events.")
            parentFragmentManager.popBackStack()
        }

        binding.btnAccept.setOnClickListener {
            Log.i(TAG, "setupClickListeners: Accept clicked. Altering orderId [$currentOrderId] status mapping -> 'Preparing'")
            viewModel.updateTicketStatus(currentOrderId, "Preparing")
        }

        binding.btnReject.setOnClickListener {
            Log.w(TAG, "setupClickListeners: Reject clicked. Altering orderId [$currentOrderId] status mapping -> 'Rejected'")
            viewModel.updateTicketStatus(currentOrderId, "Rejected")
        }

        binding.btnFinishReady.setOnClickListener {
            Log.i(TAG, "setupClickListeners: Finish clicked. Altering orderId [$currentOrderId] status mapping -> 'Ready'")
            viewModel.updateTicketStatus(currentOrderId, "Ready")
        }
    }

    private fun observeUiState() {
        // Track unique document configurations arriving directly out of Firestore databases
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.detailUiState.collectLatest { state ->
                when (state) {
                    is KitchenOrderDetailUiState.Loading -> {
                        // Data is already loaded initially from the bundle object, no skeleton state loading is needed
                    }
                    is KitchenOrderDetailUiState.Success -> {
                        Log.i(TAG, "observeUiState: Background cloud update received from Firebase for Table: ${state.orderDetails.tableName}")
                        // Refresh elements if the waiter added items or notes dynamically in the background
                        populateUi(state.orderDetails)
                    }
                    is KitchenOrderDetailUiState.Error -> {
                        Log.e(TAG, "observeUiState: Target detail document real-time snapshot channel dropped.", state.exception)
                    }
                }
            }
        }

        // Track mutation status results securely to safely manage view exits
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statusUpdateAction.collectLatest { result ->
                result?.onSuccess { updatedStatus ->
                    Log.i(TAG, "observeUiState: Transaction update finalized completely across remote servers. Next status set -> '$updatedStatus'. Popping back out to logs context view.")
                    Toast.makeText(context, "Status updated to: $updatedStatus", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusActionToken()
                    parentFragmentManager.popBackStack()
                }?.onFailure { exception ->
                    Log.e(TAG, "observeUiState: Cloud layout field status write failed for orderId: $currentOrderId", exception)
                    Toast.makeText(context, "Action failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusActionToken()
                }
            }
        }
    }

    private fun updateButtonVisibilities(status: String) {
        when (status.lowercase()) {
            "new" -> {
                binding.layoutInitialActions.visibility = View.VISIBLE
                binding.btnFinishReady.visibility = View.GONE
            }
            "preparing" -> {
                binding.layoutInitialActions.visibility = View.GONE
                binding.btnFinishReady.visibility = View.VISIBLE
            }
            else -> {
                binding.layoutInitialActions.visibility = View.GONE
                binding.btnFinishReady.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Clearing details sheet view elements layer safely.")
        _binding = null
    }
}