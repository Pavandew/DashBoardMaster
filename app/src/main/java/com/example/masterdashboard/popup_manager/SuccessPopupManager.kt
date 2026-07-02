package com.example.masterdashboard.popup_manager


import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.example.masterdashboard.databinding.DailogSuccessPremiumBinding

class SuccessPopupManager(private val context: Context) {
    fun showSuccessPopup(customMessage: String, onDismiss: () -> Unit = {}) {
        val dialog = Dialog(context).apply { requestWindowFeature(Window.FEATURE_NO_TITLE) }
        val binding = DailogSuccessPremiumBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        binding.tvSuccessMessage.text = customMessage
        binding.btnSuccessDone.setOnClickListener { dialog.dismiss(); onDismiss() }

        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }
}