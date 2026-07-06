package com.example.masterdashboard.popup_manager

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.example.masterdashboard.databinding.DailogDeletePremiumBinding

class DeletePopupManager(private val context: Context) {
    fun showDeletePopup(targetItem: String, onConfirmDelete: () -> Unit) {
        val dialog = Dialog(context).apply { requestWindowFeature(Window.FEATURE_NO_TITLE) }
        val binding = DailogDeletePremiumBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.tvDeleteMessage.text = "Are you certain you want to delete '$targetItem'? This structural change is permanent."
        binding.btnDeleteConfirm.setOnClickListener { dialog.dismiss(); onConfirmDelete() }
        binding.btnDeleteCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }
}