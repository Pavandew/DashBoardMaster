package com.example.masterdashboard.staff_dash.waiter_screens.alert

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
import com.example.masterdashboard.databinding.FragmentStaffAlertBinding
import com.example.masterdashboard.staff_dash.waiter_screens.StaffHomeActivity
import kotlinx.coroutines.launch

class StaffAlertFragment : Fragment() {

    companion object {
        private const val TAG = "StaffAlertsFragment"
    }

    private var _binding: FragmentStaffAlertBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlertsViewModel by viewModels {
        AlertsViewModel.AlertsViewModelFactory()
    }

    private lateinit var alertsAdapter: AlertsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffAlertBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: StaffAlertFragment Opened")
        Log.d(TAG, "onViewCreated: Binding data components for Alerts Feed.")

        setupToolbarLayout()
        setupAlertsRecyclerView()
        observeAlertsStateFlow()

        viewModel.fetchLiveAlertsFeed()
    }

    override fun onStart() {
        super.onStart()
        // Unlocks navigation view bar tracking back inside primary structural hubs
        (activity as? StaffHomeActivity)?.showBottomNavigation()
    }

    private fun setupToolbarLayout() {
        val toolbar = binding.staffAlertToolbar
        toolbar.tvToolbarTitle.text = getString(R.string.alerts)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE

        // Sets up optional clean back exit point
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupAlertsRecyclerView() {
        alertsAdapter = AlertsAdapter(
            onCardClicked = { clickedItem ->
                viewModel.handleCardExpansionToggle(clickedItem)
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

                        // Submit update to DiffUtil adapter calculation engine
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