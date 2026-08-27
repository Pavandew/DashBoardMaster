package com.example.masterdashboard.staff_dash.waiter_screens.table.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemTableCardBinding
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableCardData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableStatus

class TableCardsAdapter(
    private val onTableClick: (TableCardData) -> Unit
) : ListAdapter<TableCardData, TableCardsAdapter.TableCardViewHolder>(TableDiffCallback()) {

    fun updateList(newList: List<TableCardData>) {
        submitList(newList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableCardViewHolder {
        val binding = ItemTableCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TableCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TableCardViewHolder, position: Int) {
        holder.bind(getItem(position), onTableClick)
    }

    class TableCardViewHolder(val binding: ItemTableCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(table: TableCardData, onTableClick: (TableCardData) -> Unit) {
            val context = itemView.context
            binding.tvTableName.text = table.tableName
            binding.tvFloorName.text = table.floorName
            binding.tvSeatCount.text = context.getString(R.string.seat_count_format, table.totalSeats)

            // Show customer name only if the table is NOT free
            val isActuallyFree = table.status == TableStatus.FREE
            if (!table.customerName.isNullOrEmpty() && !isActuallyFree) {
                binding.tvCustomerName.visibility = View.VISIBLE
                binding.tvCustomerName.text = table.customerName
            } else {
                binding.tvCustomerName.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onTableClick(table)
            }

            when (table.status) {
                TableStatus.FREE -> {
                    binding.tvTableStatus.text = context.getString(R.string.status_free)
                    binding.tvTableStatus.setBackgroundResource(R.drawable.bg_status_free)
                    binding.tvTableStatus.setTextColor(ContextCompat.getColor(context, R.color.status_free))
                    binding.ivTableIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_free))
                    binding.cvTableCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                    binding.tvTablePrice.visibility = View.GONE
                }

                TableStatus.OCCUPIED -> {
                    binding.tvTableStatus.text = context.getString(R.string.status_occupied)
                    binding.tvTableStatus.setBackgroundResource(R.drawable.bg_status_occupied)
                    binding.tvTableStatus.setTextColor(ContextCompat.getColor(context, R.color.status_occupied))
                    binding.ivTableIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_occupied))
                    binding.cvTableCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_occupied_bg))
                    binding.tvTablePrice.visibility = View.GONE
                    binding.tvTablePrice.text = table.price ?: "—"
                }

                TableStatus.RESERVED -> {
                    binding.tvTableStatus.text = context.getString(R.string.status_reserved)
                    binding.tvTableStatus.setBackgroundResource(R.drawable.bg_status_amber)
                    binding.tvTableStatus.setTextColor(ContextCompat.getColor(context, R.color.status_reserved))
                    binding.ivTableIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_reserved))
                    binding.cvTableCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_reserved_bg))
                    binding.tvTablePrice.visibility = View.GONE
                }

                TableStatus.BILLING -> {
                    binding.tvTableStatus.text = context.getString(R.string.status_billing)
                    binding.tvTableStatus.setBackgroundResource(R.drawable.bg_status_blue)
                    binding.tvTableStatus.setTextColor(ContextCompat.getColor(context, R.color.status_billing))
                    binding.ivTableIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_billing))
                    binding.cvTableCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_billing_bg))
                    binding.tvTablePrice.visibility = View.GONE
                }
            }
        }
    }

    class TableDiffCallback : DiffUtil.ItemCallback<TableCardData>() {
        override fun areItemsTheSame(oldItem: TableCardData, newItem: TableCardData): Boolean {
            return oldItem.tableId == newItem.tableId
        }

        override fun areContentsTheSame(oldItem: TableCardData, newItem: TableCardData): Boolean {
            return oldItem == newItem
        }
    }
}
