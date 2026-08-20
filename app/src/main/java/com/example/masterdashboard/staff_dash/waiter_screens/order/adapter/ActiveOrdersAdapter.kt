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

import com.example.masterdashboard.staff_dash.utils.StatusUIUtils

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

        // Use centralized utility for status UI
        StatusUIUtils.applyStatusUI(context, binding.tvOrderStatusTag, order.status)

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