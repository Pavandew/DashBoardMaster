package com.example.masterdashboard.staff_dash.kitchen_screens.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemOrderDetailRowBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem

class KitchenDetailItemAdapter : ListAdapter<OrderDetailItem, KitchenDetailItemAdapter.ItemViewHolder>(ItemDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemOrderDetailRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewHolder(private val binding: ItemOrderDetailRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OrderDetailItem) {
            // 1. Map the clean black item title name
            binding.tvExpandedItemName.text = item.itemName

            // Mock price value data mapping rule calculations (e.g. ₹199)
            // Replace with your actual model integer parameters if they are stored in the object manifest!
            val fallbackUnitPrice = 199
            val rowTotalPrice = fallbackUnitPrice * item.quantity

            // 2. Map the gray breakdown calculation display layout string: "1 x ₹199"
            binding.tvExpandedItemQtyPrice.text = "${item.quantity} x ₹$fallbackUnitPrice"

            // 3. Map the final row total sum readout to clean black bold values: "₹199"
            binding.tvExpandedItemRowTotal.text = "₹$rowTotalPrice"
        }
    }

    private object ItemDiffCallback : DiffUtil.ItemCallback<OrderDetailItem>() {
        override fun areItemsTheSame(oldItem: OrderDetailItem, newItem: OrderDetailItem): Boolean = oldItem.itemName == newItem.itemName
        override fun areContentsTheSame(oldItem: OrderDetailItem, newItem: OrderDetailItem): Boolean = oldItem == newItem
    }
}