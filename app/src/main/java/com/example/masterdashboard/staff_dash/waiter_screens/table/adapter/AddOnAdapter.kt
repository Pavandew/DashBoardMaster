package com.example.masterdashboard.staff_dash.waiter_screens.table.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemAddonCheckboxRowBinding
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.AddonItem

class AddonsAdapter(
    private val addonsList: List<AddonItem>,
    private val onAddonSelectionChanged: () -> Unit
) : RecyclerView.Adapter<AddonsAdapter.AddonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddonViewHolder {
        val binding = ItemAddonCheckboxRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AddonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddonViewHolder, position: Int) {
        holder.bind(addonsList[position])
    }

    override fun getItemCount(): Int = addonsList.size

    inner class AddonViewHolder(private val binding: ItemAddonCheckboxRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(addon: AddonItem) {
            binding.tvAddonName.text = addon.name
            binding.tvAddonPrice.text = "+₹${addon.price.toInt()}"
            binding.cbAddon.isChecked = addon.isSelected

            binding.cbAddon.setOnCheckedChangeListener { _, isChecked ->
                addon.isSelected = isChecked
                onAddonSelectionChanged()
            }

            binding.root.setOnClickListener {
                binding.cbAddon.isChecked = !binding.cbAddon.isChecked
            }
        }
    }
}