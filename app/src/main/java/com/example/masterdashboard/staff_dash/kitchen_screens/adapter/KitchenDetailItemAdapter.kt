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
            binding.tvItemName.text = item.itemName
            
            // Format price as Integer for display (e.g. 200 instead of 200.0)
            val unitPrice = item.price.toInt()
            
            // Show only the "Delta" (New items to be prepared) with price for size context
            val newQuantity = item.quantity - item.orderedQuantity
            if (newQuantity > 0) {
                binding.tvQuantity.text = "$newQuantity x $unitPrice"
            } else {
                // If this item was fully prepared before, show total quantity
                binding.tvQuantity.text = "${item.quantity} x $unitPrice"
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