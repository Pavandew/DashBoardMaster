package com.example.masterdashboard.staff_dash.kitchen_screens.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemKitchenPreparationDetailRowBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem

class KitchenPreparationDetailAdapter(
    private val onItemToggled: (OrderDetailItem, Boolean) -> Unit
) : ListAdapter<OrderDetailItem, KitchenPreparationDetailAdapter.PrepItemViewHolder>(PrepItemDiffCallback) {

    private var orderStatus: String = "New"

    fun updateOrderStatusContext(newStatus: String) {
        this.orderStatus = newStatus
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrepItemViewHolder {
        val binding = ItemKitchenPreparationDetailRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PrepItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrepItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PrepItemViewHolder(
        private val binding: ItemKitchenPreparationDetailRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrderDetailItem) {
            binding.tvExpandedItemName.text = item.itemName
            
            // Show ready status if partially prepared
            if (item.readyQuantity > 0) {
                binding.tvExpandedItemRowTotal.text = "${item.readyQuantity}/${item.quantity} Ready"
            } else {
                binding.tvExpandedItemRowTotal.text = "x ${item.quantity}"
            }

            val normalizedStatus = orderStatus.lowercase().trim()
            
            // If already fully ready, don't show checkbox
            if (item.readyQuantity >= item.quantity) {
                binding.cbItemSelect.visibility = View.INVISIBLE
                binding.root.setOnClickListener(null)
            } else if (normalizedStatus == "preparing") {
                binding.cbItemSelect.visibility = View.VISIBLE
                binding.cbItemSelect.setOnCheckedChangeListener(null)
                binding.cbItemSelect.isChecked = false
                
                binding.cbItemSelect.setOnCheckedChangeListener { _, isChecked ->
                    onItemToggled(item, isChecked)
                }

                binding.root.setOnClickListener {
                    binding.cbItemSelect.isChecked = !binding.cbItemSelect.isChecked
                }
            } else {
                binding.cbItemSelect.visibility = View.GONE
                binding.root.setOnClickListener(null)
            }
        }
    }

    private object PrepItemDiffCallback : DiffUtil.ItemCallback<OrderDetailItem>() {
        override fun areItemsTheSame(oldItem: OrderDetailItem, newItem: OrderDetailItem): Boolean =
            oldItem.itemName == newItem.itemName

        override fun areContentsTheSame(oldItem: OrderDetailItem, newItem: OrderDetailItem): Boolean =
            oldItem == newItem
    }
}