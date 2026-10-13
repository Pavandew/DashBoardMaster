package com.restroone.app.manager_single_res_dash.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.restroone.app.databinding.ItemClosedOrderCardBinding
import com.restroone.app.manager_single_res_dash.models.CompletedOrderHistoryModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ClosedOrdersAdapter(
    private val onOrderClick: (CompletedOrderHistoryModel) -> Unit
) : ListAdapter<CompletedOrderHistoryModel, ClosedOrdersAdapter.ClosedOrderViewHolder>(DiffCallback) {

    private val currencyFormatter by lazy { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    private val dateTimeFormatter by lazy { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClosedOrderViewHolder {
        val binding = ItemClosedOrderCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ClosedOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClosedOrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ClosedOrderViewHolder(
        private val binding: ItemClosedOrderCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CompletedOrderHistoryModel) {
            val displayTable = if (item.tableName.isNotBlank()) item.tableName else "Table"
            binding.tvTableNumber.text = displayTable

            val orderIdStr = if (item.orderId.startsWith("#")) item.orderId else "#${item.orderId}"
            val formattedDate = item.timestamp?.toDate()?.let { dateTimeFormatter.format(it) } ?: ""
            val idAndTime = if (formattedDate.isNotEmpty()) "$orderIdStr • $formattedDate" else orderIdStr
            binding.tvOrderIdAndTime.text = idAndTime

            val payMethod = if (item.paymentMethod.isNotBlank()) item.paymentMethod.uppercase() else "CASH"
            binding.tvPaymentStatusChip.text = "PAID • $payMethod"

            val itemsText = if (item.itemsSummary.isNotBlank()) item.itemsSummary else "${item.totalItems} Items"
            binding.tvItemsSummary.text = itemsText

            val formattedPrice = try {
                val formatted = currencyFormatter.format(item.grandTotal)
                if (!formatted.startsWith("₹")) "₹ ${item.grandTotal}" else formatted.replace("₹", "₹ ")
            } catch (e: Exception) {
                "₹ ${item.grandTotal}"
            }
            binding.tvGrandTotal.text = formattedPrice

            val waiter = if (item.waiterName.isNotBlank()) item.waiterName else "Staff"
            binding.tvWaiterName.text = "Waiter: $waiter"

            val cashier = if (item.cashierName.isNotBlank()) item.cashierName else "Cashier"
            binding.tvCashierName.text = "Cashier: $cashier"

            binding.cvClosedOrderRoot.setOnClickListener {
                onOrderClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CompletedOrderHistoryModel>() {
        override fun areItemsTheSame(
            oldItem: CompletedOrderHistoryModel,
            newItem: CompletedOrderHistoryModel
        ): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(
            oldItem: CompletedOrderHistoryModel,
            newItem: CompletedOrderHistoryModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
