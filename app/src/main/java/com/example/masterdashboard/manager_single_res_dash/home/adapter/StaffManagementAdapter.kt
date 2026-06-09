package com.example.masterdashboard.manager_single_res_dash.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemStaffCardsBinding
import com.example.masterdashboard.manager_single_res_dash.home.models.StaffDataModel

class StaffManagementAdapter(
    private var staffList: List<StaffDataModel>,
    private val onCardLongClick: (StaffDataModel) -> Unit
) : RecyclerView.Adapter<StaffManagementAdapter.StaffViewHolder>() {

    // Pass the binding instance to the ViewHolder
    class StaffViewHolder(val binding: ItemStaffCardsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        // Inflate using the generated Binding class
        val binding = ItemStaffCardsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StaffViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        val staff = staffList[position]
        val context = holder.itemView.context

        // Direct, type-safe access to your layout views through the binding object
        holder.binding.apply {
            tvStaffName.text = staff.staffName
            tvStaffRole.text = staff.role
            tvStatusBadge.text = staff.status
            ivStaffAvatar.setImageResource(R.drawable.person)

            // Dynamic status styling
            if (staff.status.equals("Active", ignoreCase = true)) {
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active)
            } else {
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_light))
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_inactive)
            }
            
            staffCard.setOnLongClickListener {
                onCardLongClick(staff)
                true
            }
            
        }
    }

    override fun getItemCount(): Int = staffList.size

    fun updateData(newStaffList: List<StaffDataModel>) {
        this.staffList = newStaffList
        notifyDataSetChanged()
    }
}