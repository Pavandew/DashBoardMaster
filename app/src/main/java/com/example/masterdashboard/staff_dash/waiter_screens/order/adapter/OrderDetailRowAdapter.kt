package com.example.masterdashboard.staff_dash.waiter_screens.order.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemOrderDetailRowBinding
import com.example.masterdashboard.staff_dash.utils.StatusUIUtils
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.OrderExpandedItemData

class OrderDetailRowAdapter : ListAdapter<OrderExpandedItemData, OrderDetailRowAdapter.RowViewHolder>(RowDiffCallback()) {

    private var currentOrderStatus: ActiveOrderStatus = ActiveOrderStatus.PENDING

    fun updateOrderStatus(status: ActiveOrderStatus) {
        this.currentOrderStatus = status
        notifyDataSetChanged()
    }

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
        val context = binding.root.context

        // Bind raw document structural values to text layout targets smoothly
        val displayName = if (item.variantName.isNotEmpty()) "${item.name} (${item.variantName})" else item.name
        binding.tvExpandedItemName.text = displayName
        binding.tvExpandedItemQtyPrice.text = "${item.quantity} x ₹${item.unitPrice}"
        binding.tvExpandedItemRowTotal.text = "₹${item.totalPrice}"

        // Handle Strike-thru for rejected items
        if (item.status.equals("REJECTED", true)) {
            binding.tvExpandedItemName.paintFlags = binding.tvExpandedItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvExpandedItemName.setTextColor(ContextCompat.getColor(context, R.color.red_alert))
        } else {
            binding.tvExpandedItemName.paintFlags = binding.tvExpandedItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            binding.tvExpandedItemName.setTextColor(ContextCompat.getColor(context, R.color.table_id_text))
        }

        // Apply Item Status UI using centralized utility
        val isNewAddition = item.quantity > item.orderedQuantity
        val delta = item.quantity - item.orderedQuantity
        
        // If the whole order is SERVED, override item status display
        val effectiveStatus = if (currentOrderStatus == ActiveOrderStatus.SERVED) "SERVED" else item.status

        StatusUIUtils.applyItemStatusUI(
            context = context,
            textView = binding.tvItemStatusLabel,
            status = effectiveStatus,
            isNewAddition = isNewAddition,
            delta = delta
        )
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
