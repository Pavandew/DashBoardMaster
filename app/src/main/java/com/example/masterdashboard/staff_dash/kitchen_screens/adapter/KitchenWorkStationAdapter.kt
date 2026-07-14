package com.example.masterdashboard.staff_dash.kitchen_screens.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
            // Bind exact view targets matching your layout text item fields
            binding.tvOrderId.text = "#ORD-${order.orderId.takeLast(4).uppercase()}"
            binding.tvStatus.text = order.status
            binding.tvTable.text = "Table ${order.tableName.ifEmpty { "N/A" }}"
            binding.tvItems.text = "${order.items.size} Items"

            // Compute precise operational elapsed runtime duration safely from Firestore Timestamp
            if (order.timestamp != null) {
                val durationMillis = System.currentTimeMillis() - order.timestamp.toDate().time
                val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
                binding.tvTime.text = "$elapsedMinutes min"
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