package com.example.masterdashboard.staff_dash.kitchen_screens.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemKitchenOrderTicketBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.utils.StatusUIUtils
import com.example.masterdashboard.staff_dash.utils.TimeUtils

class KitchenOrderStreamAdapter(private val onItemClicked: (KitchenOrderDetailData) -> Unit) :
    ListAdapter<KitchenOrderDetailData, KitchenOrderStreamAdapter.OrderViewHolder>(OrderDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemKitchenOrderTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClicked)
    }

    class OrderViewHolder(private val binding: ItemKitchenOrderTicketBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: KitchenOrderDetailData, onItemClicked: (KitchenOrderDetailData) -> Unit) {
            val context = binding.root.context
            
            // Simpler ID display matching Detail screen
            val displayId = when {
                order.orderId.contains("-") -> order.orderId.substringAfter("-")
                order.orderId.startsWith("#") -> order.orderId.substring(1)
                else -> {
                    val digits = order.orderId.filter { it.isDigit() }
                    if (digits.length >= 4) digits.takeLast(4) else order.orderId.takeLast(4)
                }
            }
            
            binding.tvOrderId.text = "Order #$displayId"
            binding.tvTicketTable.text = order.tableName

            // Using centralized TimeUtils
            binding.tvTicketTime.text = TimeUtils.getRelativeTime(order.timestamp)

            binding.tvOrderItemsSummary.text = "${order.items.size} Items"
            
            // Using centralized StatusUIUtils
            StatusUIUtils.applyStatusUI(context, binding.tvTicketStatus, order.status, binding.statusBadge)

            binding.root.setOnClickListener { onItemClicked(order) }
        }
    }

    private object OrderDiffCallback : DiffUtil.ItemCallback<KitchenOrderDetailData>() {
        override fun areItemsTheSame(oldItem: KitchenOrderDetailData, newItem: KitchenOrderDetailData): Boolean = oldItem.orderId == newItem.orderId
        override fun areContentsTheSame(oldItem: KitchenOrderDetailData, newItem: KitchenOrderDetailData): Boolean = oldItem == newItem
    }
}