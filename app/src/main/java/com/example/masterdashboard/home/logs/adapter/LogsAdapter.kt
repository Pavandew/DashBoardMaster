package com.example.masterdashboard.home.logs.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemRestaurantRowBinding
import com.example.masterdashboard.home.logs.models.LogData

class LogsAdapter(
    private var logsList: List<LogData>,
    private val listener: OnLogClickListener
) : RecyclerView.Adapter<LogsAdapter.LogsViewHolder>() {

    interface OnLogClickListener {
        fun onViewClick(log: LogData)
        fun onEditClick(log: LogData)
        fun onDeleteClick(log: LogData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogsViewHolder {
        val binding = ItemRestaurantRowBinding.inflate(
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
        private val binding: ItemRestaurantRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LogData) {

            binding.tvId.text = item.id.toString()
            binding.tvRestaurantName.text = item.title
            binding.tvUsername.text = item.userName
            binding.tvOwner.text = item.ownerName
            binding.tvCreated.text = item.date

            if (item.status.equals("Active", ignoreCase = true)) {
                binding.tvStatus.text = "Active"
                binding.tvStatus.setTextColor(Color.parseColor("#16A34A"))
                binding.tvStatus.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                binding.tvStatus.setBackgroundResource(R.drawable.bg_card_2)

            } else {
                binding.tvStatus.text = "Inactive"
                binding.tvStatus.setTextColor(Color.parseColor("#DC2626"))
                binding.tvStatus.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                binding.tvStatus.setBackgroundResource(R.drawable.bg_card)
            }

            binding.resBtnEye.setOnClickListener {
                listener.onViewClick(item)
            }

            binding.resBtnEdit.setOnClickListener {
                listener.onEditClick(item)
            }

            binding.resBtnDelete.setOnClickListener {
                listener.onDeleteClick(item)
            }
        }
    }
}