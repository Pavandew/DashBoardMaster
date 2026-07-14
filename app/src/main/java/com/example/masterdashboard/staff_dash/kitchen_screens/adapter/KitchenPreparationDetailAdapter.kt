package com.example.masterdashboard.staff_dash.kitchen_screens.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemKitchenPreparationDetailRowBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem

class KitchenPreparationDetailAdapter :
    ListAdapter<OrderDetailItem, KitchenPreparationDetailAdapter.PrepItemViewHolder>(PrepItemDiffCallback) {

    // Variable to track the operational lifecycle phase of the parent ticket container document
    private var orderStatus: String = "New"

    /**
     * Dynamically updates the adapter status context when the ticket state changes.
     * This forces the row layouts to show/hide checkboxes instantly with zero page reload lag.
     */
    fun updateOrderStatusContext(newStatus: String) {
        this.orderStatus = newStatus
        notifyDataSetChanged() // Refreshes row visibilities immediately
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
//            binding.tvExpandedItemRowTotal.text = "${item.quantity} x ₹${item.price.toInt()}"

            // 🔄 STATE MACHINE VISIBILITY LOGIC
            if (orderStatus.equals("New", ignoreCase = true)) {
                // Phase 1: Ticket is incoming/unaccepted. Hide checkboxes completely so it looks like a clean summary list.
                binding.cbItemSelect.visibility = View.GONE

                // Clear out listeners so unaccepted tickets cannot be clicked mistakenly
                binding.root.setOnClickListener(null)
            } else {
                // Phase 2: Ticket is Active/InProgress. Expose checkboxes so chefs can tap off dishes on the line.
                binding.cbItemSelect.visibility = View.VISIBLE
                binding.cbItemSelect.isChecked = false // Reset indicator box states

                // Row interaction helper: tapping anywhere on the item row block shifts the check mark status toggles
                binding.root.setOnClickListener {
                    binding.cbItemSelect.isChecked = !binding.cbItemSelect.isChecked
                }
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