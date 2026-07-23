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
import com.example.masterdashboard.databinding.FragmentWaiterActiveOrdersBinding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.order.adapter.ActiveOrdersAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.order.repo.ActiveOrdersRepository
import com.example.masterdashboard.staff_dash.waiter_screens.order.viewModel.ActiveOrdersViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
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

        setupToolbar()
        setUpRecyclerViews()
        observeActiveOrderState()

        val managerId = sessionManager.getUid()
        Log.d(TAG, "onViewCreated: Triggering streamActiveOrders for Manager ID: $managerId")
        viewModel.streamActiveOrders(managerId)
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
        toolbar.tvToolbarTitle.text = getString(R.string.orders)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgMenu.setImageResource(R.drawable.ic_arrow_back_24dp)
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
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
            Log.d(TAG, "Order clicked: ID='${clickedOrder.orderId}', Table='${clickedOrder.tableName}', Status='${clickedOrder.status.name}', Time='${clickedOrder.orderTime}'")

            val expansionFragment = OrderDetailExpansionFragment().apply {
                arguments = Bundle().apply {
                    putString("orderId", clickedOrder.orderId)
                    putString("tableName", clickedOrder.tableName)
                    putString("orderStatus", clickedOrder.status.name)
                    putString("orderTime", clickedOrder.orderTime)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(this@WaiterActiveOrdersFragment.id, expansionFragment)
                .addToBackStack(null)
                .commit()
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
                    binding.pbLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    val processFilterChips = state.filters.map { model ->
                        TableFilterData(
                            id = model.id,
                            name = model.name,
                            isSelected = model.isSelected
                        )
                    }
                    filterAdapter.submitList(processFilterChips)
                    ordersAdapter.submitList(state.visibleOrders)

                    state.errorMessage?.let { error ->
                        Log.e(TAG, "Error loading orders: $error")
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