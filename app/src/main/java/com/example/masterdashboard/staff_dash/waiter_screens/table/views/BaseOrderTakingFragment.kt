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

/**
 * Base class containing shared logic for menu listing, searching, and filtering.
 * Role-specific fragments (Waiter/Cashier) inherit from this to handle their own navigation.
 */
abstract class BaseOrderTakingFragment : Fragment() {

    protected var _binding: FragmentOrderTakingBinding? = null
    protected val binding get() = _binding!!

    protected val sessionManager by lazy { SessionManager(requireContext()) }
    protected val viewModel: OrderTakingViewModel by activityViewModels {
        OrderTakingViewModel.OrderViewModelFactory(OrderTakingRepository())
    }

    private lateinit var categoryAdapter: FloorChipsAdapter
    private lateinit var dietAdapter: FloorChipsAdapter
    private lateinit var foodAdapter: FoodMenuAdapter
    private var searchManager: SearchQueryManager<FoodItemData>? = null
    private val mutableFoodSearchList = mutableListOf<FoodItemData>()
    private var isProgrammaticScroll = false

    // Abstract method to be implemented by Waiter/Cashier fragments
    abstract fun onCartClicked()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrderTakingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupCommonUI()
        setupRecyclerLayouts()
        setupSearchEngine()
        observeStateFlows()

        val managerId = sessionManager.getUid()
        viewModel.loadMenuData(managerId)
        
        // Initialize or resume the order session
        val tableId = arguments?.getString("tableId") ?: ""
        val tableName = arguments?.getString("tableName") ?: "Order"
        val status = arguments?.getString("status") ?: "FREE"
        viewModel.startOrderSession(tableId, tableName, status)

        if (!managerId.isNullOrEmpty()) {
            val existingDocId = arguments?.getString("existingOrderDocId")
            if (!existingDocId.isNullOrEmpty()) {
                val floorId = arguments?.getString("floorId") ?: ""
                viewModel.resumeOrderSession(managerId, floorId, tableId, existingDocId)
            }
        }
    }

    private fun setupCommonUI() {
        binding.btnViewCart.setOnClickListener { onCartClicked() }
        binding.orderTakingToolbar.toolbarImgMenu.setOnClickListener { parentFragmentManager.popBackStack() }
        
        val tableName = arguments?.getString("tableName") ?: "N/A"
        binding.orderTakingToolbar.tvToolbarTitle.text = "Order - $tableName"
    }

    private fun setupRecyclerLayouts() {
        categoryAdapter = FloorChipsAdapter { viewModel.selectCategory(it.id) }
        binding.rvMenuCategories.adapter = categoryAdapter
        
        dietAdapter = FloorChipsAdapter { viewModel.selectDietFilter(it.id) }
        binding.rvDietFilters.adapter = dietAdapter

        foodAdapter = FoodMenuAdapter(
            onQuantityIncreased = { viewModel.updateItemQuantity(it.id, true) },
            onQuantityDecreased = { viewModel.updateItemQuantity(it.id, false) },
            onItemClick = { navigateToItemCustomization(it) }
        )
        binding.rvMenuItems.adapter = foodAdapter
    }

    private fun observeStateFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.pbLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    categoryAdapter.submitList(state.categories.map { TableFilterData(it.id, it.name, it.isSelected) })
                    dietAdapter.submitList(state.dietFilters.map { TableFilterData(it.id, it.name, it.isSelected) })
                    foodAdapter.submitList(state.displayItems)
                    
                    binding.tvCartCountLabel.text = "View Cart (${state.cartSummary.totalItems})"
                    binding.tvCartTotalPrice.text = "₹ ${state.cartSummary.totalPrice}"
                    binding.btnViewCart.visibility = if (state.cartSummary.totalItems > 0) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupSearchEngine() {
        searchManager = SearchQueryManager(binding.etSearchItem, mutableFoodSearchList, { filtered ->
            foodAdapter.submitList(filtered.map { MenuItemType.Food(it) })
        }, { item, query -> item.name.contains(query, ignoreCase = true) })
    }

    private fun navigateToItemCustomization(foodItem: FoodItemData) {
        viewModel.isViewingCart = true
        val menuItemDetail = MenuItemDetailData(
            itemId = foodItem.id,
            itemName = foodItem.name,
            basePrice = foodItem.price.toDouble(),
            imageUrl = foodItem.imageUrl
        )
        parentFragmentManager.beginTransaction()
            .replace(this.id, ItemCustomizationDetailFragment.newInstance(menuItemDetail))
            .addToBackStack(null).commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}