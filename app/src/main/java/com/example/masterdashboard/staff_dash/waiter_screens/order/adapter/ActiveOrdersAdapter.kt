package com.example.masterdashboard.staff_dash.waiter_screens.order.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemActiveOrderCardBinding
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderCardData
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus

class ActiveOrdersAdapter(
    private val onOrderClicked: (ActiveOrderCardData) -> Unit
) : ListAdapter<ActiveOrderCardData, ActiveOrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderViewHolder {
        val binding = ItemActiveOrderCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        val binding = holder.binding
        val context = binding.root.context

        // Clean Table Name Formatting: Avoids duplicate "Table T-1" prefixes
        val formattedTable = if (order.tableName.startsWith("Table", ignoreCase = true)) {
            order.tableName
        } else {
            "Table ${order.tableName}"
        }

        binding.tvOrderTableName.text = formattedTable
        binding.tvOrderTicketId.text = order.orderId
        binding.tvOrderTotalItems.text = "${order.totalItems} Items"
        binding.tvOrderTimestamp.text = order.orderTime

        // Dynamic status layout badge configuration matching visual asset guidelines
        when (order.status) {
            ActiveOrderStatus.PENDING -> {
                binding.tvOrderStatusTag.text = "• Pending"
                binding.tvOrderStatusTag.setBackgroundResource(R.drawable.bg_status_preparing) // e.g., Light Yellow / Amber
                binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.status_occupied))
            }
            ActiveOrderStatus.PREPARING -> {
                binding.tvOrderStatusTag.text = "• Preparing"
                binding.tvOrderStatusTag.setBackgroundResource(R.drawable.bg_status_preparing) // Orange
                binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.status_occupied))
            }
            ActiveOrderStatus.READY -> {
                binding.tvOrderStatusTag.text = "• Ready"
                binding.tvOrderStatusTag.setBackgroundResource(R.drawable.bg_status_ready) // Green
                binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.status_free))
            }
            ActiveOrderStatus.SERVED -> {
                binding.tvOrderStatusTag.text = "• Served"
                binding.tvOrderStatusTag.setBackgroundResource(R.drawable.bg_status_served) // Charcoal
                binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.status_billing))
            }
            ActiveOrderStatus.BILLING -> {
                binding.tvOrderStatusTag.text = "• Billing"
                binding.tvOrderStatusTag.setBackgroundResource(R.drawable.bg_status_preparing)
                binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.status_occupied))
            }
            ActiveOrderStatus.PAID -> {
                binding.tvOrderStatusTag.text = "• Paid"
                binding.tvOrderStatusTag.setBackgroundResource(R.drawable.bg_status_ready)
                binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.status_free))
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