package com.example.masterdashboard.manager_single_res_dash.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemCustomerCardBinding
import com.example.masterdashboard.manager_single_res_dash.models.CustomerModel
import java.text.SimpleDateFormat
import java.util.*

class CustomerAdapter(
    private val onCustomerClick: (CustomerModel) -> Unit
) : ListAdapter<CustomerModel, CustomerAdapter.CustomerViewHolder>(CustomerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(getItem(position), onCustomerClick)
    }

    class CustomerViewHolder(private val binding: ItemCustomerCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(customer: CustomerModel, onCustomerClick: (CustomerModel) -> Unit) {
            binding.tvCustomerName.text = customer.customerName.ifEmpty { "Guest Customer" }
            binding.tvCustomerPhone.text = customer.customerMobile
            binding.tvVisitCount.text = "${customer.visitCount} Visits"
            binding.tvTotalSpent.text = "₹ ${customer.totalSpent.toInt()}"
            
            val lastVisitDate = customer.lastVisit?.toDate()
            if (lastVisitDate != null) {
                val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                binding.tvLastVisit.text = sdf.format(lastVisitDate)
            } else {
                binding.tvLastVisit.text = "Never"
            }

            binding.root.setOnClickListener { onCustomerClick(customer) }
        }
    }

    class CustomerDiffCallback : DiffUtil.ItemCallback<CustomerModel>() {
        override fun areItemsTheSame(oldItem: CustomerModel, newItem: CustomerModel): Boolean {
            return oldItem.customerId == newItem.customerId
        }

        override fun areContentsTheSame(oldItem: CustomerModel, newItem: CustomerModel): Boolean {
            return oldItem == newItem
        }
    }
}