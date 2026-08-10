package com.example.masterdashboard.staff_dash.waiter_screens.table.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.masterdashboard.databinding.BottomSheetItemCustomizationBinding
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.AddonsAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.CustomizationItem
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuItemDetailData
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * A bottom sheet for customizing a food item (quantity, variants/sizes, and addons).
 */
class ItemCustomizationBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ItemCustomizationSheet"
        private const val ARG_MENU_ITEM = "ARG_MENU_ITEM"

        fun newInstance(item: MenuItemDetailData): ItemCustomizationBottomSheet {
            return ItemCustomizationBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_MENU_ITEM, item)
                }
            }
        }
    }

    private var _binding: BottomSheetItemCustomizationBinding? = null
    private val binding get() = _binding!!

    private val orderTakingViewModel: OrderTakingViewModel by activityViewModels {
        OrderTakingViewModel.OrderViewModelFactory(OrderTakingRepository())
    }

    private var menuItem: MenuItemDetailData? = null
    private var quantityCount: Int = 1
    private lateinit var customizationAdapter: AddonsAdapter
    private val customizationList = mutableListOf<CustomizationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuItem = arguments?.getSerializable(ARG_MENU_ITEM) as? MenuItemDetailData
            ?: MenuItemDetailData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetItemCustomizationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Customization sheet opened for item: ${menuItem?.itemName}")

        menuItem?.itemId?.let { id ->
            val currentQty = orderTakingViewModel.originalFoodList.find { it.id == id }?.currentQuantity ?: 1
            if (currentQty > 0) {
                quantityCount = currentQty
            }
        }

        prepareCustomizationList()
        setupStaticData()
        setupRecyclerView()
        setupQuantityControls()
        setupListeners()
        recalculateTotalPrice()
    }

    private fun prepareCustomizationList() {
        customizationList.clear()
        
        menuItem?.let { item ->
            // 1. Add Variants (Sizes) - These work like radio buttons
            if (item.hasVariants && item.variants.isNotEmpty()) {
                item.variants.forEachIndexed { index, variant ->
                    customizationList.add(
                        CustomizationItem(
                            id = "v_$index",
                            name = variant.name,
                            price = variant.price,
                            isSelected = variant.isSelected || (index == 0 && item.variants.none { it.isSelected }),
                            isVariant = true
                        )
                    )
                }
            }

            // 2. Add Add-ons - These work like checkboxes
            item.availableAddons.forEachIndexed { index, addon ->
                customizationList.add(
                    CustomizationItem(
                        id = addon.id.ifEmpty { "a_$index" },
                        name = addon.name,
                        price = addon.price,
                        isSelected = addon.isSelected,
                        isVariant = false
                    )
                )
            }
        }
    }

    private fun setupStaticData() {
        menuItem?.let { item ->
            binding.tvFoodItemName.text = item.itemName
            binding.tvFoodBasePrice.text = "₹${item.basePrice.toInt()}"
            binding.tvFoodDescription.text = item.description

            if (item.hasVariants && item.variants.isNotEmpty()) {
                binding.tvCustomizationLabel.text = "Select Size & Add-ons"
            } else {
                binding.tvCustomizationLabel.text = "Add Add-ons"
            }
        }
    }

    private fun setupRecyclerView() {
        customizationAdapter = AddonsAdapter(customizationList) {
            recalculateTotalPrice()
        }
        binding.rvAddonsList.adapter = customizationAdapter
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
        binding.btnAddToOrder.setOnClickListener {
            val selectedVariant = customizationList.find { it.isVariant && it.isSelected }
            val selectedAddons = customizationList.filter { !it.isVariant && it.isSelected }.map { it.name }
            val customNotes = binding.etSpecialInstructions.text.toString().trim()

            Log.i(TAG, "Order Item Confirmed: Variant=${selectedVariant?.name}, Qty=$quantityCount, Addons=$selectedAddons, Notes=$customNotes")
            
            menuItem?.itemId?.let { id ->
                orderTakingViewModel.setItemCustomization(
                    foodId = id,
                    quantity = quantityCount,
                    variant = selectedVariant?.name ?: "",
                    addons = selectedAddons,
                    variantPrice = selectedVariant?.price?.toInt()
                )
            }

            Toast.makeText(context, "${menuItem?.itemName} x$quantityCount added!", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun recalculateTotalPrice() {
        val selectedVariant = customizationList.find { it.isVariant && it.isSelected }
        
        // If variant exists, use variant price. Otherwise, use item base price.
        val itemPrice = selectedVariant?.price ?: (menuItem?.basePrice ?: 0.0)
        
        val addonsPriceSum = customizationList.filter { !it.isVariant && it.isSelected }.sumOf { it.price }
        val grandTotal = (itemPrice + addonsPriceSum) * quantityCount

        binding.btnAddToOrder.text = "Add to Order • ₹${grandTotal.toInt()}"
        
        // Update price display in header if variant changes
        if (selectedVariant != null) {
            binding.tvFoodBasePrice.text = "₹${selectedVariant.price.toInt()}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
