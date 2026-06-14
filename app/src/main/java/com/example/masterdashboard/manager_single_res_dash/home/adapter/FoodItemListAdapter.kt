package com.example.masterdashboard.manager_single_res_dash.home.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemFoodDishBinding // Double check this matches your row layout XML filename
import com.example.masterdashboard.manager_single_res_dash.home.models.MenuFoodItemsData

class FoodItemListAdapter(
    private val onItemClick: (MenuFoodItemsData) -> Unit,
    private val onItemLongClick: (MenuFoodItemsData) -> Unit
) : ListAdapter<MenuFoodItemsData, FoodItemListAdapter.FoodItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodItemViewHolder {
        val binding = ItemFoodDishBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FoodItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodItemViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick, onItemLongClick)
    }

    class FoodItemViewHolder(
        private val binding: ItemFoodDishBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: MenuFoodItemsData,
            onItemClick: (MenuFoodItemsData) -> Unit,
            onItemLongClick: (MenuFoodItemsData) -> Unit
        ) {
            val context = itemView.context
            
            Log.d("FoodItemListAdapter", "Binding Item: ${item.itemName} | isVeg: ${item.isVeg}")

            // Bind textual data elements
            binding.tvFoodName.text = item.itemName
            binding.tvFoodPrice.text = context.getString(R.string.price_format, item.price)
            binding.tvStatusText.text = item.status

            // Handle active/inactive indicator state backgrounds dynamically
            if (item.status.equals("Active", ignoreCase = true)) {
                binding.viewStatusDot.setBackgroundResource(R.color.green)
                binding.llStatusBadge.setBackgroundResource(R.drawable.bg_status_active)
                binding.tvStatusText.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            } else {
                binding.llStatusBadge.setBackgroundResource(R.drawable.bg_status_inactive)
                binding.viewStatusDot.setBackgroundResource(R.color.red)
                binding.tvStatusText.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            }
            // example: Glide.with(context).load(item.imageUrl).placeholder(R.drawable.app_logo).into(binding.ivFoodImage)

            // FIXED: Correctly Resolve Colors for Veg / Non-Veg Indicators
            if (item.isVeg) {
                val vegColorValue = ContextCompat.getColor(context, android.R.color.holo_green_dark)

                // 1. Set outer square border stroke color
                binding.cardFoodTypeIndicator.strokeColor = vegColorValue
                // 2. Standard Views expect a direct resolved color integer values payload
                binding.viewFoodTypeDot.setBackgroundColor(vegColorValue)
            } else {
                val nonVegColorValue = ContextCompat.getColor(context, android.R.color.holo_red_dark)

                // 1. Set outer square border stroke color
                binding.cardFoodTypeIndicator.strokeColor = nonVegColorValue
                // 2. Set inner core dot to holo_red_dark color state value
                binding.viewFoodTypeDot.setBackgroundColor(nonVegColorValue)
            }

            // single top to edit
            binding.root.setOnClickListener { onItemClick(item) }

            // long tap trigger for deletion
            binding.root.setOnLongClickListener {
                onItemLongClick(item)
                true // Returns true to consume the click event so it doesn't trigger standard setOnClickListener
            }


        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MenuFoodItemsData>() {
        override fun areItemsTheSame(oldItem: MenuFoodItemsData, newItem: MenuFoodItemsData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuFoodItemsData, newItem: MenuFoodItemsData): Boolean {
            return oldItem == newItem
        }
    }
}