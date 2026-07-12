package com.example.masterdashboard.staff_dash.kitchen_screens.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.graphics.Typeface
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R

class KitchenFilterChipAdapter(
    private val chipItems: List<String>,
    private var selectedIndex: Int = 0,
    private val onChipClicked: (String) -> Unit
) : RecyclerView.Adapter<KitchenFilterChipAdapter.ChipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card_chips, parent, false) as TextView
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val itemText = chipItems[position]
        holder.textView.text = itemText

        val context = holder.itemView.context
        if (position == selectedIndex) {
            // Make selected text bold and ensure selected text color
            holder.textView.setTypeface(holder.textView.typeface, Typeface.BOLD)
            holder.textView.setTextColor(ContextCompat.getColor(context, R.color.black))
        } else {
            // Normal weight for unselected items and unselected styles
            holder.textView.setTypeface(holder.textView.typeface, Typeface.NORMAL)
            holder.textView.setBackgroundResource(R.drawable.bg_chip_unselected)
            holder.textView.setTextColor(ContextCompat.getColor(context, R.color.chip_unselected_text))
        }

        holder.itemView.setOnClickListener {
            val oldIndex = selectedIndex
            selectedIndex = holder.adapterPosition
            notifyItemChanged(oldIndex)
            notifyItemChanged(selectedIndex)
            onChipClicked(itemText)
        }
    }

    override fun getItemCount(): Int = chipItems.size

    class ChipViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}