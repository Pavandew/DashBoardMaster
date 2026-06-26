package com.example.masterdashboard.staff_dash.waiter_screens.order.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentOrderDetailsExpansionBinding
import com.example.masterdashboard.staff_dash.waiter_screens.StaffHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.adapter.OrderDetailRowAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.repo.OrderDetailRepository
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.viewModel.OrderDetailViewModel
import kotlinx.coroutines.launch

class OrderDetailExpansionFragment : Fragment() {

    companion object {
        private const val TAG = "OrderDetailExpansion"
    }

    private var _binding: FragmentOrderDetailsExpansionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderDetailViewModel by viewModels {
        OrderDetailViewModel.OrderDetailViewModelFactory(OrderDetailRepository())
    }

    private lateinit var rowAdapter: OrderDetailRowAdapter
    private var currentOrderId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailsExpansionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: OrderDetailExpansionFragment Opened")
        Log.d(TAG, "onViewCreated: Binding data components onto detailed specification layouts.")

        currentOrderId = arguments?.getString("orderId") ?: "N/A"

        setupToolbar()
        setupRowRecyclerView()
        observeSpecState()

        viewModel.loadOrderSpecifications(currentOrderId)
    }

    override fun onStart() {
        super.onStart()
        // Kept hidden to give premium focus onto item checkout bills
        (activity as? StaffHomeActivity)?.hideBottomNavigation()
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
                        // 1. Show loader and hide the structural layout data elements
                        binding.pbOrderDetailLoading.visibility = View.VISIBLE
                        binding.nsvContentContainer.visibility = View.INVISIBLE
                        binding.btnMarkAsServed.visibility = View.INVISIBLE
                    } else {
                        // 2. Hide loader and restore view panel access containers
                        binding.pbOrderDetailLoading.visibility = View.GONE
                        binding.nsvContentContainer.visibility = View.VISIBLE
                        binding.btnMarkAsServed.visibility = View.VISIBLE

                        // Bind Header Details Card Content
                        binding.tvExpandedTableId.text = "${getString(R.string.tables)} ${state.tableId}"
                        binding.tvExpandedOrderId.text = state.orderId
                        binding.tvExpandedTimestamp.text = state.timeStamp

                        // Set dynamic text tag states
                        when (state.status) {
                            ActiveOrderStatus.PREPARING -> {
                                binding.tvExpandedStatusTag.text = "• Preparing"
                                binding.tvExpandedStatusTag.setBackgroundResource(R.drawable.bg_status_preparing)
                                binding.tvExpandedStatusTag.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_occupied))
                            }
                            ActiveOrderStatus.READY -> {
                                binding.tvExpandedStatusTag.text = "• Ready"
                                binding.tvExpandedStatusTag.setBackgroundResource(R.drawable.bg_status_ready)
                                binding.tvExpandedStatusTag.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_free))
                            }
                            ActiveOrderStatus.SERVED -> {
                                binding.tvExpandedStatusTag.text = "• Served"
                                binding.tvExpandedStatusTag.setBackgroundResource(R.drawable.bg_status_served)
                                binding.tvExpandedStatusTag.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_billing))
                            }
                        }

                        // Submit items down to nested list view rows
                        rowAdapter.submitList(state.items)

                        // Bind aggregation metrics
                        binding.tvExpandedSubtotal.text = "${getString(R.string.currency_symbol)} ${state.subtotal}"
                        binding.tvExpandedGst.text = "${getString(R.string.currency_symbol)} ${String.format("%.2f", state.gstAmount)}"
                        binding.tvExpandedGrandTotal.text = "${getString(R.string.currency_symbol)} ${String.format("%.2f", state.grandTotal)}"

                        // Dynamic handling for Primary button action click workflows
                        binding.btnMarkAsServed.setOnClickListener {
                            viewModel.finalizeOrderAsServed(currentOrderId) {
                                Toast.makeText(context, "Order updated to Served successfully!", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.popBackStack()
                            }
                        }
                    }

                    state.errorMessage?.let {
                        binding.pbOrderDetailLoading.visibility = View.GONE
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
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