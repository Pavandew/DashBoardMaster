package com.example.masterdashboard.staff_dash.kitchen_screens.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemKitchenPreparationDetailRowBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem

class KitchenPreparationDetailAdapter(
    private val onItemToggled: (OrderDetailItem, Boolean) -> Unit
) : ListAdapter<OrderDetailItem, KitchenPreparationDetailAdapter.PrepItemViewHolder>(PrepItemDiffCallback) {

    private var orderStatus: String = "New"

    fun updateOrderStatusContext(newStatus: String) {
        this.orderStatus = newStatus
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrepItemViewHolder {
        val binding = ItemKitchenPreparationDetailRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PrepItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrepItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PrepItemViewHolder(
        private val binding: ItemKitchenPreparationDetailRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrderDetailItem) {
            val displayName = if (item.variantName.isNotEmpty()) "${item.itemName} (${item.variantName})" else item.itemName
            binding.tvExpandedItemName.text = displayName
            
            val totalQuantity = item.quantity
            val readyQuantity = item.readyQuantity
            val previouslyOrdered = item.orderedQuantity
            val newDeltaQuantity = totalQuantity - previouslyOrdered
            
            val isFullyReady = readyQuantity >= totalQuantity && totalQuantity > 0

            // If already fully ready, show prepared state
            if (isFullyReady) {
                binding.tvNewItemBadge.visibility = View.GONE
                binding.tvServedItemBadge.visibility = View.GONE
                binding.tvExpandedItemName.alpha = 1.0f
                
                // Show prepared status visually
                binding.tvExpandedItemRowTotal.text = "$totalQuantity x Prepared"
                binding.tvExpandedItemRowTotal.setBackgroundColor(android.graphics.Color.parseColor("#DCFCE7")) // Light Green
                binding.tvExpandedItemRowTotal.setTextColor(android.graphics.Color.parseColor("#16A34A")) // Success Green
                
                // Change background of the entire row to indicate completion
                binding.layoutItemContent.setBackgroundColor(android.graphics.Color.parseColor("#F0FDF4")) // Very Light Green
                
                binding.cbItemSelect.visibility = View.GONE
                binding.root.setOnClickListener(null)
            } else {
                // Not ready, reset background
                binding.layoutItemContent.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                
                // Logic for Badges
                if (newDeltaQuantity > 0) {
                    binding.tvNewItemBadge.visibility = View.VISIBLE
                    binding.tvServedItemBadge.visibility = View.GONE
                    binding.tvExpandedItemName.alpha = 1.0f
                    
                    // Emphasize the new quantity
                    binding.tvExpandedItemRowTotal.text = "$newDeltaQuantity to Prepare"
                    binding.tvExpandedItemRowTotal.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))
                    binding.tvExpandedItemRowTotal.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                } else {
                    binding.tvNewItemBadge.visibility = View.GONE
                    binding.tvServedItemBadge.visibility = View.VISIBLE
                    binding.tvExpandedItemName.alpha = 0.5f
                    
                    // Show total quantity for processed items
                    binding.tvExpandedItemRowTotal.text = "x $totalQuantity"
                    binding.tvExpandedItemRowTotal.setBackgroundColor(android.graphics.Color.parseColor("#F3F4F6"))
                    binding.tvExpandedItemRowTotal.setTextColor(android.graphics.Color.parseColor("#6B7280"))
                }

                val normalizedStatus = orderStatus.lowercase().trim()
                
                if (normalizedStatus == "preparing") {
                    // Only allow selecting items that are NEW
                    if (newDeltaQuantity > 0) {
                        binding.cbItemSelect.visibility = View.VISIBLE
                        binding.cbItemSelect.setOnCheckedChangeListener(null)
                        binding.cbItemSelect.isChecked = false
                        
                        binding.cbItemSelect.setOnCheckedChangeListener { _, isChecked ->
                            onItemToggled(item, isChecked)
                        }

                        binding.root.setOnClickListener {
                            binding.cbItemSelect.isChecked = !binding.cbItemSelect.isChecked
                        }
                    } else {
                        binding.cbItemSelect.visibility = View.INVISIBLE
                        binding.root.setOnClickListener(null)
                    }
                } else {
                    binding.cbItemSelect.visibility = View.GONE
                    binding.root.setOnClickListener(null)
                }
            }
        }
    }

    private object PrepItemDiffCallback : DiffUtil.ItemCallback<OrderDetailItem>() {
        override fun areItemsTheSame(oldItem: OrderDetailItem, newItem: OrderDetailItem): Boolean =
            oldItem.itemName == newItem.itemName

        override fun areContentsTheSame(oldItem: OrderDetailItem, newItem: OrderDetailItem): Boolean =
            oldItem == newItem
    }
}