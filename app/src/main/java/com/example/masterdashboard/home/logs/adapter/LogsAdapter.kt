package com.example.masterdashboard.home.logs.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemLogActivityBinding
import com.example.masterdashboard.databinding.ItemRestaurantRowBinding
import com.example.masterdashboard.home.logs.models.LogData

class LogsAdapter(
    private var logsList: List<LogData>,
    private val listener: OnLogClickListener
) : RecyclerView.Adapter<LogsAdapter.LogsViewHolder>() {

    interface OnLogClickListener {
        fun onViewClick(log: LogData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogsViewHolder {
        val binding = ItemLogActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogsViewHolder, position: Int) {
        val item = logsList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = logsList.size

    fun updateList(newList: List<LogData>) {
        logsList = newList
        notifyDataSetChanged()
    }

    inner class LogsViewHolder(
        private val binding: ItemLogActivityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LogData) {

            binding.tvId.text = item.id.toString()
            binding.tvRestaurantName.text = item.title
            binding.tvUsername.text = item.userName
            binding.tvOwner.text = item.ownerName
            binding.tvCreated.text = item.date

            if (item.status.equals("Active", ignoreCase = true)) {
                binding.tvStatus.text = "Active"
                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.green)
                )

                binding.tvStatus.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(binding.root.context, R.color.light_green)
                    )
                binding.tvStatus.setBackgroundResource(R.drawable.bg_card_2)

            } else {
                binding.tvStatus.text = "Inactive"

                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.red)
                )

                binding.tvStatus.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(binding.root.context, R.color.light_red)
                    )

                binding.tvStatus.setBackgroundResource(R.drawable.bg_card)
            }
            binding.resBtnEye.setOnClickListener {
                listener.onViewClick(item)
            }
        }
    }
}