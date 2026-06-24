package com.example.masterdashboard.staff_dash.waiter_screens.table.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemCardClipsBinding
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData

class FloorChipsAdapter(
    private val onChipClicked: (TableFilterData) -> Unit
) : ListAdapter<TableFilterData, FloorChipsAdapter.ChipViewHolder>(FloorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val binding = ItemCardClipsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val chip = getItem(position)
        holder.bind(chip, onChipClicked)
    }

    class ChipViewHolder(val binding: ItemCardClipsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chip: TableFilterData, onChipClicked: (TableFilterData) -> Unit) {
            binding.tvFloorChip.text = chip.name
            val context = itemView.context

            if (chip.isSelected) {
                binding.tvFloorChip.setBackgroundResource(R.drawable.bg_chip_unselected)
                binding.tvFloorChip.background.setTint(ContextCompat.getColor(context, R.color.chip_selected))
                binding.tvFloorChip.setTextColor(Color.WHITE)
            } else {
                binding.tvFloorChip.setBackgroundResource(R.drawable.bg_chip_unselected)
                binding.tvFloorChip.background.setTintList(null)
                binding.tvFloorChip.setTextColor(ContextCompat.getColor(context, R.color.chip_unselected_text))
            }

            itemView.setOnClickListener {
                onChipClicked(chip)
            }
        }
    }

    class FloorDiffCallback : DiffUtil.ItemCallback<TableFilterData>() {
        override fun areItemsTheSame(oldItem: TableFilterData, newItem: TableFilterData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TableFilterData, newItem: TableFilterData): Boolean {
            return oldItem.isSelected == newItem.isSelected && oldItem.name == newItem.name
        }
    }
}
