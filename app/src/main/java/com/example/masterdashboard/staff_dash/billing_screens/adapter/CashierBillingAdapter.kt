package com.example.masterdashboard.staff_dash.billing_screens.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemCashierOrderCardBinding
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.example.masterdashboard.staff_dash.utils.StatusUIUtils

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
            val context = itemView.context
            binding.tvTableNumber.text = item.tableName
            binding.tvOrderId.text = "• ${item.orderId}"
            binding.tvGrandTotal.text = "₹${String.format("%.2f", item.grandTotal)}"
            
            val status = item.orderStatus.uppercase()
            val type = item.orderType.uppercase()
            val isCounterOrder = type == "TAKE_AWAY" || type == "DELIVERY"
            
            // Apply Status UI using centralized utility (Mirroring ActiveOrdersAdapter pattern)
            StatusUIUtils.applyCashierStatusUI(context, binding.tvBillingStatus, status, isCounterOrder)

            // Button logic only
            when {
                status == "COMPLETED" -> {
                    binding.btnGenerateBill.visibility = android.view.View.GONE
                }
                isCounterOrder && status == "PAID" -> {
                    binding.btnGenerateBill.visibility = android.view.View.VISIBLE
                    binding.btnGenerateBill.text = "Confirm Hand Over"
                    binding.btnGenerateBill.setOnClickListener { onConfirmHandoverClicked(item) }
                }
                isCounterOrder && (status == "PENDING" || status == "PREPARING" || status == "READY") -> {
                    binding.btnGenerateBill.visibility = android.view.View.VISIBLE
                    binding.btnGenerateBill.text = "Collect Payment"
                    binding.btnGenerateBill.setOnClickListener { onGenerateBillClicked(item) }
                }
                status == "PAID" -> {
                    binding.btnGenerateBill.visibility = android.view.View.GONE
                }
                status == "BILLING" -> {
                    binding.btnGenerateBill.visibility = android.view.View.VISIBLE
                    binding.btnGenerateBill.text = "Collect Payment"
                    binding.btnGenerateBill.setOnClickListener { onGenerateBillClicked(item) }
                }
                status == "SERVED" -> {
                    binding.btnGenerateBill.visibility = android.view.View.VISIBLE
                    binding.btnGenerateBill.text = "Generate Bill"
                    binding.btnGenerateBill.setOnClickListener { onGenerateBillClicked(item) }
                }
                else -> {
                    binding.btnGenerateBill.visibility = android.view.View.VISIBLE
                    binding.btnGenerateBill.text = "Generate Bill"
                    binding.btnGenerateBill.setOnClickListener { onGenerateBillClicked(item) }
                }
            }

            // Click on entire card to navigate to settlement / view details / reprint
            binding.root.setOnClickListener {
                onGenerateBillClicked(item)
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