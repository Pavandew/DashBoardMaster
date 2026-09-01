package com.example.masterdashboard.staff_dash.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import android.widget.TextView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.DialogCustomerInfoBinding
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableCardData
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object TableDialogHelper {

    /**
     * Shows the dialog to collect customer info and decide between Dine-in or Reservation.
     */
    fun showCustomerInfoDialog(
        context: Context,
        inflater: LayoutInflater,
        onStartOrder: (name: String, phone: String) -> Unit,
        onReserve: (name: String) -> Unit
    ) {
        val binding = DialogCustomerInfoBinding.inflate(inflater)
        val dialog = MaterialAlertDialogBuilder(context, R.style.CustomDialogTheme)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        binding.btnDialogConfirm.setOnClickListener {
            val name = binding.etDialogCustomerName.text.toString().trim()
            val phone = binding.etDialogPhoneNumber.text.toString().trim()
            onStartOrder(name, phone)
            dialog.dismiss()
        }

        binding.btnDialogReserve.setOnClickListener {
            val name = binding.etDialogCustomerName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(context, "Please enter customer name to reserve", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onReserve(name)
            dialog.dismiss()
        }

        binding.btnDialogSkip.setOnClickListener {
            onStartOrder("", "")
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Shows management options for a table that is already RESERVED.
     */
    fun showReservedTableActionsDialog(
        context: Context,
        inflater: LayoutInflater,
        table: TableCardData,
        onStartOrder: () -> Unit,
        onRelease: () -> Unit
    ) {
        val view = inflater.inflate(R.layout.dialog_reserved_table_actions, null)
        val dialog = MaterialAlertDialogBuilder(context, R.style.CustomDialogTheme)
            .setView(view)
            .setCancelable(true)
            .create()

        val tvTableName = view.findViewById<TextView>(R.id.tvDialogTableName)
        val tvCustomerName = view.findViewById<TextView>(R.id.tvDialogCustomerName)
        val btnStartOrder = view.findViewById<MaterialButton>(R.id.btnDialogStartOrder)
        val btnRelease = view.findViewById<MaterialButton>(R.id.btnDialogRelease)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnDialogCancel)

        tvTableName.text = "Table ${table.tableName} is Reserved"
        tvCustomerName.text = table.customerName ?: "Unknown Guest"

        btnStartOrder.setOnClickListener {
            onStartOrder()
            dialog.dismiss()
        }

        btnRelease.setOnClickListener {
            onRelease()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
