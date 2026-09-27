package com.restroone.app.staff_dash.kitchen_screens.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.restroone.app.databinding.ItemKitchenPreparationDetailRowBinding
import com.restroone.app.staff_dash.kitchen_screens.model.OrderDetailItem

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
            val displayName = when {
                item.variantName.isEmpty() -> item.itemName
                item.itemName.contains("(${item.variantName})", ignoreCase = true) || item.itemName.contains(item.variantName, ignoreCase = true) -> item.itemName
                else -> "${item.itemName} (${item.variantName})"
            }
            binding.tvExpandedItemName.text = displayName
            
            val totalQuantity = item.quantity
            val readyQuantity = item.readyQuantity
            val previouslyOrdered = item.orderedQuantity
            val remainingToPrepare = maxOf(0, totalQuantity - readyQuantity)
            val newAdditionQuantity = maxOf(0, totalQuantity - previouslyOrdered)
            
            val isFullyReady = readyQuantity >= totalQuantity && totalQuantity > 0

            // If already fully ready, show prepared state
            if (isFullyReady) {
                binding.tvNewItemBadge.visibility = View.GONE
                binding.tvServedItemBadge.visibility = View.GONE
                binding.tvExpandedItemName.alpha = 1.0f
                binding.tvExpandedItemName.text = displayName
                
                // Show prepared status visually
                binding.tvExpandedItemRowTotal.text = "$totalQuantity x Prepared"
                binding.tvExpandedItemRowTotal.setBackgroundColor(android.graphics.Color.parseColor("#DCFCE7")) // Light Green
                binding.tvExpandedItemRowTotal.setTextColor(android.graphics.Color.parseColor("#16A34A")) // Success Green
                
                // Change background of the entire row to indicate completion
                binding.layoutItemContent.setBackgroundColor(android.graphics.Color.parseColor("#F0FDF4")) // Very Light Green
                
                binding.cbItemSelect.visibility = View.GONE
                binding.root.setOnClickListener(null)
            } else {
                // Not fully ready, reset row background
                binding.layoutItemContent.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                binding.tvExpandedItemName.alpha = 1.0f

                // Show status in right-hand pill
                val pillText = if (readyQuantity > 0) {
                    "$remainingToPrepare to Prepare ($readyQuantity Ready)"
                } else {
                    "$remainingToPrepare to Prepare"
                }
                binding.tvExpandedItemRowTotal.text = pillText
                binding.tvExpandedItemRowTotal.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2")) // Light Red
                binding.tvExpandedItemRowTotal.setTextColor(android.graphics.Color.parseColor("#EF4444")) // Red Text

                // Badges for New Additions vs Partial Preparation
                if (newAdditionQuantity > 0) {
                    binding.tvNewItemBadge.visibility = View.VISIBLE
                    binding.tvNewItemBadge.text = "+$newAdditionQuantity NEW ITEM ADDED"

                    if (readyQuantity > 0) {
                        binding.tvServedItemBadge.visibility = View.VISIBLE
                        binding.tvServedItemBadge.text = "$readyQuantity PREPARED EARLIER"
                    } else {
                        binding.tvServedItemBadge.visibility = View.GONE
                    }
                } else {
                    binding.tvNewItemBadge.visibility = View.GONE
                    if (readyQuantity > 0) {
                        binding.tvServedItemBadge.visibility = View.VISIBLE
                        binding.tvServedItemBadge.text = "$readyQuantity OF $totalQuantity PREPARED"
                    } else {
                        binding.tvServedItemBadge.visibility = View.GONE
                    }
                }

                val normalizedStatus = orderStatus.lowercase().trim()
                val isEditableState = normalizedStatus == "preparing" || normalizedStatus == "new"
                
                if (isEditableState && remainingToPrepare > 0) {
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