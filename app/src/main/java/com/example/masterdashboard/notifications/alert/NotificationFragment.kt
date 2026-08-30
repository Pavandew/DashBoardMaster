package com.example.masterdashboard.notifications.alert

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.example.masterdashboard.utils.NavigationUtils
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * Fragment responsible for displaying the role-filtered Notification Feed.
 */
class NotificationFragment : Fragment() {

    companion object {
        private const val TAG = "NotificationFragment"
    }

    private var _binding: FragmentWaiterNotificationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationViewModel by viewModels {
        NotificationViewModel.NotificationViewModelFactory(SessionManager(requireContext()))
    }

    private lateinit var notificationAdapter: NotificationAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i(TAG, "Notification permission granted.")
            viewModel.observeNotificationStream()
        } else {
            Log.w(TAG, "Notification permission denied.")
            Toast.makeText(context, "Please enable notifications to receive alerts", Toast.LENGTH_LONG).show()
            // Still observing because in-app alerts don't technically require the POST_NOTIFICATIONS permission,
            // but the user experience is better with push enabled.
            viewModel.observeNotificationStream()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaiterNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Notification screen initialized.")

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Notification permission already granted.")
                    viewModel.observeNotificationStream()
                }
                else -> {
                    Log.d(TAG, "Requesting notification permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Below Android 13, permission is granted at install time
            viewModel.observeNotificationStream()
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as? WaiterHomeActivity)?.showBottomNavigation()
        (activity as? KitchenHomeActivity)?.showBottomNavigation()
        (activity as? CashierHomeActivity)?.showBottomNavigation()
    }

    private fun setupToolbar() {
        val toolbar = binding.staffAlertToolbar
        toolbar.tvToolbarTitle.text = getString(R.string.title_live_alerts)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onCardClicked = { clickedItem ->
                Log.d(TAG, "Alert card clicked: id=${clickedItem.id} orderId=${clickedItem.orderId} orderDocPath=${clickedItem.orderDocPath}")
                // 1. Mark as read and toggle expansion locally
                viewModel.handleNotificationClicked(clickedItem)

                // 2. Perform contextual navigation if applicable
                if (clickedItem.orderId.isNotEmpty() || clickedItem.orderDocPath.isNotEmpty()) {
                    navigateToContextDetails(clickedItem)
                }
            },
            onAcceptClicked = { targetItem ->
                Log.d(TAG, "Accept clicked for alert: ${targetItem.id}")
                viewModel.updateRequestStatus(targetItem.id, RequestStatus.ACCEPTED)
                Toast.makeText(context, "Request Accepted", Toast.LENGTH_SHORT).show()
            },
            onDoneClicked = { targetItem ->
                Log.d(TAG, "Done clicked for alert: ${targetItem.id}")
                viewModel.updateRequestStatus(targetItem.id, RequestStatus.DONE)
                Toast.makeText(context, "Task Completed", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvAlertsFeedContainer.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
    }

    /**
     * Intelligently routes the user to the correct screen based on the notification content and their role.
     */
    private fun navigateToContextDetails(notification: AppNotificationModel) {
        val sessionManager = SessionManager(requireContext())
        val role = sessionManager.getRole().lowercase().trim()
        val isManager = role == "manager" || role == "owner_single" || role == "owner_multi"

        Log.d(TAG, "Navigating based on role: $role, notificationTitle: ${notification.title}")

        when {
            // A. KITCHEN / CHEF - Navigation to Order Details
            role == "kitchen" || role == "chef" -> {
                if (notification.orderId.isEmpty()) {
                    Log.w(TAG, "Cannot navigate: orderId is empty for kitchen notification")
                    return
                }
                Log.i(TAG, "Navigating to kitchen detail for orderId=${notification.orderId}")
                val kitchenData = KitchenOrderDetailData().apply {
                    orderId = notification.orderId
                    docPath = notification.orderDocPath
                    tableName = notification.tableId
                }

                val fragment = KitchenOrderDetailFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("ORDER_DATA_KEY", kitchenData)
                    }
                }
                switchFragment(fragment)
            }

            // B. BILLING / CASHIER - Navigation to Settlement screen
            role == "billing" || role == "cashier" -> {
                if (notification.orderId.isEmpty()) {
                    Log.w(TAG, "Cannot navigate: orderId is empty for billing notification")
                    return
                }
                Log.i(TAG, "Navigating to billing detail for orderId=${notification.orderId}")
                val cashierOrder = CashierBillingOrderModel(
                    orderId = notification.orderId,
                    tableName = notification.tableId,
                    docPath = notification.orderDocPath
                )
                val fragment = CashierSettleBillFragment.newInstance(cashierOrder)
                switchFragment(fragment)
            }

            // C. WAITER - Navigation to Order Details
            role == "waiter" || role == "waiter_staff" -> {
                if (notification.orderId.isEmpty()) {
                    Log.w(TAG, "Cannot navigate: orderId is empty for waiter notification")
                    return
                }
                Log.i(TAG, "Navigating to waiter order detail for orderId=${notification.orderId}")
                val fragment = OrderDetailExpansionFragment().apply {
                    arguments = Bundle().apply {
                        putString("orderId", notification.orderId)
                        putString("tableName", notification.tableId)
                        putString("orderStatus", "PENDING")
                        putString("orderTime", notification.timeStamp)
                    }
                }
                switchFragment(fragment)
            }

            // D. MANAGER - Contextual navigation based on message
            isManager -> {
                when {
                    notification.title.contains("Inventory", true) -> {
                        // TODO: Navigate to Inventory Fragment when implemented
                        Log.i(TAG, "Manager clicked inventory alert. Stay on current page or route to Inventory.")
                        Toast.makeText(context, "Redirecting to Inventory Management...", Toast.LENGTH_SHORT).show()
                    }
                    notification.orderId.isNotEmpty() -> {
                        Log.i(TAG, "Manager navigating to billing summary for orderId=${notification.orderId}")
                        val cashierOrder = CashierBillingOrderModel(
                            orderId = notification.orderId,
                            tableName = notification.tableId,
                            docPath = notification.orderDocPath
                        )
                        val fragment = CashierSettleBillFragment.newInstance(cashierOrder)
                        switchFragment(fragment)
                    }
                }
            }
        }
    }

    private fun switchFragment(fragment: Fragment) {
        Log.d(TAG, "Switching fragment to ${fragment::class.java.simpleName}")
        val containerId = NavigationUtils.getHostContainerId(activity)
        
        if (containerId == 0) return

        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        Log.d(TAG, "Loading notifications state started")
                        binding.pbAlertsLoadingIndicator.visibility = View.VISIBLE
                        binding.rvAlertsFeedContainer.visibility = View.GONE
                    } else {
                        Log.d(TAG, "Loading notifications state finished with ${state.notifications.size} items")
                        binding.pbAlertsLoadingIndicator.visibility = View.GONE
                        binding.rvAlertsFeedContainer.visibility = View.VISIBLE
                        notificationAdapter.submitList(state.notifications)
                    }

                    state.errorMessage?.let { error ->
                        Log.e(TAG, "Notification UI error: $error")
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
