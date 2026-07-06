package com.example.masterdashboard.popup_manager

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.example.masterdashboard.databinding.DailogLogoutPremiumBinding

class LogoutPopupManager(private val context: Context) {
    fun showLogoutPopup(onConfirm: () -> Unit) {
        val dialog = Dialog(context).apply { requestWindowFeature(Window.FEATURE_NO_TITLE) }
        val binding = DailogLogoutPremiumBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.btnLogoutConfirm.setOnClickListener { dialog.dismiss(); onConfirm() }
        binding.btnLogoutCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }
}