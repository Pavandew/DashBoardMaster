package com.example.masterdashboard.staff_dash.waiter_screens.order.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentOrderDetailsExpansionBinding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.order.adapter.OrderDetailRowAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.order.repo.OrderDetailRepository
import com.example.masterdashboard.staff_dash.waiter_screens.order.viewModel.OrderDetailViewModel
import kotlinx.coroutines.launch

class OrderDetailExpansionFragment : Fragment() {

    companion object {
        private const val TAG = "Order_Detail_Debug"
    }

    private var _binding: FragmentOrderDetailsExpansionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderDetailViewModel by viewModels {
        OrderDetailViewModel.OrderDetailViewModelFactory(OrderDetailRepository())
    }

    private lateinit var rowAdapter: OrderDetailRowAdapter

    private var currentOrderId: String = ""
    private var passedTableName: String = ""
    private var passedStatusStr: String = ""
    private var passedOrderTime: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailsExpansionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "📱 [FRAGMENT] OrderDetailExpansionFragment Opened")

        val sessionManager = SessionManager(requireContext())
        val managerId = sessionManager.getUid() ?: ""

        // Unpack arguments passed from previous list fragment
        currentOrderId = arguments?.getString("orderId") ?: "N/A"
        passedTableName = arguments?.getString("tableName") ?: ""
        passedStatusStr = arguments?.getString("orderStatus") ?: "PREPARING"
        passedOrderTime = arguments?.getString("orderTime") ?: ""

        Log.d(TAG, "📱 [FRAGMENT] Received Arguments -> OrderId: '$currentOrderId', Table: '$passedTableName', Status: '$passedStatusStr', Time: '$passedOrderTime'")

        setupToolbar()
        setupRowRecyclerView()
        observeSpecState()

        // Fetch detailed items payload from Firestore
        Log.d(TAG, "📱 [FRAGMENT] Triggering loadOrderSpecifications for Manager: $managerId, Order: $currentOrderId")
        viewModel.loadOrderSpecifications(
            managerId = managerId,
            orderId = currentOrderId,
            preloadedTableName = passedTableName,
            preloadedStatus = passedStatusStr,
            preloadedTime = passedOrderTime
        )
    }

    override fun onStart() {
        super.onStart()
        (activity as? WaiterHomeActivity)?.hideBottomNavigation()
    }

    private fun setupToolbar() {
        val toolbar = binding.orderDetailToolbar
        toolbar.tvToolbarTitle.text = getString(R.string.order_details)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRowRecyclerView() {
        rowAdapter = OrderDetailRowAdapter()
        binding.rvExpandedOrderItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = rowAdapter
        }
    }

    private fun observeSpecState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    if (state.isLoading) {
                        Log.d(TAG, "📱 [FRAGMENT] UI State -> Loading items...")
                        binding.pbOrderDetailLoading.visibility = View.VISIBLE
                        binding.nsvContentContainer.visibility = View.INVISIBLE
                        binding.btnMarkAsServed.visibility = View.INVISIBLE
                    } else {
                        Log.i(TAG, "📱 [FRAGMENT] UI State -> Rendered! Displaying ${state.items.size} dish items for Table '${state.tableName}'")

                        binding.pbOrderDetailLoading.visibility = View.GONE
                        binding.nsvContentContainer.visibility = View.VISIBLE
                        binding.btnMarkAsServed.visibility = View.VISIBLE

                        // Table Name Formatting
                        val formattedTable = if (state.tableName.startsWith("Table", ignoreCase = true)) {
                            state.tableName
                        } else {
                            "Table ${state.tableName}"
                        }

                        binding.tvExpandedTableId.text = formattedTable
                        binding.tvExpandedOrderId.text = state.orderId
                        binding.tvExpandedTimestamp.text = state.timeStamp

                        // Status Tag Configuration
                        when (state.status) {
                            ActiveOrderStatus.PENDING -> {
                                binding.tvExpandedStatusTag.text = "• Pending"
                                binding.tvExpandedStatusTag.setBackgroundResource(R.drawable.bg_status_preparing)
                                binding.tvExpandedStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_occupied))
                            }
                            ActiveOrderStatus.PREPARING -> {
                                binding.tvExpandedStatusTag.text = "• Preparing"
                                binding.tvExpandedStatusTag.setBackgroundResource(R.drawable.bg_status_preparing)
                                binding.tvExpandedStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_occupied))
                            }
                            ActiveOrderStatus.READY -> {
                                binding.tvExpandedStatusTag.text = "• Ready"
                                binding.tvExpandedStatusTag.setBackgroundResource(R.drawable.bg_status_ready)
                                binding.tvExpandedStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_free))
                            }
                            ActiveOrderStatus.SERVED -> {
                                binding.tvExpandedStatusTag.text = "• Served"
                                binding.tvExpandedStatusTag.setBackgroundResource(R.drawable.bg_status_served)
                                binding.tvExpandedStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_billing))
                            }
                            ActiveOrderStatus.BILLING -> {
                                binding.tvExpandedStatusTag.text = "• Billing"
                                binding.tvExpandedStatusTag.setBackgroundResource(R.drawable.bg_status_preparing) // Reuse or add new bg
                                binding.tvExpandedStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_occupied))
                            }
                            ActiveOrderStatus.PAID -> {
                                binding.tvExpandedStatusTag.text = "• Paid"
                                binding.tvExpandedStatusTag.setBackgroundResource(R.drawable.bg_status_ready)
                                binding.tvExpandedStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_free))
                            }
                            else -> {
                                binding.tvExpandedStatusTag.text = "• ${state.status.name}"
                                binding.tvExpandedStatusTag.setBackgroundResource(R.drawable.bg_status_preparing)
                                binding.tvExpandedStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_occupied))
                            }
                        }

                        // Bottom Actions Logic
                        when (state.status) {
                            ActiveOrderStatus.PENDING, ActiveOrderStatus.PREPARING, ActiveOrderStatus.READY -> {
                                binding.btnMarkAsServed.visibility = View.VISIBLE
                                binding.llAfterServedActions.visibility = View.GONE
                            }
                            ActiveOrderStatus.SERVED -> {
                                binding.btnMarkAsServed.visibility = View.GONE
                                binding.llAfterServedActions.visibility = View.VISIBLE
                            }
                            else -> {
                                binding.btnMarkAsServed.visibility = View.GONE
                                binding.llAfterServedActions.visibility = View.GONE
                            }
                        }

                        // Submit items list to adapter
                        rowAdapter.submitList(state.items)

                        // Financial totals
                        binding.tvExpandedSubtotal.text = "${getString(R.string.currency_symbol)} ${state.subtotal}"
                        binding.tvExpandedGst.text = "${getString(R.string.currency_symbol)} ${String.format("%.2f", state.gstAmount)}"
                        binding.tvExpandedGrandTotal.text = "${getString(R.string.currency_symbol)} ${String.format("%.2f", state.grandTotal)}"

                        // Mark as Served Action
                        binding.btnMarkAsServed.setOnClickListener {
                            val managerId = SessionManager(requireContext()).getUid() ?: ""
                            viewModel.finalizeOrderAsServed(
                                managerId = managerId,
                                floorId = state.floorId,
                                tableId = state.tableId,
                                orderDocId = state.documentId
                            ) {
                                Toast.makeText(context, "Order marked as Served!", Toast.LENGTH_SHORT).show()
                                // Re-load to update UI
                                viewModel.loadOrderSpecifications(managerId, currentOrderId, passedTableName, "SERVED", passedOrderTime)
                            }
                        }

                        // Add More Items Action
                        binding.btnAddMoreItems.setOnClickListener {
                            Log.d(TAG, "📱 [FRAGMENT] 'Add Items' clicked. Navigating to OrderTakingFragment for Table: ${state.tableName}")
                            
                            val bundle = Bundle().apply {
                                putString("tableId", state.tableId)
                                putString("tableName", state.tableName)
                                putString("floorId", state.floorId)
                                putString("status", "OCCUPIED")
                                putString("existingOrderDocId", state.documentId)
                                putString("existingOrderId", state.orderId)
                            }

                            val orderTakingFragment = com.example.masterdashboard.staff_dash.waiter_screens.table.views.OrderTakingFragment().apply {
                                arguments = bundle
                            }

                            parentFragmentManager.beginTransaction()
                                .replace(R.id.waiter_fragment_container, orderTakingFragment)
                                .addToBackStack(null)
                                .commit()
                        }

                        // Generate Bill Action
                        binding.btnGenerateBill.setOnClickListener {
                            val managerId = SessionManager(requireContext()).getUid() ?: ""
                            Log.d(TAG, "📱 [FRAGMENT] 'Generate Bill' clicked for DocId: '${state.documentId}'")

                            viewModel.finalizeOrderAsBilling(
                                managerId = managerId,
                                floorId = state.floorId,
                                tableId = state.tableId,
                                orderDocId = state.documentId
                            ) {
                                Toast.makeText(context, "Order sent to Billing!", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.popBackStack()
                            }
                        }
                    }

                    state.errorMessage?.let { error ->
                        Log.e(TAG, "📱 [FRAGMENT] Error displayed to user: $error")
                        binding.pbOrderDetailLoading.visibility = View.GONE
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
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