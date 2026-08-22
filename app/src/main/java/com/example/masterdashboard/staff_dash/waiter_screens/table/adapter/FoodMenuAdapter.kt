package com.example.masterdashboard.staff_dash.waiter_screens.table.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemMenuCategoryHeaderBinding
import com.example.masterdashboard.databinding.ItemMenuFoodBinding
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuItemType

class FoodMenuAdapter(
    private val onQuantityIncreased: (FoodItemData) -> Unit,
    private val onQuantityDecreased: (FoodItemData) -> Unit,
    private val onItemClick: (FoodItemData) -> Unit = {}
) : ListAdapter<MenuItemType, RecyclerView.ViewHolder>(FoodDiffCallback()) {

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_FOOD = 1
    }

    inner class FoodViewHolder(val binding: ItemMenuFoodBinding) : RecyclerView.ViewHolder(binding.root)
    inner class HeaderViewHolder(val binding: ItemMenuCategoryHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MenuItemType.Header -> TYPE_HEADER
            is MenuItemType.Food -> TYPE_FOOD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = ItemMenuCategoryHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemMenuFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            FoodViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is HeaderViewHolder && item is MenuItemType.Header) {
            // Set category name dynamically
            holder.binding.tvCategoryHeaderName.text = item.name
            Log.d("Order_Flow_Debug", "🔗 [ADAPTER] Binding Header: ${item.name}")
        } else if (holder is FoodViewHolder && item is MenuItemType.Food) {
            val foodItem = item.food
            val binding = holder.binding

            // 1. Bind structural textual data
            binding.tvFoodName.text = foodItem.name
            
            // Show "Starts from" if there are variants
            if (foodItem.hasVariants && foodItem.variantsList.isNotEmpty()) {
                binding.tvFoodPrice.text = "Starts from ₹ ${foodItem.price}"
            } else {
                binding.tvFoodPrice.text = "₹ ${foodItem.price}"
            }

            binding.tvQuantityText.text = foodItem.currentQuantity.toString()

            // 2. Load food image using Glide
            Glide.with(binding.ivFoodImg.context)
                .load(foodItem.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(binding.ivFoodImg)

            // 3. Reactively swap stepper layouts
            if (foodItem.currentQuantity > 0) {
                binding.btnOnlyAdd.visibility = View.GONE
                binding.btnMinus.visibility = View.VISIBLE
                binding.tvQuantityText.visibility = View.VISIBLE
                binding.btnPlus.visibility = View.VISIBLE
            } else {
                binding.btnOnlyAdd.visibility = View.VISIBLE
                binding.btnMinus.visibility = View.GONE
                binding.tvQuantityText.visibility = View.GONE
                binding.btnPlus.visibility = View.GONE
            }

            // 4. Hook up modular layout interaction tracking callbacks
            binding.btnPlus.setOnClickListener { onQuantityIncreased(foodItem) }
            binding.btnOnlyAdd.setOnClickListener { onQuantityIncreased(foodItem) }
            binding.btnMinus.setOnClickListener { onQuantityDecreased(foodItem) }
            
            // 5. Set click listener for item customization
            binding.root.setOnClickListener { onItemClick(foodItem) }
        }
    }

    // High performance UI recalculation utility
    class FoodDiffCallback : DiffUtil.ItemCallback<MenuItemType>() {
        override fun areItemsTheSame(oldItem: MenuItemType, newItem: MenuItemType): Boolean {
            return when {
                oldItem is MenuItemType.Header && newItem is MenuItemType.Header -> oldItem.id == newItem.id
                oldItem is MenuItemType.Food && newItem is MenuItemType.Food -> oldItem.food.id == newItem.food.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: MenuItemType, newItem: MenuItemType): Boolean {
            return oldItem == newItem
        }
    }
}
