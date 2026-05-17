package com.example.masterdashboard.home.logs.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemLogRowBinding
import com.example.masterdashboard.home.logs.models.LogData

class LogsAdapter(
    private var logsList: List<LogData>,
    private val listener: OnLogClickListener
) : RecyclerView.Adapter<LogsAdapter.LogsViewHolder>() {

    interface OnLogClickListener {
        fun onViewClick(log: LogData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogsViewHolder {
        val binding = ItemLogRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogsViewHolder, position: Int) {
        holder.bind(logsList[position])
    }

    override fun getItemCount(): Int = logsList.size

    fun updateList(newList: List<LogData>) {
        logsList = newList
        notifyDataSetChanged()
    }

    inner class LogsViewHolder(
        private val binding: ItemLogRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LogData) {
            val context = binding.root.context

            // 1. Basic Data Binding
//            binding.tvId.text = "#${item.id}"
            binding.tvAction.text = item.title
            binding.tvUser.text = "by ${item.userName}"
            binding.tvTargetRestaurant.text = "Restaurant: ${item.ownerName}"
            binding.tvTimestamp.text = item.date

            // 2. Action Type Styling (Professional status indicator)
            // Assuming your title or a status field contains the action type
            when {
                item.title.contains("Added", ignoreCase = true) ||
                        item.title.contains("Created", ignoreCase = true) -> {
                    binding.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
                }
                item.title.contains("Deleted", ignoreCase = true) ||
                        item.title.contains("Removed", ignoreCase = true) -> {
                    binding.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.red))
                }
                else -> {
                    // Default for Updates/Edits
                    binding.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.light_purple))
                }
            }

            // 3. Eye Button Click (View Details)
            binding.root.setOnClickListener {
                listener.onViewClick(item)
            }

            // If you kept the specific eye button in the new card layout:
            /*
            binding.resBtnEye.setOnClickListener {
                listener.onViewClick(item)
            }
            */
        }
    }
}