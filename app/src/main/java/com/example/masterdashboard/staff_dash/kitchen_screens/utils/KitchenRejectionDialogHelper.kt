package com.example.masterdashboard.staff_dash.kitchen_screens.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenRejectItemsAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

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
        val dialogView = layoutInflater.inflate(R.layout.dialog_kitchen_reject_items, null)
        val rvRejectItems = dialogView.findViewById<RecyclerView>(R.id.rvRejectItems)
        val etRejectReason = dialogView.findViewById<TextInputEditText>(R.id.etRejectReason)

        // Filter out items that are already "ordered" if you only want to show "new" items to reject
        // or show all items if you want to allow rejecting anything active.
        // For rejection, showing all currently active items is usually better.
        val activeItems = orderData.items.filter { it.quantity > 0 }

        val adapter = KitchenRejectItemsAdapter(activeItems)
        rvRejectItems.adapter = adapter

        MaterialAlertDialogBuilder(context, R.style.CustomDialogTheme)
            .setView(dialogView)
            .setPositiveButton("Report Unavailable") { _, _ ->
                val selectedForRejection = adapter.getSelectedItems()
                val reason = etRejectReason.text.toString().trim().ifEmpty { "Items unavailable" }

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