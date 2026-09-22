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

        // Filter to show ONLY new / unprepared items from the current KOT that can be rejected
        val activeItems = orderData.items.filter { 
            it.quantity > it.orderedQuantity || it.quantity > it.readyQuantity 
        }

        val adapter = KitchenRejectItemsAdapter(activeItems)
        binding.rvRejectItems.adapter = adapter

        MaterialAlertDialogBuilder(context, R.style.CustomDialogTheme)
            .setView(binding.root)
            .setPositiveButton("Report Unavailable") { _, _ ->
                val selectedForRejection = adapter.getSelectedItems()
                val reason = binding.etRejectReason.text.toString().trim().ifEmpty { "Items unavailable" }

                if (selectedForRejection.isNotEmpty()) {
                    val remainingItems = orderData.items.mapNotNull { item ->
                        if (item in selectedForRejection) {
                            val newDelta = maxOf(0, item.quantity - item.orderedQuantity)
                            val remainingQty = if (newDelta > 0) item.quantity - newDelta else 0
                            if (remainingQty > 0) {
                                item.copy(
                                    quantity = remainingQty,
                                    orderedQuantity = remainingQty,
                                    readyQuantity = minOf(item.readyQuantity, remainingQty),
                                    rowTotal = item.price * remainingQty
                                )
                            } else {
                                null // Fully rejected
                            }
                        } else {
                            item // Kept as is
                        }
                    }

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