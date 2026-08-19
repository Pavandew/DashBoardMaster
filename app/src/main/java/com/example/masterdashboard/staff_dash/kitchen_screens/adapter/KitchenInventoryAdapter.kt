package com.example.masterdashboard.staff_dash.kitchen_screens.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemInventoryBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.model.InventoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KitchenInventoryAdapter(
    private val onItemClick: (InventoryItem) -> Unit
) : ListAdapter<InventoryItem, KitchenInventoryAdapter.InventoryViewHolder>(InventoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val binding = ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InventoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InventoryViewHolder(private val binding: ItemInventoryBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * Binds inventory data to the item view and handles status styling.
         */
        fun bind(item: InventoryItem) {
            binding.tvItemName.text = item.itemName
            binding.tvCategory.text = item.inventoryCategory
            binding.tvQuantity.text = "${item.itemQuantity} ${item.itemUnit}"
            
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            binding.tvLastUpdated.text = "Updated: ${sdf.format(Date(item.lastUpdated))}"

            val status = item.getStockStatus()
            binding.tvStockStatus.text = status
            
            // Apply color coding based on stock level status
            when (status) {
                "In Stock" -> {
                    binding.tvStockStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.green))
                    binding.tvStockStatus.setBackgroundResource(R.drawable.bg_status_active)
                }
                "Low Stock" -> {
                    binding.tvStockStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.accent_orange))
                    binding.tvStockStatus.setBackgroundResource(R.drawable.bg_status_preparing)
                }
                "Out of Stock" -> {
                    binding.tvStockStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.red))
                    binding.tvStockStatus.setBackgroundResource(R.drawable.bg_status_occupied)
                }
            }

            // Logic for "X days left"
            if (item.estimatedDaysLeft > 0) {
                binding.tvStockDays.visibility = android.view.View.VISIBLE
                binding.tvStockDays.text = "${item.estimatedDaysLeft} days left"
            } else if (item.itemQuantity <= item.minThreshold && item.itemQuantity > 0) {
                binding.tvStockDays.visibility = android.view.View.VISIBLE
                binding.tvStockDays.text = "Low stock alert!"
            } else {
                binding.tvStockDays.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { 
                Log.d("KitchenInventoryAdapter", "Clicked on item: ${item.itemName}")
                onItemClick(item) 
            }
        }
    }

    class InventoryDiffCallback : DiffUtil.ItemCallback<InventoryItem>() {
        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
            return oldItem.inventoryId == newItem.inventoryId
        }

        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
            return oldItem == newItem
        }
    }
}
