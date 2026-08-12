package com.example.masterdashboard.staff_dash.waiter_screens.table.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemAddonCheckboxRowBinding

/**
 * Common data class for both Variants and Addons to keep the UI logic simple.
 */
data class CustomizationItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    var isSelected: Boolean = false,
    val isVariant: Boolean = false // If true, handle single selection (radio-like)
)

class AddonsAdapter(
    private val items: List<CustomizationItem>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<AddonsAdapter.CustomizationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomizationViewHolder {
        val binding = ItemAddonCheckboxRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CustomizationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomizationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CustomizationViewHolder(private val binding: ItemAddonCheckboxRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CustomizationItem) {
            binding.tvAddonName.text = item.name
            
            // Variants show absolute price (e.g. ₹250), Addons show "+" (e.g. +₹30)
            binding.tvAddonPrice.text = if (item.isVariant) "₹${item.price.toInt()}" else "+₹${item.price.toInt()}"
            
            binding.cbAddon.isChecked = item.isSelected

            binding.cbAddon.setOnClickListener {
                handleItemSelection(item)
            }

            binding.root.setOnClickListener {
                handleItemSelection(item)
            }
        }

        private fun handleItemSelection(selectedItem: CustomizationItem) {
            if (selectedItem.isVariant) {
                // Single selection logic for variants: Deselect all other variants
                items.filter { it.isVariant }.forEach { it.isSelected = (it == selectedItem) }
                notifyDataSetChanged()
            } else {
                // Multi selection logic for addons
                selectedItem.isSelected = !selectedItem.isSelected
                binding.cbAddon.isChecked = selectedItem.isSelected
            }
            onSelectionChanged()
        }
    }
}
