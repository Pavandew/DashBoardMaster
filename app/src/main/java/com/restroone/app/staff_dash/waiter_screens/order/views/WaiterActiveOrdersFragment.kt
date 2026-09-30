package com.restroone.app.staff_dash.waiter_screens.order.views

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
import com.restroone.app.R
import com.restroone.app.databinding.FragmentWaiterActiveOrdersBinding
import com.restroone.app.utils.NavigationUtils
import com.restroone.app.utils.SessionManager
import com.restroone.app.staff_dash.waiter_screens.WaiterHomeActivity
import com.restroone.app.staff_dash.waiter_screens.order.adapter.ActiveOrdersAdapter
import com.restroone.app.staff_dash.waiter_screens.order.repo.ActiveOrdersRepository
import com.restroone.app.staff_dash.waiter_screens.order.viewModel.ActiveOrdersViewModel
import com.restroone.app.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
import com.restroone.app.staff_dash.waiter_screens.table.models.TableFilterData
import kotlinx.coroutines.launch

class WaiterActiveOrdersFragment : Fragment() {
    companion object {
        private const val TAG = "WaiterActiveOrdersFragment"
    }

    private var _binding: FragmentWaiterActiveOrdersBinding? = null
    private val binding get() = _binding!!

    private val sessionManager by lazy { SessionManager(requireContext()) }

    private val viewModel: ActiveOrdersViewModel by viewModels {
        ActiveOrdersViewModel.ActiveOrdersViewModelFactory(ActiveOrdersRepository())
    }

    private lateinit var filterAdapter: FloorChipsAdapter
    private lateinit var ordersAdapter: ActiveOrdersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaiterActiveOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: WaiterActiveOrdersFragment Opened")

        val managerId = sessionManager.getUid()

        setupToolbar()
        setUpRecyclerViews()
        setupSwipeRefreshAndRetry(managerId)
        observeActiveOrderState()

        Log.d(TAG, "onViewCreated: Triggering streamActiveOrders for Manager ID: $managerId")
        viewModel.streamActiveOrders(managerId)
    }

    private fun setupSwipeRefreshAndRetry(managerId: String) {
        binding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "Swipe-to-refresh triggered on Active Orders")
            viewModel.forceRefresh(managerId)
        }

        binding.includeErrorState.btnErrorRetry.setOnClickListener {
            Log.d(TAG, "Retry button clicked on Active Orders")
            binding.includeErrorState.layoutErrorContainer.visibility = View.GONE
            viewModel.forceRefresh(managerId)
        }
    }

    override fun onResume() {
        super.onResume()
        // FIX: Executing resetFilterToAll in onResume guarantees the StateFlow collector
        // is active and will immediately force the UI chip selection back to "All".
        viewModel.resetFilterToAll()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Ensuring bottom navigation is visible for Orders screen.")
        (activity as? WaiterHomeActivity)?.showBottomNavigation()
    }

    private fun setupToolbar() {
        val toolbar = binding.staffOrdersToolbar
        toolbar.tvToolbarTitle.text = getString(R.string.title_track_orders)
        toolbar.llSubtitleContainer.visibility = View.GONE

        if (parentFragmentManager.backStackEntryCount > 0) {
            toolbar.toolbarImgMenu.visibility = View.VISIBLE
            toolbar.toolbarImgMenu.setImageResource(R.drawable.ic_arrow_back_24dp)
            toolbar.toolbarImgMenu.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
        } else {
            toolbar.toolbarImgMenu.visibility = View.GONE
        }
    }

    private fun setUpRecyclerViews() {
        filterAdapter = FloorChipsAdapter { selectedFilter ->
            viewModel.selectFilterCategory(selectedFilter.id)
        }

        binding.rvStatusFilters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = filterAdapter
        }

        ordersAdapter = ActiveOrdersAdapter { clickedOrder ->
            Log.d(TAG, "Order clicked: ID='${clickedOrder.orderId}', Table='${clickedOrder.tableName}', Status='${clickedOrder.status.name}', Time='${clickedOrder.orderTime}', DocPath='${clickedOrder.docPath}'")

            val expansionFragment = OrderDetailExpansionFragment().apply {
                arguments = Bundle().apply {
                    putString("orderId", clickedOrder.orderId)
                    putString("docPath", clickedOrder.docPath)
                    putString("tableName", clickedOrder.tableName)
                    putString("orderStatus", clickedOrder.status.name)
                    putString("orderTime", clickedOrder.orderTime)
                }
            }

            val containerId = NavigationUtils.getHostContainerId(activity)
            if (containerId != 0) {
                parentFragmentManager.beginTransaction()
                    .replace(containerId, expansionFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.rvActiveOrders.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ordersAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeActiveOrderState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.pbLoading.visibility = if (state.isLoading && !state.isRefreshing) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = state.isRefreshing

                    if (state.errorMessage != null) {
                        binding.rvActiveOrders.visibility = View.GONE
                        binding.includeEmptyState.layoutEmptyContainer.visibility = View.GONE
                        binding.includeErrorState.layoutErrorContainer.visibility = View.VISIBLE
                        binding.includeErrorState.tvErrorStateMessage.text = state.errorMessage
                    } else {
                        binding.includeErrorState.layoutErrorContainer.visibility = View.GONE

                        if (state.visibleOrders.isEmpty()) {
                            binding.rvActiveOrders.visibility = View.GONE
                            binding.includeEmptyState.layoutEmptyContainer.visibility = View.VISIBLE
                            binding.includeEmptyState.tvEmptyStateTitle.text = "No Active Orders"
                            binding.includeEmptyState.tvEmptyStateMessage.text = "There are no active orders in this section currently."
                        } else {
                            binding.includeEmptyState.layoutEmptyContainer.visibility = View.GONE
                            binding.rvActiveOrders.visibility = View.VISIBLE
                        }
                    }

                    val processFilterChips = state.filters.map { model ->
                        TableFilterData(
                            id = model.id,
                            name = model.name,
                            isSelected = model.isSelected
                        )
                    }
                    filterAdapter.submitList(processFilterChips)
                    ordersAdapter.submitList(state.visibleOrders)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}