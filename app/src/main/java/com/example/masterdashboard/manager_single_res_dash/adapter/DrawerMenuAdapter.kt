package com.example.masterdashboard.manager_single_res_dash.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemDrawerMenuBinding
import com.example.masterdashboard.manager_single_res_dash.models.DrawerMenuItem

class DrawerMenuAdapter(
    private val menuItems: List<DrawerMenuItem>,
    private val onItemClick: (DrawerMenuItem) -> Unit
) : RecyclerView.Adapter<DrawerMenuAdapter.MenuViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemDrawerMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(menuItems[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = menuItems.size

    inner class MenuViewHolder(private val binding: ItemDrawerMenuBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DrawerMenuItem, isSelected: Boolean) {
            binding.txtMenuTitle.text = item.title
            binding.imgMenuIcon.setImageResource(item.iconRes)

            val context = binding.root.context

            // Setup Badge Notifications count values dynamically
            if (item.badgeCount > 0) {
                binding.badgeNotification.visibility = View.VISIBLE
                binding.badgeNotification.text = item.badgeCount.toString()
            } else {
                binding.badgeNotification.visibility = View.GONE
            }

            // Apply specific UI styles matching layout design guidelines
            when {
                item.isLogout -> {
                    // Constant Red design layout presentation parameters for Logout
                    binding.menuRowContainer.setCardBackgroundColor(Color.TRANSPARENT)
                    binding.imgMenuIcon.setColorFilter(context.getColor(R.color.red_alert))
                    binding.txtMenuTitle.setTextColor(context.getColor(R.color.red_alert))
                }
                isSelected -> {
                    // Purple Selection highlight theme matching mockups
                    binding.menuRowContainer.setCardBackgroundColor(Color.parseColor("#2A1B4E"))
                    binding.imgMenuIcon.setColorFilter(context.getColor(R.color.accent_purple))
                    binding.txtMenuTitle.setTextColor(context.getColor(R.color.accent_purple))
                }
                else -> {
                    // Standard Idle view choices state parameters configuration options
                    binding.menuRowContainer.setCardBackgroundColor(Color.TRANSPARENT)
                    binding.imgMenuIcon.setColorFilter(context.getColor(R.color.text_secondary))
                    binding.txtMenuTitle.setTextColor(context.getColor(R.color.text_primary))
                }
            }

            binding.root.setOnClickListener {
                if (!item.isLogout) {
                    val previousSelection = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(previousSelection)
                    notifyItemChanged(selectedPosition)
                }
                onItemClick(item)
            }
        }
    }
}

