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
            val ts = order.timestamp
            if (ts != null) {
                val durationMillis = System.currentTimeMillis() - ts.toDate().time
                val min = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(durationMillis)
                val hr = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(durationMillis)
                val dy = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(durationMillis)

                binding.tvTicketTime.text = when {
                    dy > 0 -> "$dy d ago"
                    hr > 0 -> "$hr h ago"
                    else -> "$min min ago"
                }
            } else {
                binding.tvTicketTime.text = "Just now"
            }
            // OPTION A: If you want to show total unique rows count (e.g., 3 unique types of dishes)
            binding.tvOrderItemsSummary.text = "${order.items.size} Items"
            // Contextual Status Badge Coloring (Light Theme)
            when (order.status.lowercase().trim()) {
                "new", "pending" -> {
                    binding.statusBadge.setCardBackgroundColor(Color.parseColor("#E0F2FE")) // Light Blue
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#0369A1"))
                }
                "preparing" -> {
                    binding.statusBadge.setCardBackgroundColor(Color.parseColor("#FEF3C7")) // Light Amber
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#92400E"))
                }
                "ready" -> {
                    binding.statusBadge.setCardBackgroundColor(Color.parseColor("#DCFCE7")) // Light Green
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#15803D"))
                }
                "rejected" -> {
                    binding.statusBadge.setCardBackgroundColor(Color.parseColor("#FEE2E2")) // Light Red
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#B91C1C"))
                }
                else -> {
                    binding.statusBadge.setCardBackgroundColor(Color.parseColor("#F3F4F6")) // Light Gray
                    binding.tvTicketStatus.setTextColor(Color.parseColor("#374151"))
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