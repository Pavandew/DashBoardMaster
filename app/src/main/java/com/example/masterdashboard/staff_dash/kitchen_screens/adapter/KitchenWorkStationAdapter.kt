package com.example.masterdashboard.staff_dash.kitchen_screens.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemKitchenOrderCardBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import java.util.concurrent.TimeUnit

class KitchenWorkstationAdapter(
    private val onCardClick: (KitchenOrderDetailData) -> Unit
) : ListAdapter<KitchenOrderDetailData, KitchenWorkstationAdapter.WorkstationViewHolder>(WorkstationDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkstationViewHolder {
        val binding = ItemKitchenOrderCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkstationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkstationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WorkstationViewHolder(private val binding: ItemKitchenOrderCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: KitchenOrderDetailData) {
            binding.tvOrderId.text = "#ORD-${order.orderId.takeLast(4).uppercase()}"
            binding.tvStatus.text = order.status
            
            // Just use order.tableName directly as it is now pre-formatted by the Repository
            binding.tvTable.text = order.tableName
            
            binding.tvItems.text = "${order.items.size} Items"

            // Status Badge Styling (Light Theme)
            when (order.status.lowercase().trim()) {
                "preparing" -> {
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_preparing)
                    binding.tvStatus.setTextColor(Color.parseColor("#92400E")) // Dark Amber
                }
                "ready" -> {
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_ready)
                    binding.tvStatus.setTextColor(Color.parseColor("#15803D")) // Dark Green
                }
                "completed" -> {
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
                    binding.tvStatus.setTextColor(Color.parseColor("#15803D")) // Dark Green
                }
                "new", "pending" -> {
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_blue)
                    binding.tvStatus.setTextColor(Color.parseColor("#0369A1")) // Dark Blue
                }
                else -> {
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
                    binding.tvStatus.setTextColor(Color.parseColor("#374151")) // Dark Gray
                }
            }

            val ts = order.timestamp
            if (ts != null) {
                val durationMillis = System.currentTimeMillis() - ts.toDate().time
                val min = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
                val hr = TimeUnit.MILLISECONDS.toHours(durationMillis)
                val dy = TimeUnit.MILLISECONDS.toDays(durationMillis)

                binding.tvTime.text = when {
                    dy > 0 -> "$dy d"
                    hr > 0 -> "$hr h"
                    else -> "$min min"
                }
            } else {
                binding.tvTime.text = "0 min"
            }

            binding.root.setOnClickListener { onCardClick(order) }
        }
    }

    private object WorkstationDiffCallback : DiffUtil.ItemCallback<KitchenOrderDetailData>() {
        override fun areItemsTheSame(oldItem: KitchenOrderDetailData, newItem: KitchenOrderDetailData): Boolean = oldItem.orderId == newItem.orderId
        override fun areContentsTheSame(oldItem: KitchenOrderDetailData, newItem: KitchenOrderDetailData): Boolean = oldItem == newItem
    }
}