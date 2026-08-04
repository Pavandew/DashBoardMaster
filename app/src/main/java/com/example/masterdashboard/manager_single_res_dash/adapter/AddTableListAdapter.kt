package com.example.masterdashboard.manager_single_res_dash.table_management.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemTableGridCellBinding
import com.example.masterdashboard.manager_single_res_dash.models.TableData

class AddTableListAdapter(
    private val onTableClick: (TableData) -> Unit,
    private val onTableLongClick: (TableData) -> Unit
) : ListAdapter<TableData, AddTableListAdapter.TableViewHolder>(TableDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val binding = ItemTableGridCellBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TableViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        holder.bind(getItem(position), onTableClick, onTableLongClick)
    }

    inner class TableViewHolder(
        private val binding: ItemTableGridCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            table: TableData,
            onClick: (TableData) -> Unit,
            onLongClick: (TableData) -> Unit
        ) {
            // 1. Map data models values directly onto your text components
            binding.tvTableShortCode.text = table.tableName
            binding.tvTableStatusLabel.text = table.status
            binding.tvTableCapacityLabel.text = "${table.capacity} Seats"

            // 2. Compute dynamic color palette highlights for textual emphasis
            val statusColorString = when (table.status.uppercase()) {
                "AVAILABLE" -> "#4CAF50"  // Soft Green UI
                "OCCUPIED" -> "#FF9800"   // Soft Orange UI
                "RESERVED" -> "#BA68C8"   // Soft Purple UI
                else -> "#E57373"         // DIRTY / BLOCKED Red UI
            }

            val calculatedStatusColor = Color.parseColor(statusColorString)

            // Applying colors explicitly to the label LIVING OUTSIDE the card
            binding.tvTableStatusLabel.setTextColor(calculatedStatusColor)

            // Soft white/gray color mapping for inside card metadata text layout fields
            binding.tvTableCapacityLabel.setTextColor(Color.parseColor("#A5A1CD"))

            // 3. Apply solid background fills to the inner card layouts cleanly
            when (table.status.uppercase()) {
                "AVAILABLE" -> {
                    binding.clCellBackground.setBackgroundColor(Color.parseColor("#112B1B"))
                    binding.tvTableShortCode.setTextColor(Color.parseColor("#4CAF50"))
                }
                "OCCUPIED" -> {
                    binding.clCellBackground.setBackgroundColor(Color.parseColor("#3E2006"))
                    binding.tvTableShortCode.setTextColor(Color.parseColor("#FF9800"))
                }
                "RESERVED" -> {
                    binding.clCellBackground.setBackgroundColor(Color.parseColor("#2C123D"))
                    binding.tvTableShortCode.setTextColor(Color.parseColor("#BA68C8"))
                }
                else -> { // DIRTY / BLOCKED
                    binding.clCellBackground.setBackgroundColor(Color.parseColor("#321111"))
                    binding.tvTableShortCode.setTextColor(Color.parseColor("#E57373"))
                }
            }

            // Bind interaction click streams specifically to the Card component bounds
            binding.cvTableBoxCard.setOnClickListener { onClick(table) }
            binding.cvTableBoxCard.setOnLongClickListener {
                onLongClick(table)
                true
            }
        }
    }
}

class TableDiffCallback : DiffUtil.ItemCallback<TableData>() {
    // Dictates item replacement checks purely by unique backend document path keys
    override fun areItemsTheSame(oldItem: TableData, newItem: TableData): Boolean =
        oldItem.tableId == newItem.tableId

    // Ensures any changes inside metadata fields trigger an immediate, smooth item redraw animation
    override fun areContentsTheSame(oldItem: TableData, newItem: TableData): Boolean =
        oldItem == newItem
}

