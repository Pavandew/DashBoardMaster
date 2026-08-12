package com.example.masterdashboard.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object MenuDialogHelper {
    /**
     * Displays a consistent, premium Material 3 confirmation dialog for permanent deletions.
     * Can be reused for food items, menu categories, staff accounts, etc.
     */
    fun showDeleteConfirmation(
        context: Context,
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton("Delete") { dialog, _ ->
                onConfirm() // Execute the passed code action
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Smooth closure
            }
            .show()
    }
}