package com.example.masterdashboard.staff_dash.billing_screens.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemCashierOrderCardBinding
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel

class CashierBillingAdapter(
    private val onGenerateBillClicked: (CashierBillingOrderModel) -> Unit,
    private val onConfirmHandoverClicked: (CashierBillingOrderModel) -> Unit
) : ListAdapter<CashierBillingOrderModel, CashierBillingAdapter.BillingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillingViewHolder {
        val binding = ItemCashierOrderCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BillingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillingViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onGenerateBillClicked, onConfirmHandoverClicked)
    }

    class BillingViewHolder(private val binding: ItemCashierOrderCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: CashierBillingOrderModel,
            onGenerateBillClicked: (CashierBillingOrderModel) -> Unit,
            onConfirmHandoverClicked: (CashierBillingOrderModel) -> Unit
        ) {
            binding.tvTableNumber.text = item.tableName
            binding.tvOrderId.text = "• ${item.orderId}"
            binding.tvGrandTotal.text = "₹${String.format("%.2f", item.grandTotal)}"
            
            val status = item.orderStatus.uppercase()
            val type = item.orderType.uppercase()
            val isCounterOrder = type == "TAKE_AWAY" || type == "DELIVERY"
            
            when {
                status == "COMPLETED" -> {
                    binding.tvBillingStatus.text = "HANDED OVER"
                    binding.tvBillingStatus.setBackgroundResource(R.drawable.bg_status_green)
                    binding.tvBillingStatus.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, R.color.dark_green))
                    binding.btnGenerateBill.visibility = android.view.View.GONE
                }
                status == "PAID" && isCounterOrder -> {
                    // For Takeaway/Delivery that is PAID but not yet handed over
                    binding.tvBillingStatus.text = "PAID - PENDING PICKUP"
                    binding.tvBillingStatus.setBackgroundResource(R.drawable.bg_status_ready)
                    binding.tvBillingStatus.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, R.color.dark_green))
                    
                    binding.btnGenerateBill.visibility = android.view.View.VISIBLE
                    binding.btnGenerateBill.text = "Confirm Hand Over"
                    binding.btnGenerateBill.setOnClickListener { onConfirmHandoverClicked(item) }
                }
                status == "PAID" -> {
                    // Table order that is PAID
                    binding.tvBillingStatus.text = "PAID"
                    binding.tvBillingStatus.setBackgroundResource(R.drawable.bg_status_green)
                    binding.tvBillingStatus.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, R.color.dark_green))
                    binding.btnGenerateBill.visibility = android.view.View.GONE
                }
                status == "BILLING" -> {
                    binding.tvBillingStatus.text = "READY FOR BILL"
                    binding.tvBillingStatus.setBackgroundResource(R.drawable.bg_status_amber)
                    binding.tvBillingStatus.setTextColor(android.graphics.Color.parseColor("#C2410C"))
                    binding.btnGenerateBill.visibility = android.view.View.VISIBLE
                    binding.btnGenerateBill.text = "Collect Payment"
                    binding.btnGenerateBill.setOnClickListener { onGenerateBillClicked(item) }
                }
                else -> {
                    // Standard "SERVED" Table Order or unpaid counter order
                    binding.tvBillingStatus.text = if (isCounterOrder) status.replace("_", " ") else "RUNNING BILL"
                    binding.tvBillingStatus.setBackgroundResource(R.drawable.bg_status_blue)
                    binding.tvBillingStatus.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, R.color.primary_blue))
                    binding.btnGenerateBill.visibility = android.view.View.VISIBLE
                    binding.btnGenerateBill.text = "Generate Bill"
                    binding.btnGenerateBill.setOnClickListener { onGenerateBillClicked(item) }
                }
            }

            // Click on entire card to navigate to settlement (if not completed)
            binding.root.setOnClickListener {
                if (status != "COMPLETED") {
                    onGenerateBillClicked(item)
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<CashierBillingOrderModel>() {
        override fun areItemsTheSame(
            oldItem: CashierBillingOrderModel,
            newItem: CashierBillingOrderModel
        ): Boolean = oldItem.orderId == newItem.orderId

        override fun areContentsTheSame(
            oldItem: CashierBillingOrderModel,
            newItem: CashierBillingOrderModel
        ): Boolean = oldItem == newItem
    }
}