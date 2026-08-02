package com.example.masterdashboard.staff_dash.waiter_screens.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentItemCustomizationDetailBinding
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.AddonsAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuItemDetailData
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel

class ItemCustomizationDetailFragment : Fragment() {

    companion object {
        private const val TAG = "ItemCustomizationDetail"
        private const val ARG_MENU_ITEM = "ARG_MENU_ITEM"

        fun newInstance(item: MenuItemDetailData): ItemCustomizationDetailFragment {
            return ItemCustomizationDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_MENU_ITEM, item)
                }
            }
        }
    }

    private var _binding: FragmentItemCustomizationDetailBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel to update the cart in the parent OrderTakingFragment
    private val orderTakingViewModel: OrderTakingViewModel by activityViewModels {
        OrderTakingViewModel.OrderViewModelFactory(OrderTakingRepository())
    }

    private var menuItem: MenuItemDetailData? = null
    private var quantityCount: Int = 1
    private lateinit var addonsAdapter: AddonsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuItem = arguments?.getSerializable(ARG_MENU_ITEM) as? MenuItemDetailData
            ?: MenuItemDetailData() // Fallback dummy instance for testing
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemCustomizationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        (activity as? WaiterHomeActivity)?.hideBottomNavigation()
        (activity as? CashierHomeActivity)?.hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Customization sheet opened for item: ${menuItem?.itemName}")

        // Load current quantity from ViewModel if it exists
        menuItem?.itemId?.let { id ->
            val currentQty = orderTakingViewModel.originalFoodList.find { it.id == id }?.currentQuantity ?: 1
            if (currentQty > 0) {
                quantityCount = currentQty
            }
        }

        setupStaticData()
        setupAddonsRecyclerView()
        setupQuantityControls()
        setupListeners()
        recalculateTotalPrice()
    }

    private fun setupStaticData() {
        menuItem?.let { item ->
            binding.tvCustomizationToolbarTitle.text = item.itemName
            binding.tvFoodItemName.text = item.itemName
            binding.tvFoodBasePrice.text = "₹${item.basePrice.toInt()}"
            binding.tvFoodDescription.text = item.description
        }
    }

    private fun setupAddonsRecyclerView() {
        val addons = menuItem?.availableAddons ?: emptyList()
        addonsAdapter = AddonsAdapter(addons) {
            recalculateTotalPrice()
        }
        binding.rvAddonsList.adapter = addonsAdapter
    }

    private fun setupQuantityControls() {
        binding.tvQuantityCount.text = quantityCount.toString()

        binding.btnIncrementQty.setOnClickListener {
            quantityCount++
            binding.tvQuantityCount.text = quantityCount.toString()
            recalculateTotalPrice()
        }

        binding.btnDecrementQty.setOnClickListener {
            if (quantityCount > 1) {
                quantityCount--
                binding.tvQuantityCount.text = quantityCount.toString()
                recalculateTotalPrice()
            }
        }
    }

    private fun setupListeners() {
        binding.btnCustomizationBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnAddToOrder.setOnClickListener {
            val selectedAddons = menuItem?.availableAddons?.filter { it.isSelected }?.map { it.name } ?: emptyList()
            val spiceLevel = when (binding.rgSpiceLevel.checkedRadioButtonId) {
                R.id.rbSpiceMild -> "Mild"
                R.id.rbSpiceHot -> "Spicy"
                else -> "Medium"
            }
            val customNotes = binding.etSpecialInstructions.text.toString().trim()

            Log.i(TAG, "Order Item Confirmed: Qty=$quantityCount, Spice=$spiceLevel, Addons=$selectedAddons, Notes=$customNotes")
            
            // UPDATE SHARED CART: This updates the quantity in the parent fragment's menu
            menuItem?.itemId?.let { id ->
                orderTakingViewModel.setItemQuantity(id, quantityCount)
            }

            Toast.makeText(context, "${menuItem?.itemName} x$quantityCount added!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Dynamically calculates base price + selected add-ons multiplied by item quantity count
     */
    private fun recalculateTotalPrice() {
        val basePrice = menuItem?.basePrice ?: 0.0
        val addonsPriceSum = menuItem?.availableAddons?.filter { it.isSelected }?.sumOf { it.price } ?: 0.0
        val grandTotal = (basePrice + addonsPriceSum) * quantityCount

        binding.btnAddToOrder.text = "Add to Order • ₹${grandTotal.toInt()}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}