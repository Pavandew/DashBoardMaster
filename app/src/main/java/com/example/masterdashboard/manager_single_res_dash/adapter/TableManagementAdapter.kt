package com.example.masterdashboard.manager_single_res_dash.table_management.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemFloorsBinding
import com.example.masterdashboard.manager_single_res_dash.models.FloorDataModel

/**
 * Handles rendering the top-level floors layout list.
 * Reuses type-safe ListAdapter check patterns seamlessly.
 */
class FloorListAdapter(
    private val onFloorClick: (FloorDataModel) -> Unit,
    private val onItemLongClick: (FloorDataModel) -> Unit
) : ListAdapter<FloorDataModel, FloorListAdapter.FloorViewHolder>(FloorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FloorViewHolder {
        val binding = ItemFloorsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FloorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FloorViewHolder, position: Int) {
        holder.bind(getItem(position), onItemLongClick)
    }

    inner class FloorViewHolder(
        private val binding: ItemFloorsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            floor: FloorDataModel,
            onLongClickAction: (FloorDataModel) -> Unit
        ) {
            binding.txtFloorName.text = floor.floorName
            // This displays exactly how many tables live on this floor (e.g., "4 Tables").
            binding.txtTableCount.text = if (floor.tableCount == 1) {
                "${floor.tableCount} Table"
            } else {
                "${floor.tableCount} Tables"
            }
            binding.root.setOnClickListener {
                onFloorClick(floor)
            }

            binding.root.setOnLongClickListener {
                onLongClickAction(floor)
                true
            }
        }
    }
}

class FloorDiffCallback : DiffUtil.ItemCallback<FloorDataModel>() {
    override fun areItemsTheSame(oldItem: FloorDataModel, newItem: FloorDataModel): Boolean =
        oldItem.floorId == newItem.floorId

    override fun areContentsTheSame(oldItem: FloorDataModel, newItem: FloorDataModel): Boolean =
        oldItem == newItem
}

