package com.example.masterdashboard.staff_dash.home.dashboard

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
import com.example.masterdashboard.databinding.FragmentStaffDashboardBinding
import com.example.masterdashboard.staff_dash.home.StaffHomeActivity
import kotlinx.coroutines.launch

class StaffDashboardFragment : Fragment() {

    companion object {
        private const val TAG = "StaffDashboardFragment"
    }

    private var _binding: FragmentStaffDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StaffDashboardViewModel by viewModels {
        StaffDashboardViewModel.StaffDashboardViewModelFactory()
    }

    private lateinit var recentActivityAdapter: RecentActivityAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing Dashboard layout bindings.")

        setupToolbar()
        setupRecentActivityRecyclerView()
        observeDashboardUiState()

        viewModel.loadDashboardMetrics()
    }

    override fun onStart() {
        super.onStart()
        // Restores bottom navigation views for secondary core layout modules
        (activity as? StaffHomeActivity)?.showBottomNavigation()
    }

    private fun setupToolbar() {
        val toolbar = binding.staffDashboardToolbar
        toolbar.tvToolbarTitle.text = "Dashboard"
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgMenu.setImageResource(R.drawable.ic_menu_24dp)
        toolbar.toolbarImgMenu.visibility = View.GONE
    }

    private fun setupRecentActivityRecyclerView() {
        recentActivityAdapter = RecentActivityAdapter()
        binding.recyclerRecentActivity.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentActivityAdapter
            isNestedScrollingEnabled = false // Optimization rule inside NestedScrollViews
        }

        // View All click listener flow mapping
        binding.dashTvViewAll.setOnClickListener {
            Toast.makeText(context, "Navigating to full notifications view...", Toast.LENGTH_SHORT).show()
            // Parent fragment manager dynamic transaction to swap into StaffAlertsFragment here
        }
    }

    private fun observeDashboardUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // Bind metrics to included card subcomponents (Assuming custom title/count IDs inside layout files)
                    // binding.staffDashActiveTableCard.tvCardCount.text = state.activeTablesCount
                    // binding.staffDashPendOrderCard.tvCardCount.text = state.pendingOrdersCount
                    // binding.staffDashReadyOrderCard.tvCardCount.text = state.readyOrdersCount
                    // binding.dashCardTotalAdmin.tvCardCount.text = state.totalServedCount

                    // Pass arrays cleanly down into UI calculations logic engines
                    recentActivityAdapter.submitList(state.logsList)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}