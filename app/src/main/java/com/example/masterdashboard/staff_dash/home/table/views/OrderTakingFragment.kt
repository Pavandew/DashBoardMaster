package com.example.masterdashboard.staff_dash.home.table.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // FIXED: Imported activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentOrderTakingBinding
import com.example.masterdashboard.master_dash.home.SearchQueryManager
import com.example.masterdashboard.staff_dash.home.StaffHomeActivity
import com.example.masterdashboard.staff_dash.home.table.adapter.FloorChipsAdapter
import com.example.masterdashboard.staff_dash.home.table.adapter.FoodMenuAdapter
import com.example.masterdashboard.staff_dash.home.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.home.table.models.TableFilterData
import com.example.masterdashboard.staff_dash.home.table.models.TableStatus
import com.example.masterdashboard.staff_dash.home.table.repo.OrderRepository
import com.example.masterdashboard.staff_dash.home.table.viewModels.OrderViewModel
import kotlinx.coroutines.launch

class OrderTakingFragment : Fragment() {

    companion object {
        private const val TAG = "OrderTakingFragment"
    }

    private var _binding: FragmentOrderTakingBinding? = null
    private val binding get() = _binding!!

    // FIXED: Swapped 'by viewModels' for 'by activityViewModels' to share state layers seamlessly
    private val viewModel: OrderViewModel by activityViewModels {
        OrderViewModel.OrderViewModelFactory(OrderRepository())
    }

    private lateinit var categoryAdapter: FloorChipsAdapter
    private lateinit var foodAdapter: FoodMenuAdapter

    private var searchManager: SearchQueryManager<FoodItemData>? = null
    private val mutableFoodSearchList = mutableListOf<FoodItemData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderTakingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing Fragment")

        setupToolbarNavigation()
        setupClickListeners()
        setupTableDetailsFromArgs()
        setupRecyclerLayouts()
        setupSearchEngine()
        observeStateFlows()
    }

    override fun onStart() {
        super.onStart()
        (activity as? StaffHomeActivity)?.hideBottomNavigation()
    }

    private fun setupToolbarNavigation() {
        binding.orderTakingToolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupClickListeners() {
        binding.btnViewCart.setOnClickListener {
            Log.d(TAG, "btnViewCart Clicked: Passing operational context to Cart detail screen")
            val tableId = arguments?.getString("tableId") ?: "N/A"

            val bundle = Bundle().apply {
                putString("tableId", tableId)
            }

            val cartDetailFragment = ViewCartDetailsFragment().apply {
                arguments = bundle
            }

            // Swap fragment container dynamically using a standard clean backstack replacement operation
            parentFragmentManager.beginTransaction()
                .replace(this@OrderTakingFragment.id, cartDetailFragment) // FIXED: Uses dynamic container ID to support both Staff and Manager activities
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupTableDetailsFromArgs() {
        val tableId = arguments?.getString("tableId") ?: "N/A"
        val seats = arguments?.getInt("totalSeats") ?: 0
        val statusName = arguments?.getString("status") ?: "FREE"

        Log.d(TAG, "setupTableDetailsFromArgs: tableId=$tableId, seats=$seats, status=$statusName")

        val toolbar = binding.orderTakingToolbar
        toolbar.llSubtitleContainer.visibility = View.VISIBLE
        toolbar.tvToolbarTitle.text = "Table $tableId"
        toolbar.tvToolbarSubtitle.text = "$seats Seats"

        val status = try { TableStatus.valueOf(statusName) } catch (e: Exception) { TableStatus.FREE }

        toolbar.tvToolbarStatusTag.text = status.name.lowercase().replaceFirstChar { it.uppercase() }

        val context = requireContext()
        when (status) {
            TableStatus.FREE -> {
                toolbar.tvToolbarStatusTag.setBackgroundResource(R.drawable.bg_status_free)
                toolbar.tvToolbarStatusTag.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.status_free))
            }
            TableStatus.OCCUPIED -> {
                toolbar.tvToolbarStatusTag.setBackgroundResource(R.drawable.bg_status_occupied)
                toolbar.tvToolbarStatusTag.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.status_occupied))
            }
            TableStatus.RESERVED -> {
                toolbar.tvToolbarStatusTag.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.status_reserved_bg))
                toolbar.tvToolbarStatusTag.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.status_reserved))
            }
            TableStatus.BILLING -> {
                toolbar.tvToolbarStatusTag.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.status_billing_bg))
                toolbar.tvToolbarStatusTag.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.status_billing))
            }
        }
    }

    private fun setupRecyclerLayouts() {
        categoryAdapter = FloorChipsAdapter { selectedCategory ->
            viewModel.selectCategory(selectedCategory.id)
        }
        binding.rvMenuCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        foodAdapter = FoodMenuAdapter(
            onQuantityIncreased = { viewModel.updateItemQuantity(it.id, true) },
            onQuantityDecreased = { viewModel.updateItemQuantity(it.id, false) }
        )
        binding.rvMenuItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = foodAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeStateFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d(TAG, "observeStateFlows: New state received - isLoading=${state.isLoading}")

                    // Handle Loading state
                    // binding.pbLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Handle Error state
                    state.errorMessage?.let {
                        Log.e(TAG, "observeStateFlows: Error observed: $it")
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }

                    val categoryChips = state.categories.map {
                        TableFilterData(id = it.id, name = it.name, isSelected = it.isSelected)
                    }
                    categoryAdapter.submitList(categoryChips)

                    // Update Grid items List representation safely
                    foodAdapter.submitList(state.menuItems)

                    // Sync the internal search query snapshot collection point directly
                    mutableFoodSearchList.clear()
                    mutableFoodSearchList.addAll(viewModel.originalFoodList)

                    // Update Cart Summary
                    binding.tvCartCountLabel.text = getString(R.string.view_cart_format, state.cartSummary.totalItems)
                    binding.tvCartTotalPrice.text = getString(R.string.currency_symbol) + " ${state.cartSummary.totalPrice}"
                    binding.btnViewCart.visibility = if (state.cartSummary.totalItems > 0) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupSearchEngine() {
        searchManager = SearchQueryManager(
            searchEditText = binding.etSearchItem,
            originalList = mutableFoodSearchList,
            onResultFiltered = { filteredList ->
                foodAdapter.submitList(filteredList)
            },
            filterRule = { foodItem, query ->
                foodItem.name.contains(query, ignoreCase = true)
            }
        )
    }

    override fun onDestroyView() {
        searchManager?.removeListener()
        super.onDestroyView()
        _binding = null
    }
}
