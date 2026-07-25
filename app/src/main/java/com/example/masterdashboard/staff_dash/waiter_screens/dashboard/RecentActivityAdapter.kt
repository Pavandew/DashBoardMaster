package com.example.masterdashboard.staff_dash.waiter_screens.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemRecentAcitvityCardBinding

class RecentActivityAdapter : ListAdapter<RecentActivityItem, RecentActivityAdapter.ActivityViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemRecentAcitvityCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ActivityViewHolder(private val binding: ItemRecentAcitvityCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecentActivityItem) {
            // Map parameters directly to your XML layout View IDs
            binding.tvAlertTitle.text = item.title
            binding.tvAlertMessage.text = item.subtitle
            binding.tvAlertTime.text = item.timestampText

            // Set contextual icon based on notification/event type
            when (item.type.uppercase()) {
                "BILL" -> binding.ivAlertIcon.setImageResource(R.drawable.ic_restaurant_24dp)
                "CALL" -> binding.ivAlertIcon.setImageResource(R.drawable.ic_notifications_24dp)
                else -> binding.ivAlertIcon.setImageResource(R.drawable.bg_status_served) // "ORDER"
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RecentActivityItem>() {
        override fun areItemsTheSame(oldItem: RecentActivityItem, newItem: RecentActivityItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RecentActivityItem, newItem: RecentActivityItem): Boolean =
            oldItem == newItem
    }
}