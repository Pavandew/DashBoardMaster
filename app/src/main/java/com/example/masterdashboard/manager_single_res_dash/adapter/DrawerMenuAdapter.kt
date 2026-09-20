package com.example.masterdashboard.manager_single_res_dash.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemDrawerChildMenuBinding
import com.example.masterdashboard.databinding.ItemDrawerHeaderBinding
import com.example.masterdashboard.databinding.ItemDrawerMenuBinding
import com.example.masterdashboard.manager_single_res_dash.models.DrawerMenuItem

class DrawerMenuAdapter(
    private val rootMenuItems: List<DrawerMenuItem>,
    private val onItemClick: (DrawerMenuItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SINGLE = 0
        private const val TYPE_HEADER = 1
        private const val TYPE_CHILD = 2

        private val COLOR_SELECTED_BG = Color.parseColor("#1E293B")
        private val COLOR_SELECTED_ACCENT = Color.parseColor("#38BDF8")
        private val COLOR_UNSELECTED_TEXT = Color.parseColor("#FFFFFF")
        private val COLOR_UNSELECTED_ICON = Color.parseColor("#8E92A2")
    }

    private var selectedItemId = 0
    private val expandedHeaderIds = mutableSetOf<Int>()
    private var displayList: List<DrawerMenuItem> = emptyList()

    init {
        rootMenuItems.filter { it.isHeader && it.isExpanded }.forEach { expandedHeaderIds.add(it.id) }
        rebuildDisplayList(rootMenuItems)
    }

    fun setSelectedItem(id: Int) {
        selectedItemId = id
        notifyDataSetChanged()
    }

    fun updateMenuItems(newItems: List<DrawerMenuItem>) {
        newItems.filter { it.isHeader && it.isExpanded }.forEach { expandedHeaderIds.add(it.id) }
        rebuildDisplayList(newItems)
        notifyDataSetChanged()
    }

    private fun rebuildDisplayList(items: List<DrawerMenuItem> = rootMenuItems) {
        val list = mutableListOf<DrawerMenuItem>()
        val targetList = if (items.isNotEmpty()) items else rootMenuItems
        for (item in targetList) {
            if (item.isHeader && item.children.isNotEmpty()) {
                val headerCopy = item.copy(isExpanded = expandedHeaderIds.contains(item.id))
                list.add(headerCopy)
                if (expandedHeaderIds.contains(item.id)) {
                    list.addAll(item.children)
                }
            } else {
                list.add(item)
            }
        }
        displayList = list
    }

    override fun getItemViewType(position: Int): Int {
        val item = displayList[position]
        return when {
            item.isHeader && item.children.isNotEmpty() -> TYPE_HEADER
            item.parentHeaderId != null -> TYPE_CHILD
            else -> TYPE_SINGLE
        }
    }

    override fun getItemCount(): Int = displayList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(ItemDrawerHeaderBinding.inflate(inflater, parent, false))
            TYPE_CHILD -> ChildViewHolder(ItemDrawerChildMenuBinding.inflate(inflater, parent, false))
            else -> SingleViewHolder(ItemDrawerMenuBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayList[position]
        val isSelected = item.id == selectedItemId
        when (holder) {
            is HeaderViewHolder -> holder.bind(item)
            is ChildViewHolder -> holder.bind(item, isSelected)
            is SingleViewHolder -> holder.bind(item, isSelected)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemDrawerHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DrawerMenuItem) {
            binding.txtHeaderTitle.text = item.title
            binding.imgHeaderIcon.setImageResource(item.iconRes)

            val isExpanded = expandedHeaderIds.contains(item.id)
            binding.imgExpandArrow.rotation = if (isExpanded) 180f else 0f

            binding.root.setOnClickListener {
                if (expandedHeaderIds.contains(item.id)) {
                    expandedHeaderIds.remove(item.id)
                } else {
                    expandedHeaderIds.add(item.id)
                }
                rebuildDisplayList()
                notifyDataSetChanged()
            }
        }
    }

    inner class ChildViewHolder(private val binding: ItemDrawerChildMenuBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DrawerMenuItem, isSelected: Boolean) {
            binding.txtChildMenuTitle.text = item.title
            binding.imgChildMenuIcon.setImageResource(item.iconRes)

            if (item.badgeCount > 0) {
                binding.badgeNotification.visibility = View.VISIBLE
                binding.badgeNotification.text = item.badgeCount.toString()
            } else {
                binding.badgeNotification.visibility = View.GONE
            }

            if (isSelected) {
                binding.childMenuRowContainer.setCardBackgroundColor(COLOR_SELECTED_BG)
                binding.imgChildMenuIcon.setColorFilter(COLOR_SELECTED_ACCENT)
                binding.txtChildMenuTitle.setTextColor(COLOR_SELECTED_ACCENT)
            } else {
                binding.childMenuRowContainer.setCardBackgroundColor(Color.TRANSPARENT)
                binding.imgChildMenuIcon.setColorFilter(COLOR_UNSELECTED_ICON)
                binding.txtChildMenuTitle.setTextColor(COLOR_UNSELECTED_TEXT)
            }

            binding.root.setOnClickListener {
                selectedItemId = item.id
                notifyDataSetChanged()
                onItemClick(item)
            }
        }
    }

    inner class SingleViewHolder(private val binding: ItemDrawerMenuBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DrawerMenuItem, isSelected: Boolean) {
            binding.txtMenuTitle.text = item.title
            binding.imgMenuIcon.setImageResource(item.iconRes)

            val context = binding.root.context

            if (item.badgeCount > 0) {
                binding.badgeNotification.visibility = View.VISIBLE
                binding.badgeNotification.text = item.badgeCount.toString()
            } else {
                binding.badgeNotification.visibility = View.GONE
            }

            when {
                item.isLogout -> {
                    binding.menuRowContainer.setCardBackgroundColor(Color.TRANSPARENT)
                    binding.imgMenuIcon.setColorFilter(context.getColor(R.color.red_alert))
                    binding.txtMenuTitle.setTextColor(context.getColor(R.color.red_alert))
                }
                isSelected -> {
                    binding.menuRowContainer.setCardBackgroundColor(COLOR_SELECTED_BG)
                    binding.imgMenuIcon.setColorFilter(COLOR_SELECTED_ACCENT)
                    binding.txtMenuTitle.setTextColor(COLOR_SELECTED_ACCENT)
                }
                else -> {
                    binding.menuRowContainer.setCardBackgroundColor(Color.TRANSPARENT)
                    binding.imgMenuIcon.setColorFilter(COLOR_UNSELECTED_ICON)
                    binding.txtMenuTitle.setTextColor(COLOR_UNSELECTED_TEXT)
                }
            }

            binding.root.setOnClickListener {
                if (!item.isLogout) {
                    selectedItemId = item.id
                    notifyDataSetChanged()
                }
                onItemClick(item)
            }
        }
    }
}
