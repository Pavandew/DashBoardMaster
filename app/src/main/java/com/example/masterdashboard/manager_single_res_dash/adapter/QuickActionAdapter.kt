package com.example.masterdashboard.manager_single_res_dash.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemQuickActionBinding
import com.example.masterdashboard.manager_single_res_dash.models.QuickActionModel

class QuickActionAdapter(
    private val actions: List<QuickActionModel>,
    private val onActionClick: (ManagerDashboardAdapter.QuickActionType) -> Unit
) : RecyclerView.Adapter<QuickActionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuickActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(actions[position])
    }

    override fun getItemCount(): Int = actions.size

    inner class ViewHolder(private val binding: ItemQuickActionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(action: QuickActionModel) {
            binding.quickActionTitle.text = action.title
            binding.quickActionIcon.setImageResource(action.iconRes)
            binding.quickActionIcon.setColorFilter(action.iconColor)
            binding.cardQuickAction.strokeColor = action.strokeColor
            binding.cardQuickAction.setCardBackgroundColor(action.bgColor)
            
            binding.root.setOnClickListener {
                onActionClick(action.type)
            }
        }
    }
}
