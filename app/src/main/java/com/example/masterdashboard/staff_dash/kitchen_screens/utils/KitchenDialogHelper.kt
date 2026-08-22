package com.example.masterdashboard.staff_dash.kitchen_screens.utils

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.DialogAddInventoryItemBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.model.InventoryItem

/**
 * Helper class to manage complex dialogs for the Kitchen module.
 * Centralizing dialog logic keeps Fragments clean and ensures UI consistency.
 */
object KitchenDialogHelper {
    private const val TAG = "KitchenDialogHelper"

    /**
     * Shows a customized dialog for Adding or Editing an Inventory Item.
     * @param categories Existing categories for AutoComplete suggestions.
     */
    fun showInventoryAddEditDialog(
        context: Context,
        item: InventoryItem?,
        categories: List<String>,
        onSave: (InventoryItem) -> Unit,
        onDelete: (String) -> Unit
    ) {
        val binding = DialogAddInventoryItemBinding.inflate(LayoutInflater.from(context))
        
        val builder = AlertDialog.Builder(context, R.style.CustomDialogTheme)
            .setView(binding.root)

        val dialog = builder.create()

        // Configure Category AutoComplete
        val categoryAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)
        binding.etCategory.setAdapter(categoryAdapter)

        // Configure Unit Spinner
        val units = arrayOf("kg", "gm", "Liters", "ml", "Packets", "Units", "Boxes", "Pieces")
        val unitAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, units)
        binding.spinnerUnit.adapter = unitAdapter

        // Configure Manual Status Spinner
        val statuses = arrayOf("AUTO (Calculate)", "In Stock", "Low Stock", "Out of Stock")
        val statusAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, statuses)
        binding.spinnerStatus.adapter = statusAdapter

        // Pre-fill data if editing
        if (item != null) {
            binding.tvDialogTitle.text = "Edit Inventory Item"
            binding.etItemName.setText(item.itemName)
            binding.etCategory.setText(item.inventoryCategory)
            binding.etQuantity.setText(item.itemQuantity.toString())
            binding.etMinThreshold.setText(item.minThreshold.toString())
            binding.etDaysLeft.setText(item.estimatedDaysLeft.toString())
            
            val unitPos = units.indexOf(item.itemUnit)
            if (unitPos >= 0) binding.spinnerUnit.setSelection(unitPos)

            val statusVal = if (item.manualStatus == "AUTO") "AUTO (Calculate)" else item.manualStatus
            val statusPos = statuses.indexOf(statusVal)
            if (statusPos >= 0) binding.spinnerStatus.setSelection(statusPos)
            
            binding.btnDelete.visibility = View.VISIBLE
        }

        // Logic for Save Button
        binding.btnSave.setOnClickListener {
            val name = binding.etItemName.text.toString().trim()
            val category = binding.etCategory.text.toString().trim()
            val qty = binding.etQuantity.text.toString().toDoubleOrNull() ?: 0.0
            val threshold = binding.etMinThreshold.text.toString().toDoubleOrNull() ?: 0.0
            val daysLeft = binding.etDaysLeft.text.toString().toIntOrNull() ?: 0
            val unit = binding.spinnerUnit.selectedItem.toString()
            
            val selectedStatus = binding.spinnerStatus.selectedItem.toString()
            val manualStatus = if (selectedStatus == "AUTO (Calculate)") "AUTO" else selectedStatus

            if (name.isEmpty()) {
                binding.etItemName.error = "Required"
                return@setOnClickListener
            }
            if (category.isEmpty()) {
                binding.etCategory.error = "Required"
                return@setOnClickListener
            }

            val updatedItem = InventoryItem(
                inventoryId = item?.inventoryId ?: "",
                itemName = name,
                itemQuantity = qty,
                itemUnit = unit,
                minThreshold = threshold,
                inventoryCategory = category,
                estimatedDaysLeft = daysLeft,
                manualStatus = manualStatus,
                lastUpdated = System.currentTimeMillis()
            )

            onSave(updatedItem)
            dialog.dismiss()
        }

        binding.btnDelete.setOnClickListener {
            item?.inventoryId?.let { id ->
                onDelete(id)
                dialog.dismiss()
            }
        }

        binding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()

        // OPTIMIZATION: Fix dialog width to match parent with horizontal margins
        dialog.window?.let { window ->
            val width = (context.resources.displayMetrics.widthPixels * 0.95).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
}
