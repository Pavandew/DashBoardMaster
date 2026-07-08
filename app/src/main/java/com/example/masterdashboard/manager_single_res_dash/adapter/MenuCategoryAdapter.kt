package com.example.masterdashboard.manager_single_res_dash.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemMenuCategoryBinding
import com.example.masterdashboard.manager_single_res_dash.models.MenuCategory

class MenuCategoryAdapter(
    private val onItemClick: (MenuCategory) -> Unit,
    private val onItemLongClick: (MenuCategory) -> Unit,
) : ListAdapter<MenuCategory, MenuCategoryAdapter.MenuViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick, onItemLongClick)
    }

    class MenuViewHolder(
        private val binding: ItemMenuCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            category: MenuCategory,
            onItemClick: (MenuCategory) -> Unit,
            onItemLongClick: (MenuCategory) -> Unit
        ) {
            val context = itemView.context

            binding.tvCategoryName.text = category.name
            binding.tvItemCount.text = String.format("%02d items", category.itemCount)

            // Dynamic Reflection Identifier Lookup: Converts String keys back to real Drawables safely
            val resourceId = context.resources.getIdentifier(
                category.imageResId,
                "drawable",
                context.packageName
            )

            // Fallback gracefully to app_logo if the database string is misconfigured or asset is missing
            if (resourceId != 0) {
                binding.ivCategoryIcon.setImageResource(resourceId)
            } else {
                binding.ivCategoryIcon.setImageResource(R.drawable.app_logo)
            }

            binding.root.setOnClickListener { onItemClick(category) }

            // long tap trigger for deletion
            binding.root.setOnLongClickListener {
                onItemLongClick(category)
                true // Returns true to consume the click event so it doesn't trigger standard setOnClickListener
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MenuCategory>() {
        override fun areItemsTheSame(oldItem: MenuCategory, newItem: MenuCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuCategory, newItem: MenuCategory): Boolean {
            return oldItem == newItem
        }
    }
}