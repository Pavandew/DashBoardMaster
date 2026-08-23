package com.example.masterdashboard.staff_dash.waiter_screens.table.utils

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.example.masterdashboard.databinding.BottomSheetItemCustomizationBinding
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.AddonsAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.CustomizationItem
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.AddonItem
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.manager_single_res_dash.models.ItemVariant

/**
 * Manages the "Instant Overlay" for food item customization.
 * Decouples UI logic from the Fragment for cleaner code.
 */
class ItemCustomizationOverlayManager(
    private val context: Context,
    private val binding: BottomSheetItemCustomizationBinding,
    private val overlayContainer: View,
    private val onConfirm: (foodId: String, quantity: Int, variant: String, addons: List<String>, variantPrice: Int?) -> Unit
) {
    private var currentItem: FoodItemData? = null
    private var quantity: Int = 1
    private val customizationList = mutableListOf<CustomizationItem>()
    private val addonsAdapter = AddonsAdapter(customizationList) { recalculatePrice() }

    init {
        setupListeners()
    }

    private fun setupListeners() {
        binding.rvAddonsList.adapter = addonsAdapter

        binding.btnIncrementQty.setOnClickListener {
            quantity++
            updateQuantityText()
        }

        binding.btnDecrementQty.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantityText()
            }
        }

        binding.btnAddToOrder.setOnClickListener {
            val item = currentItem ?: return@setOnClickListener
            val selectedVariant = customizationList.find { it.isVariant && it.isSelected }
            val selectedAddons = customizationList.filter { !it.isVariant && it.isSelected }.map { it.name }
            
            onConfirm(
                item.id,
                quantity,
                selectedVariant?.name ?: "",
                selectedAddons,
                selectedVariant?.price?.toInt()
            )
            hide()
        }

        overlayContainer.setOnClickListener { hide() }
    }

    fun show(item: FoodItemData) {
        currentItem = item
        quantity = if (item.currentQuantity > 0) item.currentQuantity else 1
        
        binding.tvFoodItemName.text = item.name
        binding.etSpecialInstructions.text?.clear()
        updateQuantityText()

        Glide.with(context)
            .load(item.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(binding.ivFoodHeroImage)

        // Populate initial list (Variants + already loaded Addons)
        refreshList(item)

        overlayContainer.visibility = View.VISIBLE
    }

    fun hide() {
        overlayContainer.visibility = View.GONE
        currentItem = null
    }

    fun isVisible(): Boolean = overlayContainer.visibility == View.VISIBLE

    fun getCurrentItemId(): String? = currentItemId()

    private fun currentItemId(): String? = currentItem?.id

    fun refreshList(item: FoodItemData) {
        customizationList.clear()
        
        // 1. Add Variants
        if (item.hasVariants) {
            item.variantsList.forEachIndexed { index, v ->
                customizationList.add(CustomizationItem(
                    id = "v_$index", 
                    name = v.variantName, 
                    price = v.price, 
                    isSelected = (v.variantName == item.variantName) || (index == 0 && item.variantName.isEmpty()),
                    isVariant = true
                ))
            }
        }

        // 2. Add Addons
        item.availableAddons.forEach { addon ->
            customizationList.add(CustomizationItem(
                id = addon.id,
                name = addon.name,
                price = addon.price,
                isSelected = item.selectedAddons.contains(addon.name),
                isVariant = false
            ))
        }

        addonsAdapter.notifyDataSetChanged()

        // Show placeholder if no addons are available
        val hasAddons = item.availableAddons.isNotEmpty()
        binding.rvAddonsList.visibility = if (hasAddons) View.VISIBLE else View.GONE
        binding.tvNoAddons.visibility = if (hasAddons) View.GONE else View.VISIBLE

        recalculatePrice()
    }

    private fun updateQuantityText() {
        binding.tvQuantityCount.text = quantity.toString()
        recalculatePrice()
    }

    private fun recalculatePrice() {
        val selectedVariant = customizationList.find { it.isVariant && it.isSelected }
        val itemPrice = selectedVariant?.price ?: (currentItem?.price?.toDouble() ?: 0.0)
        val addonsPrice = customizationList.filter { !it.isVariant && it.isSelected }.sumOf { it.price }
        
        val total = (itemPrice + addonsPrice) * quantity
        binding.btnAddToOrder.text = "Add • ₹${total.toInt()}"
        binding.tvFoodBasePrice.text = "₹${itemPrice.toInt()}"
    }
}
