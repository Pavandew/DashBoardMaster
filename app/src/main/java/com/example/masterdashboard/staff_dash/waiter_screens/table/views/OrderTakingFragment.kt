package com.example.masterdashboard.staff_dash.waiter_screens.table.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentOrderTakingBinding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.master_dash.utils.SearchQueryManager
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.FoodMenuAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableStatus
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import kotlinx.coroutines.launch

class OrderTakingFragment : Fragment() {

    companion object {
        // Tag unified across order flow components for clear Logcat tracking
        private const val TAG = "Order_Flow_Debug"
    }

    private var _binding: FragmentOrderTakingBinding? = null
    private val binding get() = _binding!!

    // Lazy initialization of the session cache preference utility
    private val sessionManager by lazy { SessionManager(requireContext()) }

    // Scoped to activityViewModels to seamlessly share cart modifications across workflow screens
    private val viewModel: OrderTakingViewModel by activityViewModels {
        OrderTakingViewModel.OrderViewModelFactory(OrderTakingRepository())
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
        Log.i(TAG, "📱 [FRAGMENT] OrderTakingFragment Launched Successfully")

        // Retrieve table context from arguments
        val tableId = arguments?.getString("tableId") ?: ""
        val status = arguments?.getString("status") ?: "FREE"
        val existingDocId = arguments?.getString("existingOrderDocId")
        val existingOrderId = arguments?.getString("existingOrderId")

        // Initialize or resume the order session
        viewModel.startOrderSession(tableId, status, existingDocId, existingOrderId)

        // Retrieve the restaurant context key token from the local session cache
        val managerId = sessionManager.getUid()
        val managerName = sessionManager.getUserName() ?: "Unknown Manager"
        Log.d(TAG, "📱 [FRAGMENT] Session Context: [ManagerName: $managerName | ID: $managerId]")

        setupToolbarNavigation()
        setupClickListeners()
        setupTableDetailsFromArgs()
        setupRecyclerLayouts()
        setupSearchEngine()
        observeStateFlows()

        // Fetch the fresh real-time menu and category data pipes
        viewModel.loadMenuData(managerId)

        // If resuming an order, trigger the data merge
        if (!existingDocId.isNullOrEmpty() && !managerId.isNullOrEmpty()) {
            val floorId = arguments?.getString("floorId") ?: ""
            viewModel.resumeOrderSession(managerId, floorId, tableId, existingDocId)
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as? WaiterHomeActivity)?.hideBottomNavigation()
    }

    private fun setupToolbarNavigation() {
        binding.orderTakingToolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupClickListeners() {
        binding.btnViewCart.setOnClickListener {
            // Set flag in ViewModel to indicate we are moving to Cart
            viewModel.isViewingCart = true

            val tableName = arguments?.getString("tableName") ?: "N/A"
            val tableId = arguments?.getString("tableId") ?: ""
            val floorId = arguments?.getString("floorId") ?: ""
            val status = arguments?.getString("status") ?: "FREE"

            val bundle = Bundle().apply {
                putString("tableId", tableId)
                putString("tableName", tableName)
                putString("floorId", floorId)
                putString("status", status)
            }

            val cartDetailFragment = ViewCartDetailsFragment().apply {
                arguments = bundle
            }

            parentFragmentManager.beginTransaction()
                .replace(this@OrderTakingFragment.id, cartDetailFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupTableDetailsFromArgs() {
        val tableName = arguments?.getString("tableName") ?: "N/A"
        val seats = arguments?.getInt("totalSeats") ?: 0
        val statusName = arguments?.getString("status") ?: "FREE"

        val toolbar = binding.orderTakingToolbar
        toolbar.llSubtitleContainer.visibility = View.VISIBLE
        toolbar.tvToolbarTitle.text = "Table - $tableName"
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
            // Informs the ViewModel to apply the selected category filter to the visible list
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

    /**
     * Observes centralized live UI states streaming down from the Shared ViewModel state flow pipeline layer.
     */
    private fun observeStateFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // Manage visibility of progress indicators dynamically
                    binding.pbLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    state.errorMessage?.let {
                        Log.e(TAG, "📱 [FRAGMENT] Error stream response caught: $it")
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }

                    // Maps core dynamic menu category classes over to horizontal chip views cleanly
                    val categoryChips = state.categories.map {
                        TableFilterData(id = it.id, name = it.name, isSelected = it.isSelected)
                    }
                    categoryAdapter.submitList(categoryChips)

                    // Refresh food cards inside the visible layout menu list
                    Log.i(TAG, "📱 [FRAGMENT] Displaying ${state.menuItems.size} food items matching current filter layout.")
                    foodAdapter.submitList(state.menuItems)

                    // FIX: Sync search engine snapshots directly from the master originalFoodList container
                    // This guarantees items selected from hidden categories aren't wiped out during search text parsing
                    mutableFoodSearchList.clear()
                    mutableFoodSearchList.addAll(viewModel.originalFoodList)

                    // Update layout cart footer details visibility conditions
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