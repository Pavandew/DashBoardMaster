package com.example.masterdashboard.staff_dash.waiter_screens.table.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.FragmentOrderTakingBinding
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.master_dash.utils.SearchQueryManager
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.*
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.*
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.*
import com.example.masterdashboard.staff_dash.waiter_screens.table.utils.ItemCustomizationOverlayManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Base fragment for the Order Taking UI.
 * Coordinates between Session (shared activity-scoped) and Menu UI (fragment-scoped) ViewModels.
 * Handles the display of categories, diet filters, food items, and the search engine.
 */
abstract class BaseOrderTakingFragment : Fragment() {

    companion object {
        private const val TAG = "BaseOrderTakingFrag"
    }

    protected var _binding: FragmentOrderTakingBinding? = null
    protected val binding get() = _binding!!

    protected val sessionManager by lazy { SessionManager(requireContext()) }
    
    // ACTIVITY-SCOPED: Persistent Session & Cart logic
    protected val sessionViewModel: OrderTakingViewModel by activityViewModels {
        OrderTakingViewModel.OrderViewModelFactory(OrderTakingRepository())
    }

    // FRAGMENT-SCOPED: Volatile Menu UI logic
    private val menuViewModel: OrderMenuViewModel by viewModels {
        OrderTakingViewModel.OrderViewModelFactory(OrderTakingRepository())
    }

    private val customizationViewModel: ItemCustomizationViewModel by viewModels()
    private var overlayManager: ItemCustomizationOverlayManager? = null

    private lateinit var categoryAdapter: FloorChipsAdapter
    private lateinit var dietAdapter: FloorChipsAdapter
    private lateinit var foodAdapter: FoodMenuAdapter
    private var searchManager: SearchQueryManager<FoodItemData>? = null
    private var isProgrammaticScroll = false

    /**
     * Navigation hook for subclasses (Waiter/Cashier) to handle cart opening.
     */
    abstract fun onCartClicked()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrderTakingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Initializing Ordering UI components.")

        setupBackPressHandling()
        setupCommonUI()
        setupRecyclerLayouts()
        setupSearchEngine()
        setupCustomizationOverlay()
        observeViewStates()

        val managerId = sessionManager.getUid()
        
        // 1. Initialize session meta-data in the Activity-scoped ViewModel
        val tableId = arguments?.getString("tableId") ?: ""
        val tableName = arguments?.getString("tableName") ?: "Order"
        val status = arguments?.getString("status") ?: "FREE"
        val existingDocId = arguments?.getString("existingOrderDocId")
        val existingOrderId = arguments?.getString("existingOrderId")
        
        sessionViewModel.startOrderSession(tableId, tableName, status, existingDocId, existingOrderId)

        // 2. Fetch or Resume active order if the table is already occupied
        if (managerId.isNotEmpty()) {
            val floorId = arguments?.getString("floorId") ?: ""
            if (!existingDocId.isNullOrEmpty()) {
                sessionViewModel.resumeOrderSession(managerId, floorId, tableId, existingDocId)
            } else if (status.uppercase() != "FREE") {
                sessionViewModel.findAndResumeOrderSession(managerId, floorId, tableId)
            }
            
            // FIX: Ensure the persistent catalog starts loading/syncing in the Activity-scoped ViewModel
            sessionViewModel.loadCatalog(managerId)
        }
        
        // 3. Load Menu Data (Fragment Local)
        // CRITICAL: We pass a callback to keep the activity-scoped session in sync with the Firestore stream
        Log.d(TAG, "UI: Requesting menu catalog fetch from Firestore.")
        menuViewModel.loadMenuData(
            managerId = managerId, 
            currentCart = sessionViewModel.originalFoodList.value,
            initialCategories = sessionViewModel.categories.value
        ) { fullCatalog ->
            sessionViewModel.syncMenuCatalog(fullCatalog)
            searchManager?.updateDataList(fullCatalog)
        }
    }

    private fun setupCommonUI() {
        binding.btnViewCart.setOnClickListener { onCartClicked() }
        binding.orderTakingToolbar.toolbarImgMenu.setOnClickListener { 
            if (overlayManager?.isVisible() == true) overlayManager?.hide() else parentFragmentManager.popBackStack()
        }
        val tableName = arguments?.getString("tableName") ?: "N/A"
        binding.orderTakingToolbar.tvToolbarTitle.text = "Order - $tableName"
    }

    private fun setupRecyclerLayouts() {
        // Setup Category Horizontal Scroll (Chips)
        categoryAdapter = FloorChipsAdapter { 
            Log.d(TAG, "Filter: Category chip clicked: ${it.name}")
            isProgrammaticScroll = true
            menuViewModel.selectCategory(it.id, sessionViewModel.originalFoodList.value) 
            scrollToCategory(it.id)
        }
        binding.rvMenuCategories.adapter = categoryAdapter
        
        // Setup Diet Filter Scroll (Veg / Non-Veg)
        dietAdapter = FloorChipsAdapter { 
            Log.d(TAG, "Filter: Diet chip clicked: ${it.name}")
            menuViewModel.selectDietFilter(it.id, sessionViewModel.originalFoodList.value) 
        }
        binding.rvDietFilters.adapter = dietAdapter

        // Setup Main Food Menu List
        foodAdapter = FoodMenuAdapter(
            onQuantityIncreased = { 
                sessionViewModel.updateItemQuantity(it.id, true)
                menuViewModel.refreshMenuWithCart(sessionViewModel.originalFoodList.value)
            },
            onQuantityDecreased = { 
                sessionViewModel.updateItemQuantity(it.id, false)
                menuViewModel.refreshMenuWithCart(sessionViewModel.originalFoodList.value)
            },
            onItemClick = { showCustomizationOverlay(it) }
        )
        binding.rvMenuItems.adapter = foodAdapter

        setupScrollSync()
    }

    private fun setupCustomizationOverlay() {
        overlayManager = ItemCustomizationOverlayManager(requireContext(), binding.layoutCustomization, binding.customizationOverlay) { foodId, quantity, variant, addons, variantPrice ->
            sessionViewModel.setItemCustomization(foodId, quantity, variant, addons, variantPrice)
            menuViewModel.refreshMenuWithCart(sessionViewModel.originalFoodList.value)
        }

        // Connect the overlay to the customization ViewModel to handle dynamic addon loading
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                customizationViewModel.addonsState.collect { addonsMap ->
                    val openId = overlayManager?.getCurrentItemId()
                    if (overlayManager?.isVisible() == true && openId != null) {
                        val updatedItem = sessionViewModel.originalFoodList.value.find { it.id == openId }
                        updatedItem?.let {
                            it.availableAddons = addonsMap[openId] ?: emptyList()
                            overlayManager?.refreshList(it)
                        }
                    }
                }
            }
        }
    }

    private fun showCustomizationOverlay(item: FoodItemData) {
        val managerId = sessionManager.getUid()
        if (managerId.isNotEmpty() && item.categoryId.isNotEmpty()) {
            customizationViewModel.loadAddons(managerId, item.categoryId, item.id)
        }
        item.availableAddons = customizationViewModel.getAddonsForItem(item.id)
        overlayManager?.show(item)
    }

    /**
     * Logic to highlight the top category chip based on current list scroll position.
     */
    private fun setupScrollSync() {
        binding.rvMenuItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isProgrammaticScroll) return
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val firstPos = layoutManager.findFirstVisibleItemPosition()
                if (firstPos != RecyclerView.NO_POSITION) {
                    // FIX: Ensure "All" is selected when at the very top
                    val isAtTop = firstPos == 0 && (layoutManager.findViewByPosition(0)?.top ?: 0) >= 0
                    val catId = if (isAtTop) "ALL_ITEMS" else {
                        val item = foodAdapter.currentList.getOrNull(firstPos)
                        when (item) {
                            is MenuItemType.Header -> item.id
                            is MenuItemType.Food -> item.food.categoryId
                            else -> "ALL_ITEMS"
                        }
                    }
                    menuViewModel.syncCategoryHighlight(catId)
                    val chipPos = categoryAdapter.currentList.indexOfFirst { it.id == catId }
                    if (chipPos != -1) binding.rvMenuCategories.smoothScrollToPosition(chipPos)
                }
            }
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) isProgrammaticScroll = false
            }
        })
    }

    private var lastAutoScrolledCatId: String? = null
    private var lastAutoScrolledDietId: String? = null

    private fun observeViewStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                // Observe Menu UI Logic (List rendering and Filtering)
                launch {
                    menuViewModel.uiState.collect { state ->
                        binding.pbLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                        
                        // Submit latest categories and diet filters to their respective chip adapters
                        val catList = state.categories.map { TableFilterData(it.id, it.name, it.isSelected) }
                        categoryAdapter.submitList(catList)
                        
                        val dietList = state.dietFilters.map { TableFilterData(it.id, it.name, it.isSelected) }
                        dietAdapter.submitList(dietList)
                        
                        // AUTO-SCROLL logic with guards to prevent redundant jumps
                        val currentCatId = catList.find { it.isSelected }?.id
                        if (currentCatId != null && currentCatId != lastAutoScrolledCatId) {
                            val selectedCatPos = catList.indexOfFirst { it.id == currentCatId }
                            if (selectedCatPos != -1) {
                                binding.rvMenuCategories.post {
                                    binding.rvMenuCategories.smoothScrollToPosition(selectedCatPos)
                                }
                            }
                            lastAutoScrolledCatId = currentCatId
                        }
                        
                        val currentDietId = dietList.find { it.isSelected }?.id
                        if (currentDietId != null && currentDietId != lastAutoScrolledDietId) {
                            val selectedDietPos = dietList.indexOfFirst { it.id == currentDietId }
                            if (selectedDietPos != -1) {
                                binding.rvDietFilters.post {
                                    binding.rvDietFilters.smoothScrollToPosition(selectedDietPos)
                                }
                            }
                            lastAutoScrolledDietId = currentDietId
                        }

                        // Update the RecyclerView: Only submit grouped list if search is not active
                        if (binding.searchBar.etSearchOrder.text.isNullOrEmpty()) {
                            foodAdapter.submitList(state.displayItems)
                        } else {
                            searchManager?.refreshSearch()
                        }
                    }
                }
                
                // Observe Session/Cart Logic (Price totals and Stepper numbers)
                launch {
                    sessionViewModel.cartSummary.collect { summary ->
                        binding.tvCartCountLabel.text = "View Cart (${summary.totalItems})"
                        binding.tvCartTotalPrice.text = "₹ ${summary.totalPrice}"
                        binding.btnViewCart.visibility = if (summary.totalItems > 0) View.VISIBLE else View.GONE
                        
                        // Push cart changes back into the menu UI to keep stepper counts accurate
                        menuViewModel.refreshMenuWithCart(sessionViewModel.originalFoodList.value)
                    }
                }

                // FIX: Observe persistent categories from Session VM and sync them to the Menu UI
                launch {
                    sessionViewModel.categories.collect { categories ->
                        if (categories.isNotEmpty()) {
                            Log.d(TAG, "UI: Syncing ${categories.size} categories from Session to Menu.")
                            menuViewModel.syncCategories(categories)
                        }
                    }
                }
            }
        }
    }

    private fun scrollToCategory(categoryId: String) {
        val position = if (categoryId == "ALL_ITEMS") 0 else foodAdapter.currentList.indexOfFirst { (it is MenuItemType.Header && it.id == categoryId) }
        if (position != -1) (binding.rvMenuItems.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, 0)
    }

    private fun setupSearchEngine() {
        searchManager = SearchQueryManager(
            binding.searchBar.etSearchOrder, 
            menuViewModel.getRawFoodList(),
            { filtered ->
                if (binding.searchBar.etSearchOrder.text.isNullOrEmpty()) {
                    foodAdapter.submitList(menuViewModel.uiState.value.displayItems)
                } else {
                    foodAdapter.submitList(filtered.map { MenuItemType.Food(it) })
                }
            }, 
            { item, query -> item.name.contains(query, ignoreCase = true) }
        )
    }

    private fun setupBackPressHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (overlayManager?.isVisible() == true) overlayManager?.hide() else { isEnabled = false; requireActivity().onBackPressedDispatcher.onBackPressed() }
            }
        })
    }

    override fun onDestroyView() { 
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Cleaning up Fragment View Binding.")
        _binding = null 
    }
}
