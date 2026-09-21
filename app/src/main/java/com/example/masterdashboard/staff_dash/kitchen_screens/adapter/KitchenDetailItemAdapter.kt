package com.example.masterdashboard.staff_dash.kitchen_screens.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemKitchenDetailRowBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem

/**
 * Adapter specifically for the Kitchen Detail screen.
 * Shows only the Item Name and Quantity, as the chef doesn't need to see prices.
 */
class KitchenDetailItemAdapter : ListAdapter<OrderDetailItem, KitchenDetailItemAdapter.ItemViewHolder>(ItemDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemKitchenDetailRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewHolder(private val binding: ItemKitchenDetailRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OrderDetailItem) {
            val displayName = when {
                item.variantName.isEmpty() -> item.itemName
                item.itemName.contains("(${item.variantName})", ignoreCase = true) || item.itemName.contains(item.variantName, ignoreCase = true) -> item.itemName
                else -> "${item.itemName} (${item.variantName})"
            }
            binding.tvItemName.text = displayName
            
            // Format price as Integer for display (e.g. 200 instead of 200.0)
            val unitPrice = item.price.toInt()
            
            val totalQuantity = item.quantity
            val previouslyOrdered = item.orderedQuantity
            val newDeltaQuantity = maxOf(0, totalQuantity - previouslyOrdered)

            if (newDeltaQuantity > 0) {
                // This item has NEW quantities to be prepared in this KOT
                binding.tvNewItemBadge.visibility = android.view.View.VISIBLE
                binding.tvServedItemBadge.visibility = android.view.View.GONE
                
                // Emphasize the new quantity to prepare
                binding.tvQuantity.text = "$newDeltaQuantity x $unitPrice"
                binding.tvQuantity.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))
                binding.tvQuantity.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                
                binding.tvItemName.alpha = 1.0f

                if (previouslyOrdered > 0) {
                    binding.tvItemName.text = "$displayName\n(Prev Ordered: $previouslyOrdered)"
                } else {
                    binding.tvItemName.text = displayName
                }
            } else {
                // This item was already accepted / ordered
                binding.tvNewItemBadge.visibility = android.view.View.GONE
                binding.tvServedItemBadge.visibility = android.view.View.VISIBLE
                
                binding.tvQuantity.text = "$totalQuantity x $unitPrice"
                binding.tvQuantity.setBackgroundColor(android.graphics.Color.parseColor("#F3F4F6"))
                binding.tvQuantity.setTextColor(android.graphics.Color.parseColor("#6B7280"))
                binding.tvItemName.text = displayName
                binding.tvItemName.alpha = 0.5f
            }
        }
    }

    private object ItemDiffCallback : DiffUtil.ItemCallback<OrderDetailItem>() {
        override fun areItemsTheSame(oldItem: OrderDetailItem, newItem: OrderDetailItem): Boolean = 
            oldItem.itemName == newItem.itemName
        override fun areContentsTheSame(oldItem: OrderDetailItem, newItem: OrderDetailItem): Boolean = 
            oldItem == newItem
    }
}