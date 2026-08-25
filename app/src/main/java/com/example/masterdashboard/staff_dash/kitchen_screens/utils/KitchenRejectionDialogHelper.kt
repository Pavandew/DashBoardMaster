package com.example.masterdashboard.staff_dash.kitchen_screens.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.DialogKitchenRejectItemsBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenRejectItemsAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class KitchenRejectionDialogHelper(
    private val context: Context,
    private val layoutInflater: LayoutInflater
) {

    interface RejectionListener {
        fun onFullRejection(reason: String)
        fun onPartialRejection(remainingItems: List<OrderDetailItem>, reason: String)
    }

    fun showRejectionDialog(
        orderData: KitchenOrderDetailData,
        listener: RejectionListener
    ) {
        val binding = DialogKitchenRejectItemsBinding.inflate(layoutInflater)

        // Filter out items that are already "ordered" if you only want to show "new" items to reject
        // or show all items if you want to allow rejecting anything active.
        // For rejection, showing all currently active items is usually better.
        val activeItems = orderData.items.filter { it.quantity > 0 }

        val adapter = KitchenRejectItemsAdapter(activeItems)
        binding.rvRejectItems.adapter = adapter

        MaterialAlertDialogBuilder(context, R.style.CustomDialogTheme)
            .setView(binding.root)
            .setPositiveButton("Report Unavailable") { _, _ ->
                val selectedForRejection = adapter.getSelectedItems()
                val reason = binding.etRejectReason.text.toString().trim().ifEmpty { "Items unavailable" }

                if (selectedForRejection.isNotEmpty()) {
                    val remainingItems = orderData.items.filter { it !in selectedForRejection }

                    if (remainingItems.isEmpty()) {
                        listener.onFullRejection(reason)
                    } else {
                        listener.onPartialRejection(remainingItems, reason)
                    }
                } else {
                    Toast.makeText(context, "No items selected for rejection", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}