package com.example.masterdashboard.notifications.alert

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemAlertCardBinding

/**
 * Adapter for rendering the notification feed with dynamic status-based styling.
 */
class NotificationAdapter(
    private val onCardClicked: (AppNotificationModel) -> Unit,
    private val onAcceptClicked: (AppNotificationModel) -> Unit,
    private val onDoneClicked: (AppNotificationModel) -> Unit
) : ListAdapter<AppNotificationModel, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemAlertCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemAlertCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppNotificationModel) {
            val context = binding.root.context

            binding.tvAlertTitle.text = item.title
            binding.tvAlertMessage.text = item.message
            binding.tvAlertTime.text = item.timeStamp

            // Unread State UI Styling
            if (item.isRead) {
                binding.viewUnreadDot.visibility = View.GONE
                binding.cvAlertRoot.strokeWidth = 0
                binding.tvAlertTitle.alpha = 0.7f
                binding.tvAlertMessage.alpha = 0.7f
            } else {
                binding.viewUnreadDot.visibility = View.VISIBLE
                binding.cvAlertRoot.strokeWidth = 2
                binding.cvAlertRoot.strokeColor = ContextCompat.getColor(context, R.color.accent_blue)
                binding.tvAlertTitle.alpha = 1.0f
                binding.tvAlertMessage.alpha = 1.0f
            }

            // Role-specific Icon & Color mapping
            when {
                item.title.contains("Inventory", true) -> {
                    binding.ivAlertIcon.setImageResource(R.drawable.ic_inventory_24dp)
                    binding.ivAlertIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.accent_orange)
                }
                item.message.contains("Bill", true) -> {
                    binding.ivAlertIcon.setImageResource(R.drawable.biling)
                    binding.ivAlertIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.status_billing)
                }
                item.message.contains("Ready", true) || item.message.contains("Confirmed", true) -> {
                    binding.ivAlertIcon.setImageResource(R.drawable.ic_add_circle_24dp)
                    binding.ivAlertIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.status_free)
                }
                else -> {
                    binding.ivAlertIcon.setImageResource(R.drawable.ic_notifications_24dp)
                    binding.ivAlertIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.status_occupied)
                }
            }

            // Handle Actionable Request UI Expansion
            if (item.isExpanded && item.type == NotificationType.ACTIONABLE_REQUEST && item.status != RequestStatus.DONE) {
                binding.llActionContainer.visibility = View.VISIBLE
                binding.btnAcceptRequest.visibility = if (item.status == RequestStatus.ACCEPTED) View.GONE else View.VISIBLE
            } else {
                binding.llActionContainer.visibility = View.GONE
            }

            binding.cvAlertRoot.setOnClickListener { onCardClicked(item) }
            binding.btnAcceptRequest.setOnClickListener { onAcceptClicked(item) }
            binding.btnDoneRequest.setOnClickListener { onDoneClicked(item) }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<AppNotificationModel>() {
        override fun areItemsTheSame(oldItem: AppNotificationModel, newItem: AppNotificationModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AppNotificationModel, newItem: AppNotificationModel) = oldItem == newItem
    }
}
