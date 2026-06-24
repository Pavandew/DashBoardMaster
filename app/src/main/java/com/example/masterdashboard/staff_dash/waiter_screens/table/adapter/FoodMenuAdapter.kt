package com.example.masterdashboard.staff_dash.waiter_screens.table.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemMenuFoodBinding
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData

class FoodMenuAdapter(
    private val onQuantityIncreased: (FoodItemData) -> Unit,
    private val onQuantityDecreased: (FoodItemData) -> Unit
) : ListAdapter<FoodItemData, FoodMenuAdapter.FoodViewHolder>(FoodDiffCallback()) {

    inner class FoodViewHolder(val binding: ItemMenuFoodBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemMenuFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val foodItem = getItem(position)
        val binding = holder.binding

        // 1. Bind structural textual data
        binding.tvFoodName.text = foodItem.name
        binding.tvFoodPrice.text = "₹ ${foodItem.price}"
        binding.tvQuantityText.text = foodItem.currentQuantity.toString()

        // 2. Load food image using Glide
        Glide.with(binding.ivFoodImg.context)
            .load(foodItem.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .centerCrop()
            .into(binding.ivFoodImg)

        // 3. Reactively swap stepper layouts based on counter metrics matching your UI spec
        if (foodItem.currentQuantity > 0) {
            // Show full minus/quantity/plus controller layout sequence
            binding.btnOnlyAdd.visibility = View.GONE
            binding.btnMinus.visibility = View.VISIBLE
            binding.tvQuantityText.visibility = View.VISIBLE
            binding.btnPlus.visibility = View.VISIBLE
        } else {
            // Collapse down cleanly to show only a single addition indicator tap target
            binding.btnOnlyAdd.visibility = View.VISIBLE
            binding.btnMinus.visibility = View.GONE
            binding.tvQuantityText.visibility = View.GONE
            binding.btnPlus.visibility = View.GONE
        }

        // 4. Hook up modular layout interaction tracking callbacks straight back to ViewModel
        binding.btnPlus.setOnClickListener { onQuantityIncreased(foodItem) }
        binding.btnOnlyAdd.setOnClickListener { onQuantityIncreased(foodItem) }
        binding.btnMinus.setOnClickListener { onQuantityDecreased(foodItem) }
    }

    // High performance UI recalculation utility
    class FoodDiffCallback : DiffUtil.ItemCallback<FoodItemData>() {
        override fun areItemsTheSame(oldItem: FoodItemData, newItem: FoodItemData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FoodItemData, newItem: FoodItemData): Boolean {
            return oldItem == newItem
        }
    }
}
