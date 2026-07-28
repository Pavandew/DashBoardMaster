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
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentOrderTakingBinding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.master_dash.utils.SearchQueryManager
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.views.ItemCustomizationDetailFragment
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuItemDetailData
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.FoodMenuAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuItemType
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableStatus
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import kotlinx.coroutines.launch

class OrderTakingFragment : Fragment() {

    companion object {
        private const val TAG = "Order_Flow_Debug"
    }

    private var _binding: FragmentOrderTakingBinding? = null
    private val binding get() = _binding!!

    private val sessionManager by lazy { SessionManager(requireContext()) }

    private val viewModel: OrderTakingViewModel by activityViewModels {
        OrderTakingViewModel.OrderViewModelFactory(OrderTakingRepository())
    }

    private lateinit var categoryAdapter: FloorChipsAdapter
    private lateinit var dietAdapter: FloorChipsAdapter
    private lateinit var foodAdapter: FoodMenuAdapter

    private var searchManager: SearchQueryManager<FoodItemData>? = null
    private val mutableFoodSearchList = mutableListOf<FoodItemData>()

    // Flag to prevent recursive scroll calls during programmatic jumps
    private var isProgrammaticScroll = false

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
        val tableName = arguments?.getString("tableName") ?: "Takeaway"
        val status = arguments?.getString("status") ?: "FREE"
        val existingDocId = arguments?.getString("existingOrderDocId")
        val existingOrderId = arguments?.getString("existingOrderId")

        // Initialize or resume the order session with the Table Name included
        viewModel.startOrderSession(tableId, tableName, status, existingDocId, existingOrderId)

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
        (activity as? CashierHomeActivity)?.hideBottomNavigation()
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

            val isCashier = arguments?.getBoolean("isCashier") ?: false

            val bundle = Bundle().apply {
                putString("tableId", tableId)
                putString("tableName", tableName)
                putString("floorId", floorId)
                putString("status", status)
                putBoolean("isCashier", isCashier) // Forward the flag
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
            
            // Programmatically scroll the menu when a chip is clicked in "All Items" mode
            if (viewModel.uiState.value.activeFilterId == "ALL_ITEMS") {
                scrollToCategorySection(selectedCategory.id)
            }
        }
        binding.rvMenuCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
            visibility = View.VISIBLE
        }

        dietAdapter = FloorChipsAdapter { selectedDiet ->
            viewModel.selectDietFilter(selectedDiet.id)
        }
        binding.rvDietFilters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dietAdapter
            visibility = View.VISIBLE
        }

        foodAdapter = FoodMenuAdapter(
            onQuantityIncreased = { viewModel.updateItemQuantity(it.id, true) },
            onQuantityDecreased = { viewModel.updateItemQuantity(it.id, false) },
            onItemClick = { foodItem ->
                navigateToItemCustomization(foodItem)
            }
        )
        binding.rvMenuItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = foodAdapter
            setHasFixedSize(true)
            
            // Add scroll listener for auto-selecting chips based on visible section
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    // Only sync chips if the user is scrolling manually and we are in "All Items" mode
                    // Also skip if searching since chips are hidden
                    if (isProgrammaticScroll || dy == 0 || binding.etSearchItem.text.isNotEmpty()) return
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
                    
                    if (firstVisiblePos == 0) {
                        // At the very top, select "All" chip
                        viewModel.syncCategorySelectionFromScroll("ALL_ITEMS")
                        return
                    }

                    if (firstVisiblePos != RecyclerView.NO_POSITION && foodAdapter.currentList.isNotEmpty()) {
                        val item = foodAdapter.currentList.getOrNull(firstVisiblePos)
                        val categoryId = when (item) {
                            is MenuItemType.Header -> item.id
                            is MenuItemType.Food -> item.food.categoryId
                            null -> null
                        }
                        
                        categoryId?.let { id ->
                            // Only update visual chip highlights, don't change the filter!
                            viewModel.syncCategorySelectionFromScroll(id)
                        }
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        isProgrammaticScroll = false
                    }
                }
            })
        }
    }

    private fun scrollToCategorySection(categoryId: String) {
        if (categoryId == "ALL_ITEMS") {
            binding.rvMenuItems.smoothScrollToPosition(0)
            return
        }
        
        val position = foodAdapter.currentList.indexOfFirst { 
            it is MenuItemType.Header && it.id == categoryId
        }
        if (position != -1) {
            isProgrammaticScroll = true
            (binding.rvMenuItems.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
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
                    
                    Log.d(TAG, "📱 [FRAGMENT] Submitting ${categoryChips.size} chips to adapter")
                    categoryAdapter.submitList(categoryChips)

                    val dietChips = state.dietFilters.map {
                        TableFilterData(id = it.id, name = it.name, isSelected = it.isSelected)
                    }
                    dietAdapter.submitList(dietChips)

                    // Update the master list used for searching
                    mutableFoodSearchList.clear()
                    mutableFoodSearchList.addAll(viewModel.originalFoodList)

                    // Update the adapter list
                    val query = binding.etSearchItem.text.toString().trim()
                    if (query.isEmpty()) {
                        Log.i(TAG, "📱 [FRAGMENT] Displaying ${state.displayItems.size} items with headers (Filter: ${state.activeFilterId})")
                        foodAdapter.submitList(state.displayItems)
                    } else {
                        // REFRESH SEARCH RESULTS: Ensure quantity updates (stepper) show up while searching
                        val filteredList = viewModel.originalFoodList.filter { 
                            it.name.contains(query, ignoreCase = true) 
                        }.map { MenuItemType.Food(it) }
                        Log.d(TAG, "📱 [FRAGMENT] Refreshing search results for '$query' (${filteredList.size} items)")
                        foodAdapter.submitList(filteredList)
                    }

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
                val query = binding.etSearchItem.text.toString().trim()
                val isSearching = query.isNotEmpty()

                // 1. Hide/Show Filter Chips during search
                binding.rvDietFilters.visibility = if (isSearching) View.GONE else View.VISIBLE
                binding.rvMenuCategories.visibility = if (isSearching) View.GONE else View.VISIBLE

                // 2. Submit items to adapter
                if (isSearching) {
                    // Show a flat list of results without headers during search
                    val displayList = filteredList.map { MenuItemType.Food(it) }
                    foodAdapter.submitList(displayList)
                } else {
                    // Restore the sectioned list from ViewModel state when search is cleared
                    foodAdapter.submitList(viewModel.uiState.value.displayItems)
                }
            },
            filterRule = { foodItem, query ->
                foodItem.name.contains(query, ignoreCase = true)
            }
        )
    }

    private fun navigateToItemCustomization(foodItem: FoodItemData) {
        // PREVENT SESSION RESET: Tell ViewModel we are just navigating, so it doesn't clear the cart
        viewModel.isViewingCart = true

        val menuItemDetail = MenuItemDetailData(
            itemId = foodItem.id,
            itemName = foodItem.name,
            basePrice = foodItem.price.toDouble(),
            imageUrl = foodItem.imageUrl
        )

        val customizationFragment = ItemCustomizationDetailFragment.newInstance(menuItemDetail)

        parentFragmentManager.beginTransaction()
            .replace(this@OrderTakingFragment.id, customizationFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        searchManager?.removeListener()
        super.onDestroyView()
        _binding = null
    }
}
