package com.restroone.app.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.widget.Toast
import com.restroone.app.databinding.DialogTableQrBinding
import java.io.File
import java.io.FileOutputStream

object TableQrDialogHelper {

    fun showTableQrDialog(
        context: Context,
        restaurantId: String,
        floorName: String,
        tableId: String,
        tableName: String
    ) {
        val binding = DialogTableQrBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Construct the digital ordering deep link / URL
        val orderingUrl = "https://restroone.app/order?restaurantId=$restaurantId&tableId=$tableId&tableName=$tableName"

        binding.tvTableInfo.text = "Table: $tableName • Floor: $floorName"
        binding.tvQrUrl.text = orderingUrl

        // Generate QR Code bitmap
        val qrBitmap = QRCodeGenerator.generateQRCode(orderingUrl, 512, 512)
        if (qrBitmap != null) {
            binding.imgQrCode.setImageBitmap(qrBitmap)
        }

        binding.btnShareQr.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Table $tableName - Digital Menu & Ordering")
                putExtra(Intent.EXTRA_TEXT, "Scan or tap to order directly at $tableName ($floorName):\n$orderingUrl")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Table QR Link"))
        }

        binding.btnDownloadQr.setOnClickListener {
            if (qrBitmap != null) {
                try {
                    val imagesDir = File(context.getExternalFilesDir(null), "QR_Codes")
                    if (!imagesDir.exists()) imagesDir.mkdirs()
                    val file = File(imagesDir, "Table_${tableName}_QR.png")
                    val fos = FileOutputStream(file)
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    fos.close()
                    Toast.makeText(context, "QR Code saved successfully!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to save QR code: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }
}
