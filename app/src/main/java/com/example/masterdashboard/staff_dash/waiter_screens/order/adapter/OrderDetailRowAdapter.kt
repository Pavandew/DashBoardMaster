package com.example.masterdashboard.staff_dash.waiter_screens.order.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemOrderDetailRowBinding
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.OrderExpandedItemData

class OrderDetailRowAdapter : ListAdapter<OrderExpandedItemData, OrderDetailRowAdapter.RowViewHolder>(RowDiffCallback()) {

    // View Holder class holding item bindings securely in memory
    inner class RowViewHolder(val binding: ItemOrderDetailRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val binding = ItemOrderDetailRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        // Bind raw document structural values to text layout targets smoothly
        binding.tvExpandedItemName.text = item.name
        binding.tvExpandedItemQtyPrice.text = "${item.quantity} x ₹${item.unitPrice}"
        binding.tvExpandedItemRowTotal.text = "₹${item.totalPrice}"
    }

    // High-performance DiffUtil callback to optimize layout element item changes dynamically
    class RowDiffCallback : DiffUtil.ItemCallback<OrderExpandedItemData>() {
        override fun areItemsTheSame(oldItem: OrderExpandedItemData, newItem: OrderExpandedItemData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OrderExpandedItemData, newItem: OrderExpandedItemData): Boolean {
            return oldItem == newItem
        }
    }
}