package com.example.masterdashboard.staff_dash.utils

import android.content.Context
import androidx.core.graphics.toColorInt
import android.view.LayoutInflater
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.DialogPaymentQrBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

/**
 * Centrally manages the Payment Confirmation (QR/Verification) dialog.
 * Used by OrderPaymentFragment and CashierSettleBillFragment to maintain UI consistency.
 */
object PaymentDialogHelper {

    fun showPaymentConfirmation(
        context: Context,
        amount: Double,
        paymentMethod: String,
        onConfirm: () -> Unit
    ) {
        val inflater = LayoutInflater.from(context)
        val binding = DialogPaymentQrBinding.inflate(inflater)

        // Set amount
        binding.tvAmountToPay.text = context.getString(R.string.amount_format, String.format(Locale.US, "%.2f", amount))
        
        // Set dynamic title based on method
        binding.tvPaymentTitle.text = context.getString(R.string.payment_title_format, paymentMethod)

        // Configure Subtitle and Icon based on payment type
        when (paymentMethod.uppercase(Locale.US)) {
            "UPI" -> {
                binding.tvPaymentSubtitle.text = context.getString(R.string.payment_scan_qr)
                binding.ivPaymentQr.setImageResource(R.drawable.ic_upi_pay_24dp)
                binding.ivPaymentQr.colorFilter = null
            }
            "CASH" -> {
                binding.tvPaymentSubtitle.text = context.getString(R.string.payment_confirm_cash)
                binding.ivPaymentQr.setImageResource(R.drawable.ic_payments_24dp)
                binding.ivPaymentQr.setColorFilter("#16A34A".toColorInt())
            }
            "CARD" -> {
                binding.tvPaymentSubtitle.text = context.getString(R.string.payment_confirm_card)
                binding.ivPaymentQr.setImageResource(R.drawable.ic_card_payment_24dp)
                binding.ivPaymentQr.setColorFilter("#3554FF".toColorInt())
            }
            else -> {
                binding.tvPaymentSubtitle.text = context.getString(R.string.payment_verify_receipt)
                binding.ivPaymentQr.setImageResource(R.drawable.ic_inventory_24dp)
                binding.ivPaymentQr.colorFilter = null
            }
        }

        val dialog = MaterialAlertDialogBuilder(context, R.style.CustomDialogTheme)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        binding.btnCancelPayment.setOnClickListener { dialog.dismiss() }
        
        binding.btnConfirmPayment.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
    }
}
