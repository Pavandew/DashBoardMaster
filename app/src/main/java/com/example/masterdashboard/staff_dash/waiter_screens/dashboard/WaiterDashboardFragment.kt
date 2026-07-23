package com.example.masterdashboard.staff_dash.waiter_screens.dashboard

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
import com.example.masterdashboard.databinding.FragmentWaiterDashboardBinding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import kotlinx.coroutines.launch

class WaiterDashboardFragment : Fragment() {

    companion object {
        private const val TAG = "WaiterDashboardFragment"
    }

    private var _binding: FragmentWaiterDashboardBinding? = null
    private val binding get() = _binding!!

    // SessionManager instance to retrieve logged-in waiter session details
    private val sessionManager by lazy { SessionManager(requireContext()) }

    private val viewModel: WaiterDashboardViewModel by viewModels {
        WaiterDashboardViewModel.WaiterDashboardViewModelFactory()
    }

    private lateinit var recentActivityAdapter: RecentActivityAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaiterDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Initializing Waiter Dashboard bindings.")

        setupToolbar()
        setupCardStaticLabels()
        setupCardClickListeners()
        setupRecentActivityRecyclerView()
        observeDashboardUiState()

        val currentManagerId = sessionManager.getUid()
        viewModel.observeRealtimeDashboard(currentManagerId)
    }

    override fun onStart() {
        super.onStart()
        (activity as? WaiterHomeActivity)?.showBottomNavigation()
    }

    /**
     * Set dynamic greeting with Waiter Name and Emoji 👋
     */
    private fun setupToolbar() {
        val toolbar = binding.staffDashboardToolbar

        // Dynamic name mapping
        val waiterName = sessionManager.getUserName()?.ifEmpty { "Waiter" }
        toolbar.toolbarTvTitle.text = "Hello $waiterName 👋"

        toolbar.toolbarImgNotification.setOnClickListener {
            Log.d(TAG, "Notification icon clicked in toolbar. Navigating to AlertFragment.")
            navigateToAlertFragment()
        }
    }

    private fun setupCardStaticLabels() {
        binding.staffDashActiveTableCard.cardTvTitle.text = "Active Tables"
        binding.staffDashActiveTableCard.cardSubtitle.text = "Occupied Floor Tables"
        binding.staffDashActiveTableCard.cardIcon.setImageResource(R.drawable.ic_restaurant_24dp)

        binding.staffDashPendOrderCard.cardTvTitle.text = "Pending Orders"
        binding.staffDashPendOrderCard.cardSubtitle.text = "Awaiting Preparation"
        binding.staffDashPendOrderCard.cardIcon.setImageResource(R.drawable.bg_status_active)

        binding.staffDashReadyOrderCard.cardTvTitle.text = "Ready to Serve"
        binding.staffDashReadyOrderCard.cardSubtitle.text = "Dishes Ready at Pass"
        binding.staffDashReadyOrderCard.cardIcon.setImageResource(R.drawable.bg_status_served)

        binding.dashCardTotalAdmin.cardTvTitle.text = "Total Served"
        binding.dashCardTotalAdmin.cardSubtitle.text = "Shift Completed Orders"
        binding.dashCardTotalAdmin.cardIcon.setImageResource(R.drawable.ic_add_circle_24dp)
    }

    private fun setupCardClickListeners() {
        binding.staffDashActiveTableCard.root.setOnClickListener { Log.d(TAG, "Active Tables card clicked") }
        binding.staffDashPendOrderCard.root.setOnClickListener { Log.d(TAG, "Pending Orders card clicked") }
        binding.staffDashReadyOrderCard.root.setOnClickListener { Log.d(TAG, "Ready Orders card clicked") }
        binding.dashCardTotalAdmin.root.setOnClickListener { Log.d(TAG, "Total Served card clicked") }
        binding.dashTvViewAll.setOnClickListener {
            Toast.makeText(context, "Opening full activity log...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecentActivityRecyclerView() {
        recentActivityAdapter = RecentActivityAdapter()
        binding.recyclerRecentActivity.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentActivityAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeDashboardUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.staffDashActiveTableCard.cardTvCount.text = state.activeTablesCount
                    binding.staffDashPendOrderCard.cardTvCount.text = state.pendingOrdersCount
                    binding.staffDashReadyOrderCard.cardTvCount.text = state.readyOrdersCount
                    binding.dashCardTotalAdmin.cardTvCount.text = state.totalServedCount

                    recentActivityAdapter.submitList(state.logsList)
                }
            }
        }
    }

    /**
     * 🚀 Helper function to handle fragment transition to AlertFragment
     * delegating to the activity to handle bottom navigation synchronization.
     */
    private fun navigateToAlertFragment() {
        (activity as? WaiterHomeActivity)?.openAlerts()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}