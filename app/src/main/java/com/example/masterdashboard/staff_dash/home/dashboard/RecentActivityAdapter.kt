package com.example.masterdashboard.staff_dash.home.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemRecentAcitvityCardBinding

class RecentActivityAdapter : ListAdapter<RecentActivityItem, RecentActivityAdapter.ActivityViewHolder>(ActivityDiffCallback()) {

    // Enforces the 5 items limit directly at the RecyclerView UI layout level
    override fun getItemCount(): Int {
        val actualSize = super.getItemCount()
        return if (actualSize > 5) 5 else actualSize
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemRecentAcitvityCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActivityViewHolder(private val binding: ItemRecentAcitvityCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RecentActivityItem) {
            binding.tvAlertTitle.text = "Table ${item.tableId}"
            binding.tvAlertMessage.text = item.description
            binding.tvAlertTime.text = item.timeAgo

            // Dynamically assign vector drawables based on log type context profiles
            when (item.alertType) {
                "BILL" -> binding.ivAlertIcon.setImageResource(R.drawable.biling)
                "CALL" -> binding.ivAlertIcon.setImageResource(R.drawable.ic_notifications_24dp)
                else -> binding.ivAlertIcon.setImageResource(R.drawable.ic_add_circle_24dp)
            }
        }
    }

    class ActivityDiffCallback : DiffUtil.ItemCallback<RecentActivityItem>() {
        override fun areItemsTheSame(oldItem: RecentActivityItem, newItem: RecentActivityItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RecentActivityItem, newItem: RecentActivityItem): Boolean = oldItem == newItem
    }
}