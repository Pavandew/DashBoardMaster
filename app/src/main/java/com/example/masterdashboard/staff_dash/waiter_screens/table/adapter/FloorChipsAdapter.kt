package com.example.masterdashboard.staff_dash.waiter_screens.table.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemCardChipsBinding
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData

class FloorChipsAdapter(
    private val onChipClicked: (TableFilterData) -> Unit
) : ListAdapter<TableFilterData, FloorChipsAdapter.ChipViewHolder>(FloorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val binding = ItemCardChipsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val chip = getItem(position)
        Log.d("Order_Flow_Debug", "🔗 [CHIP ADAPTER] Binding Chip: ${chip.name} (Selected: ${chip.isSelected})")
        holder.bind(chip, onChipClicked)
    }

    class ChipViewHolder(val binding: ItemCardChipsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chip: TableFilterData, onChipClicked: (TableFilterData) -> Unit) {
            // 1. Set chip text label
            binding.tvFloorChip.text = chip.name

            // 🔄 2. Set view selection state directly.
            // This triggers res/drawable/bg_chip_selector and res/color/color_chip_text_selector automatically!
            binding.tvFloorChip.isSelected = chip.isSelected
            itemView.isSelected = chip.isSelected

            // 3. Register click callback
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
            return oldItem == newItem
        }
    }
}