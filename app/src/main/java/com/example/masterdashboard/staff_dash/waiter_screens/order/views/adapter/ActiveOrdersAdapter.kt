package com.example.masterdashboard.staff_dash.waiter_screens.order.views.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemActiveOrderCardBinding
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.models.ActiveOrderCardData
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.models.ActiveOrderStatus

class ActiveOrdersAdapter(
    private val onOrderClicked: (ActiveOrderCardData) -> Unit
) : ListAdapter<ActiveOrderCardData, ActiveOrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(
        p0: ViewGroup,
        p1: Int
    ): OrderViewHolder {
        val binding = ItemActiveOrderCardBinding.inflate(
            LayoutInflater.from(p0.context),
            p0,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        val binding = holder.binding
        val context = binding.root.context

        binding.tvOrderTableId.text = "Table ${order.tableId}"
        binding.tvOrderTicketId.text = order.orderId
        binding.tvOrderTotalItems.text = "${order.totalItems} Items"
        binding.tvOrderTimestamp.text = "₹ ${order.timestamp}" // Handles explicit price text layout injection matching asset

        // Dynamic status layout badge configuration matching visual asset guidelines
        when (order.status) {
            ActiveOrderStatus.PREPARING -> {
                binding.tvOrderStatusTag.text = "• Preparing"
                binding.tvOrderStatusTag.setBackgroundResource(R.drawable.bg_status_preparing)
                binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.status_occupied)) // Reuses your orange color hex
            }
            ActiveOrderStatus.READY -> {
                binding.tvOrderStatusTag.text = "• Ready"
                binding.tvOrderStatusTag.setBackgroundResource(R.drawable.bg_status_ready)
                binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.status_free)) // Reuses your green color hex
            }
            ActiveOrderStatus.SERVED -> {
                binding.tvOrderStatusTag.text = "• Served"
                binding.tvOrderStatusTag.setBackgroundResource(R.drawable.bg_status_served)
                binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.status_billing)) // Charcoal / Dark slate gray
            }
        }
        binding.clOrderCardContent.setOnClickListener {
            onOrderClicked(order)
        }
    }

    inner class OrderViewHolder(val binding: ItemActiveOrderCardBinding) : RecyclerView.ViewHolder(binding.root)

    class OrderDiffCallback : DiffUtil.ItemCallback<ActiveOrderCardData>() {
        override fun areItemsTheSame(oldItem: ActiveOrderCardData, newItem: ActiveOrderCardData): Boolean = oldItem.orderId == newItem.orderId
        override fun areContentsTheSame(oldItem: ActiveOrderCardData, newItem: ActiveOrderCardData): Boolean = oldItem == newItem
    }
}