package com.example.masterdashboard.staff_dash.kitchen_screens.adapter

import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemKitchenOrderTicketBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData

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
            binding.tvOrderId.text = "Order #${order.orderId.takeLast(4).uppercase()}"
//            binding.tvOrderItemsSummary.text = order.itemsSummary
            binding.tvTicketTable.text = order.tableName
            binding.tvTicketStatus.text = order.status

            // Clean Operational Time Formatting Logic safely
            val timestampMs = order.timestamp?.toDate()?.time
            if (timestampMs != null) {
                binding.tvTicketTime.text = DateUtils.getRelativeTimeSpanString(
                    timestampMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                )
            } else {
                binding.tvTicketTime.text = "Just now"
            }
            // OPTION A: If you want to show total unique rows count (e.g., 3 unique types of dishes)
            binding.tvOrderItemsSummary.text = "${order.items.size} Items"
            // Contextual Status Badge Coloring
            val context = binding.root.context
            when (order.status.lowercase()) {
                "new" -> {
                    binding.tvTicketStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
                }
                "preparing" -> {
                    binding.statusBadge.setCardBackgroundColor(Color.parseColor("#1A2F1C"))
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#81C784"))
                }
                else -> {
                    binding.statusBadge.setCardBackgroundColor(Color.parseColor("#2C2C2C"))
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#9E9E9E"))
                }
            }

            binding.root.setOnClickListener { onItemClicked(order) }
        }
    }

    private object OrderDiffCallback : DiffUtil.ItemCallback<KitchenOrderDetailData>() {
        override fun areItemsTheSame(oldItem: KitchenOrderDetailData, newItem: KitchenOrderDetailData): Boolean = oldItem.orderId == newItem.orderId
        override fun areContentsTheSame(oldItem: KitchenOrderDetailData, newItem: KitchenOrderDetailData): Boolean = oldItem == newItem
    }
}