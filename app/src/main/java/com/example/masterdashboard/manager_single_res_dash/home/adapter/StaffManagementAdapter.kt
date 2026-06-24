package com.example.masterdashboard.manager_single_res_dash.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemStaffCardsBinding
import com.example.masterdashboard.manager_single_res_dash.home.models.StaffDataModel

class StaffManagementAdapter(
    private val onCardLongClick: (StaffDataModel) -> Unit,
    private val onCardClick: (StaffDataModel) -> Unit
) : ListAdapter<StaffDataModel, StaffManagementAdapter.StaffViewHolder>(StaffDiffCallback()) {

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
        val staff = getItem(position)
        val context = holder.itemView.context

        // Direct, type-safe access to your layout views through the binding object
        holder.binding.apply {
            tvStaffName.text = staff.staffId
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

            // FIX 2: Handle regular short-click to transition smoothly into the profile Detail View
            staffCard.setOnClickListener {
                onCardClick(staff)
            }

            // FIX 3: Separate long-click so it cleanly isolates your deletion dialog alert logic
            staffCard.setOnLongClickListener {
                onCardLongClick(staff)
                true // Consumes the touch window callback event cleanly
            }
            
        }
    }

    /**
     * Compatibility method to keep existing logic working while using ListAdapter
     */
    fun updateData(newList: List<StaffDataModel>) {
        submitList(newList)
    }
}

class StaffDiffCallback : DiffUtil.ItemCallback<StaffDataModel>() {
    override fun areItemsTheSame(oldItem: StaffDataModel, newItem: StaffDataModel): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: StaffDataModel, newItem: StaffDataModel): Boolean =
        oldItem == newItem
}
