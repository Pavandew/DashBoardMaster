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
            val context = binding.root.context
            val displayName = if (item.variantName.isNotEmpty()) "${item.name} (${item.variantName})" else item.name
            binding.tvExpandedItemName.text = displayName

            // Format example output string layout cleanly: "2 x ₹199"
            binding.tvExpandedItemQtyPrice.text = "${item.currentQuantity} x ₹${item.price}"

            // Calculate aggregate row total metrics directly
            val rowTotal = item.price * item.currentQuantity
            binding.tvExpandedItemRowTotal.text = "₹$rowTotal"

            // Reset UI state for the card
            binding.cardItemRoot.alpha = 1.0f
            binding.cardItemRoot.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(context, com.example.masterdashboard.R.color.white))

            // Logic to show status of item (New vs Already Served)
            when {
                item.previousQuantity == 0 -> {
                    // Entirely new item in the cart
                    binding.tvItemStatusLabel.visibility = android.view.View.VISIBLE
                    binding.tvItemStatusLabel.text = "New Item"
                    binding.tvItemStatusLabel.setTextColor(androidx.core.content.ContextCompat.getColor(context, com.example.masterdashboard.R.color.status_occupied))
                    binding.viewStatusStrip.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, com.example.masterdashboard.R.color.accent_blue))
                }
                item.currentQuantity > item.previousQuantity -> {
                    // Some units already sent, some are new additions
                    binding.tvItemStatusLabel.visibility = android.view.View.VISIBLE
                    binding.tvItemStatusLabel.text = "Served: ${item.previousQuantity} | New: +${item.currentQuantity - item.previousQuantity}"
                    binding.tvItemStatusLabel.setTextColor(androidx.core.content.ContextCompat.getColor(context, com.example.masterdashboard.R.color.status_occupied))
                    binding.viewStatusStrip.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, com.example.masterdashboard.R.color.accent_orange))
                }
                else -> {
                    // Item was already sent to kitchen/served
                    binding.tvItemStatusLabel.visibility = android.view.View.VISIBLE
                    binding.tvItemStatusLabel.text = "Previously Sent"
                    binding.tvItemStatusLabel.setTextColor(androidx.core.content.ContextCompat.getColor(context, com.example.masterdashboard.R.color.gray_text))
                    binding.viewStatusStrip.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, com.example.masterdashboard.R.color.chip_border))
                    binding.cardItemRoot.alpha = 0.8f
                }
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