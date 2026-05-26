package com.example.masterdashboard.staff_dash.home.alert

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemAlertCardBinding
class AlertsAdapter(
    private val onCardClicked: (StaffAlertItem) -> Unit,
    private val onAcceptClicked: (StaffAlertItem) -> Unit,
    private val onDoneClicked: (StaffAlertItem) -> Unit
) : ListAdapter<StaffAlertItem, AlertsAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlertViewHolder(private val binding: ItemAlertCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StaffAlertItem) {
            val context = binding.root.context

            // Bind Basic Data Elements
            binding.tvAlertTitle.text = item.title
            binding.tvAlertMessage.text = item.message
            binding.tvAlertTime.text = item.timeStamp

            // Dynamic Unread Dot State Indicator Toggle
            binding.viewUnreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE

            // Assign Dynamic Icons and Tint Profiles depending on content string types
            when {
                item.message.contains("Bill", ignoreCase = true) -> {
                    binding.ivAlertIcon.setImageResource(R.drawable.biling) // Add your resource vectors here
                    binding.ivAlertIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.status_billing)
                }
                item.message.contains("Ready", ignoreCase = true) || item.message.contains("Confirmed", ignoreCase = true) -> {
                    binding.ivAlertIcon.setImageResource(R.drawable.ic_add_circle_24dp)
                    binding.ivAlertIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.status_free)
                }
                else -> {
                    binding.ivAlertIcon.setImageResource(R.drawable.ic_notifications_24dp)
                    binding.ivAlertIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.status_occupied)
                }
            }

            // Handle Dynamic Expandable States Programmatically
            if (item.isExpanded && item.type == NotificationType.ACTIONABLE_REQUEST && item.status != RequestStatus.DONE) {
                binding.llActionContainer.visibility = View.VISIBLE

                // Fine-tune actions dynamically depending on whether it's already Accepted
                if (item.status == RequestStatus.ACCEPTED) {
                    binding.btnAcceptRequest.visibility = View.GONE
                } else {
                    binding.btnAcceptRequest.visibility = View.VISIBLE
                }
            } else {
                binding.llActionContainer.visibility = View.GONE
            }

            // Root Click Interception
            binding.cvAlertRoot.setOnClickListener {
                onCardClicked(item)
            }

            // Event Listeners for Terminal Action Triggers
            binding.btnAcceptRequest.setOnClickListener { onAcceptClicked(item) }
            binding.btnDoneRequest.setOnClickListener { onDoneClicked(item) }
        }
    }

    class AlertDiffCallback : DiffUtil.ItemCallback<StaffAlertItem>() {
        override fun areItemsTheSame(oldItem: StaffAlertItem, newItem: StaffAlertItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: StaffAlertItem, newItem: StaffAlertItem): Boolean = oldItem == newItem
    }
}