package com.restroone.app.staff_dash.kitchen_screens.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.restroone.app.databinding.ItemKitchenDetailRowBinding
import com.restroone.app.staff_dash.kitchen_screens.model.OrderDetailItem

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
            
            val totalQuantity = item.quantity
            val previouslyOrdered = item.orderedQuantity
            val newDeltaQuantity = maxOf(0, totalQuantity - previouslyOrdered)

            if (newDeltaQuantity > 0) {
                // NEW item or additional quantity needing kitchen preparation
                binding.tvNewItemBadge.text = if (newDeltaQuantity > 1) "+$newDeltaQuantity NEW ITEMS ADDED" else "+1 NEW ITEM ADDED"
                binding.tvNewItemBadge.visibility = android.view.View.VISIBLE
                binding.tvServedItemBadge.visibility = android.view.View.GONE
                
                binding.tvQuantity.text = "$newDeltaQuantity to Prepare"
                binding.tvQuantity.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))
                binding.tvQuantity.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                
                binding.layoutItemContent.setBackgroundColor(android.graphics.Color.parseColor("#FFF5F5"))
                binding.tvItemName.alpha = 1.0f

                if (previouslyOrdered > 0) {
                    binding.tvItemName.text = "$displayName\n(Prev Ordered: $previouslyOrdered)"
                } else {
                    binding.tvItemName.text = displayName
                }
            } else {
                // Previously prepared or served items
                binding.tvNewItemBadge.visibility = android.view.View.GONE
                
                val statusText = if (item.itemStatus.equals("SERVED", true)) "PREVIOUSLY SERVED" else "PREVIOUSLY PREPARED"
                binding.tvServedItemBadge.text = statusText
                binding.tvServedItemBadge.visibility = android.view.View.VISIBLE
                
                binding.tvQuantity.text = "$totalQuantity x Prepared"
                binding.tvQuantity.setBackgroundColor(android.graphics.Color.parseColor("#F3F4F6"))
                binding.tvQuantity.setTextColor(android.graphics.Color.parseColor("#6B7280"))
                
                binding.layoutItemContent.setBackgroundColor(android.graphics.Color.parseColor("#FAFAFA"))
                binding.tvItemName.text = displayName
                binding.tvItemName.alpha = 0.55f
            }
        }
    }

    private object ItemDiffCallback : DiffUtil.ItemCallback<OrderDetailItem>() {
        override fun areItemsTheSame(oldItem: OrderDetailItem, newItem: OrderDetailItem): Boolean = 
            (if (oldItem.itemId.isNotEmpty()) oldItem.itemId else "${oldItem.itemName}_${oldItem.variantName}") == 
            (if (newItem.itemId.isNotEmpty()) newItem.itemId else "${newItem.itemName}_${newItem.variantName}")
        override fun areContentsTheSame(oldItem: OrderDetailItem, newItem: OrderDetailItem): Boolean = 
            oldItem == newItem
    }
}