package com.example.masterdashboard.staff_dash.kitchen_screens.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem

class KitchenRejectItemsAdapter(
    private val items: List<OrderDetailItem>
) : RecyclerView.Adapter<KitchenRejectItemsAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<OrderDetailItem>()

    fun getSelectedItems(): List<OrderDetailItem> = selectedItems.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_kitchen_reject_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbSelectItem: CheckBox = itemView.findViewById(R.id.cbSelectItem)
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)

        fun bind(item: OrderDetailItem) {
            // Log to check if data is coming correctly
            Log.d("KitchenRejectAdapter", "Binding item: name='${item.itemName}', variant='${item.variantName}', qty=${item.quantity}")
            
            val displayName = when {
                item.itemName.isNullOrBlank() -> "Unnamed Item"
                item.variantName.isNotEmpty() -> "${item.itemName} (${item.variantName})"
                else -> item.itemName
            }
            tvItemName.text = displayName
            tvQuantity.text = "x ${item.quantity}"
            
            // Handle checkbox state correctly
            cbSelectItem.setOnCheckedChangeListener(null)
            cbSelectItem.isChecked = selectedItems.contains(item)
            
            cbSelectItem.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(item)
                } else {
                    selectedItems.remove(item)
                }
            }
            
            // Allow clicking the whole row to toggle
            itemView.setOnClickListener {
                cbSelectItem.isChecked = !cbSelectItem.isChecked
            }
        }
    }
}