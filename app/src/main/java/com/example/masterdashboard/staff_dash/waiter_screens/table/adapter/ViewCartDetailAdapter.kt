package com.example.masterdashboard.staff_dash.waiter_screens.table.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemOrderDetailRowBinding
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData

class ViewCartDetailAdapter : ListAdapter<FoodItemData, ViewCartDetailAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemOrderDetailRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CartViewHolder(private val binding: ItemOrderDetailRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FoodItemData) {
            binding.tvExpandedItemName.text = item.name

            // Format example output string layout cleanly: "2 x ₹199"
            binding.tvExpandedItemQtyPrice.text = "${item.currentQuantity} x ₹${item.price}"

            // Calculate aggregate row total metrics directly
            val rowTotal = item.price * item.currentQuantity
            binding.tvExpandedItemRowTotal.text = "₹$rowTotal"

            // Logic to show status of item (New vs Already Served)
            if (item.previousQuantity == 0) {
                binding.tvItemStatusLabel.visibility = android.view.View.VISIBLE
                binding.tvItemStatusLabel.text = "New Item"
                binding.tvItemStatusLabel.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, com.example.masterdashboard.R.color.status_occupied))
            } else if (item.currentQuantity > item.previousQuantity) {
                binding.tvItemStatusLabel.visibility = android.view.View.VISIBLE
                binding.tvItemStatusLabel.text = "Served: ${item.previousQuantity} | New: +${item.currentQuantity - item.previousQuantity}"
                binding.tvItemStatusLabel.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, com.example.masterdashboard.R.color.status_occupied))
            } else {
                binding.tvItemStatusLabel.visibility = android.view.View.VISIBLE
                binding.tvItemStatusLabel.text = "Already Served"
                binding.tvItemStatusLabel.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, com.example.masterdashboard.R.color.search_bar_hint))
            }
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<FoodItemData>() {
        override fun areItemsTheSame(oldItem: FoodItemData, newItem: FoodItemData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FoodItemData, newItem: FoodItemData): Boolean {
            return oldItem == newItem
        }
    }
}