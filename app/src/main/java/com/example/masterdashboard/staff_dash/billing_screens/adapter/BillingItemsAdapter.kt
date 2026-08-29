package com.example.masterdashboard.staff_dash.billing_screens.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemOrderDetailRowBinding
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderItemModel

class BillingItemsAdapter : ListAdapter<OrderItemModel, BillingItemsAdapter.ItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemOrderDetailRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewHolder(private val binding: ItemOrderDetailRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OrderItemModel) {
            val context = binding.root.context
            val displayName = if (item.variantName.isNotEmpty()) "${item.itemName} (${item.variantName})" else item.itemName
            binding.tvExpandedItemName.text = displayName
            binding.tvExpandedItemQtyPrice.text = "${item.quantity} x ₹${item.price}"
            binding.tvExpandedItemRowTotal.text = "₹${item.rowTotal}"

            // RESET SHARED UI COMPONENTS
            // Ensure white background and full visibility for the settlement screen
            binding.cardItemRoot.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.white))
            binding.cardItemRoot.alpha = 1.0f
            
            // Hide the status strip and label as they are not needed on the "Settle Bill" screen
            binding.viewStatusStrip.visibility = android.view.View.GONE
            binding.tvItemStatusLabel.visibility = android.view.View.GONE
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<OrderItemModel>() {
        override fun areItemsTheSame(oldItem: OrderItemModel, newItem: OrderItemModel) =
            oldItem.itemId == newItem.itemId
        override fun areContentsTheSame(oldItem: OrderItemModel, newItem: OrderItemModel) =
            oldItem == newItem
    }
}