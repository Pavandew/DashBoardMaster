package com.example.masterdashboard.notifications.alert

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
import com.example.masterdashboard.databinding.FragmentWaiterNotificationBinding
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.kitchen_screens.KitchenHomeActivity
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.views.KitchenOrderDetailFragment
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.example.masterdashboard.staff_dash.billing_screens.views.CashierSettleBillFragment
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.OrderDetailExpansionFragment
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

class StaffNotificationFragment : androidx.fragment.app.Fragment() {

    companion object {
        private const val TAG = "StaffAlertsFragment"
    }

    private var _binding: FragmentWaiterNotificationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlertsViewModel by viewModels {
        AlertsViewModel.AlertsViewModelFactory(SessionManager(requireContext()))
    }

    private lateinit var alertsAdapter: AlertsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaiterNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: WaiterNotificationFragment Opened")

        setupToolbarLayout()
        setupAlertsRecyclerView()
        observeAlertsStateFlow()

        viewModel.fetchLiveAlertsFeed()
    }

    override fun onStart() {
        super.onStart()
        (activity as? WaiterHomeActivity)?.showBottomNavigation()
        (activity as? KitchenHomeActivity)?.showBottomNavigation()
        (activity as? CashierHomeActivity)?.showBottomNavigation()
    }

    private fun setupToolbarLayout() {
        val toolbar = binding.staffAlertToolbar
        toolbar.tvToolbarTitle.text = getString(R.string.alerts)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE

        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupAlertsRecyclerView() {
        alertsAdapter = AlertsAdapter(
            onCardClicked = { clickedItem ->
                // If the notification has order info, navigate to details on click
                if (clickedItem.orderId.isNotEmpty() || clickedItem.orderDocPath.isNotEmpty()) {
                    navigateToOrderDetails(clickedItem)
                } else {
                    // Fallback to expansion if it's actionable but has no direct order link
                    viewModel.handleCardExpansionToggle(clickedItem)
                }
            },
            onAcceptClicked = { targetItem ->
                viewModel.updateAlertRequestStatus(targetItem.id, RequestStatus.ACCEPTED)
                Toast.makeText(context, "${targetItem.title} Request Accepted", Toast.LENGTH_SHORT).show()
            },
            onDoneClicked = { targetItem ->
                viewModel.updateAlertRequestStatus(targetItem.id, RequestStatus.DONE)
                Toast.makeText(context, "${targetItem.title} Task Cleared", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvAlertsFeedContainer.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = alertsAdapter
        }
    }

    private fun navigateToOrderDetails(alert: StaffAlertItem) {
        if (alert.orderId.isEmpty() && alert.orderDocPath.isEmpty()) {
            Log.w(TAG, "Cannot navigate: No order ID or DocPath in notification")
            return
        }

        val sessionManager = SessionManager(requireContext())
        val role = sessionManager.getRole().lowercase().trim()

        when {
            // 1. KITCHEN ROLE -> Kitchen Detail Screen
            role == "kitchen" || role == "chef" -> {
                val kitchenData = KitchenOrderDetailData().apply {
                    orderId = alert.orderId
                    docPath = alert.orderDocPath
                    tableName = alert.tableId
                }
                
                val fragment = KitchenOrderDetailFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("ORDER_DATA_KEY", kitchenData)
                    }
                }
                switchFragment(fragment)
            }

            // 2. CASHIER / BILLING ROLE -> Settlement Screen
            role == "billing" || role == "cashier" -> {
                val cashierOrder = CashierBillingOrderModel(
                    orderId = alert.orderId,
                    tableName = alert.tableId,
                    docPath = alert.orderDocPath
                )
                val fragment = CashierSettleBillFragment.newInstance(cashierOrder)
                switchFragment(fragment)
            }

            // 3. WAITER ROLE -> Order Details Expansion
            role == "waiter" || role == "waiter_staff" -> {
                val fragment = OrderDetailExpansionFragment().apply {
                    arguments = Bundle().apply {
                        putString("orderId", alert.orderId)
                        putString("tableName", alert.tableId)
                        putString("orderStatus", "PENDING") // Will be updated by real-time listener
                        putString("orderTime", alert.timeStamp)
                    }
                }
                switchFragment(fragment)
            }
        }
    }

    private fun switchFragment(fragment: Fragment) {
        val containerId = when (activity) {
            is WaiterHomeActivity -> R.id.waiter_fragment_container
            is KitchenHomeActivity -> R.id.kitchen_fragment_container
            is CashierHomeActivity -> R.id.billing_fragment_container
            else -> return
        }

        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun observeAlertsStateFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    if (state.isLoading) {
                        binding.pbAlertsLoadingIndicator.visibility = View.VISIBLE
                        binding.rvAlertsFeedContainer.visibility = View.GONE
                    } else {
                        binding.pbAlertsLoadingIndicator.visibility = View.GONE
                        binding.rvAlertsFeedContainer.visibility = View.VISIBLE
                        alertsAdapter.submitList(state.alertsList)
                    }

                    state.errorMessage?.let { error ->
                        binding.pbAlertsLoadingIndicator.visibility = View.GONE
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
