package com.example.masterdashboard.staff_dash.home.order.views

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
import com.example.masterdashboard.databinding.FragmentStaffOrdersBinding
import com.example.masterdashboard.staff_dash.home.StaffHomeActivity
import com.example.masterdashboard.staff_dash.home.order.views.adapter.ActiveOrdersAdapter
import com.example.masterdashboard.staff_dash.home.order.views.repo.ActiveOrdersRepository
import com.example.masterdashboard.staff_dash.home.order.views.viewModel.ActiveOrdersViewModel
import com.example.masterdashboard.staff_dash.home.table.adapter.FloorChipsAdapter
import com.example.masterdashboard.staff_dash.home.table.models.TableFilterData
import kotlinx.coroutines.launch

class StaffOrdersFragment : Fragment() {
    companion object {
        private const val TAG = "StaffOrdersFragment"
    }

    private var _binding: FragmentStaffOrdersBinding? = null
    private val binding get() = _binding!!

    // Declaring explicit screen viewmodel attached to fragment lifecycle boundaries
    private val viewModel: ActiveOrdersViewModel by viewModels {
        ActiveOrdersViewModel.ActiveOrdersViewModelFactory(ActiveOrdersRepository())
    }

    private lateinit var filterAdapter: FloorChipsAdapter
    private lateinit var ordersAdapter: ActiveOrdersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing Orders screen.")

        setupToolbar()
        setUpRecyclerViews()
        observeActiveOrderState()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Ensuring bottom navigation is visible for Orders screen.")
        (activity as? StaffHomeActivity)?.showBottomNavigation()
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
        // horizontal filters row setup
        filterAdapter = FloorChipsAdapter { selectedFilter ->
            viewModel.selectFilterCategory(selectedFilter.id)
        }

        binding.rvStatusFilters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = filterAdapter
        }

        // vertical Orders List Row Setup
        ordersAdapter = ActiveOrdersAdapter { clickedOrder ->
            Log.d(TAG, "Order clicked: ${clickedOrder.orderId}")

            val expansionFragment = OrderDetailExpansionFragment().apply {
                arguments = Bundle().apply {
                    putString("orderId", clickedOrder.orderId)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(this@StaffOrdersFragment.id, expansionFragment)
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
                    // Toggle progress bar loader layout visibility fields
                    binding.pbLoading.visibility = if(state.isLoading) View.VISIBLE else View.GONE

                    // map custom filters payload array onto common TableFilterData configurations
                    val processFilterChips = state.filters.map { model ->
                        TableFilterData(
                            id = model.id,
                            name = model.name,
                            isSelected = model.isSelected
                        )
                    }
                    filterAdapter.submitList(processFilterChips)

                    // dispatch the core active tracking rows dataset to adapter instance
                    ordersAdapter.submitList(state.visibleOrders)

                    // fallback defensive processing against downstream connection failures
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