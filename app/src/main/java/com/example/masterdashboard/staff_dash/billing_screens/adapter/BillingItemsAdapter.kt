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
            binding.tvExpandedItemName.text = item.itemName
            binding.tvExpandedItemQtyPrice.text = "${item.quantity} x ₹${item.price}"
            binding.tvExpandedItemRowTotal.text = "₹${item.rowTotal}"
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<OrderItemModel>() {
        override fun areItemsTheSame(oldItem: OrderItemModel, newItem: OrderItemModel) =
            oldItem.itemId == newItem.itemId
        override fun areContentsTheSame(oldItem: OrderItemModel, newItem: OrderItemModel) =
            oldItem == newItem
    }
}