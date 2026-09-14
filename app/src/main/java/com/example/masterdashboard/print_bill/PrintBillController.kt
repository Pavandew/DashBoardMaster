package com.example.masterdashboard.print_bill

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.BillingPrinterSettings
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Orchestrates the UI flow for printing a bill:
 * 1. Permission Check
 * 2. Printer Discovery/Selection
 * 3. Command Generation
 * 4. Bluetooth Transmission
 */
class PrintBillController(
    private val fragment: Fragment,
    private val onProgressUpdate: (Boolean) -> Unit
) {
    private val TAG = "PrintBillController"
    private val context = fragment.requireContext()
    private val printerManager = BluetoothPrinterManager(context)
    private val printHelper = PrintCommandHelper()
    private val sessionManager = SessionManager(context)

    // Order to be printed
    private var pendingOrder: CashierBillingOrderModel? = null

    // Permission launcher must be registered in the Fragment/Activity
    private val bluetoothPermissionLauncher: ActivityResultLauncher<Array<String>> =
        fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Log.i(TAG, "Bluetooth permissions granted by user")
                pendingOrder?.let { startPrintFlow(it) }
            } else {
                Toast.makeText(context, "Bluetooth permissions are required to print", Toast.LENGTH_SHORT).show()
            }
        }

    /**
     * Entry point: Call this from the Fragment's button click.
     */
    fun checkAndPrint(order: CashierBillingOrderModel) {
        this.pendingOrder = order
        
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            startPrintFlow(order)
        } else {
            bluetoothPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun startPrintFlow(order: CashierBillingOrderModel) {
        if (!printerManager.isBluetoothEnabled()) {
            Toast.makeText(context, "Please turn on Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedPrinters = printerManager.getPairedPrinters()
        if (pairedPrinters.isEmpty()) {
            Toast.makeText(context, "No paired printers found. Please pair your printer in settings.", Toast.LENGTH_LONG).show()
            return
        }

        showPrinterSelectionDialog(order, pairedPrinters)
    }

    @SuppressLint("MissingPermission")
    private fun showPrinterSelectionDialog(order: CashierBillingOrderModel, printers: List<BluetoothDevice>) {
        val printerNames = printers.map { it.name ?: "Unknown Printer" }
        
        val listView = ListView(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, printerNames)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Select Thermal Printer")
            .setView(listView)
            .setNegativeButton("Cancel", null)
            .show()

        listView.setOnItemClickListener { _, _, position, _ ->
            dialog.dismiss()
            executePrint(order, printers[position])
        }
    }

    private fun executePrint(order: CashierBillingOrderModel, device: BluetoothDevice) {
        val cachedDetails = sessionManager.getCachedRestaurantDetails()
        val receiptSettings = cachedDetails?.getEffectiveBillingPrinterSettings() ?: BillingPrinterSettings()

        val restaurantName = sessionManager.getRestaurantName().ifEmpty { sessionManager.getUserName() ?: "Restaurant" }

        val addressStr = cachedDetails?.let {
            val parts = listOf(it.address, it.landmark, "${it.city} ${it.pinCode}".trim()).filter { p -> p.isNotEmpty() }
            parts.joinToString(", ")
        } ?: ""

        val gstin = if (receiptSettings.showGstin) (cachedDetails?.gstNumber ?: "") else ""
        val fssai = if (receiptSettings.showFssai) (cachedDetails?.fssaiNumber ?: "") else ""
        val tagline = receiptSettings.customHeaderTagline
        val footerMsg = receiptSettings.customFooterMessage.ifEmpty { "Thank You Visit Again" }
        val cashierName = sessionManager.getUserName() ?: "Cashier"
        val gstRate = sessionManager.getGstRate()

        // Logcat Preview for debugging without physical printer
        Log.i(TAG, "Generating Print Preview...")
        Log.d(TAG, printHelper.getLoggablePreview(
            restaurantName = restaurantName,
            address = addressStr,
            gstin = gstin,
            fssai = fssai,
            cashierName = cashierName,
            headerTagline = tagline,
            footerMessage = footerMsg,
            order = order
        ))

        val printBytes = printHelper.generateBillBytes(
            restaurantName = restaurantName,
            address = addressStr,
            gstin = gstin,
            fssai = fssai,
            cashierName = cashierName,
            gstRate = gstRate,
            headerTagline = tagline,
            footerMessage = footerMsg,
            showCustomerInfo = receiptSettings.showCustomerInfo,
            order = order
        )

        fragment.lifecycleScope.launch {
            printerManager.printData(device, printBytes, object : BluetoothPrinterManager.PrintListener {
                override fun onPrintStarted() {
                    onProgressUpdate(true)
                }

                override fun onPrintSuccess() {
                    onProgressUpdate(false)
                    Toast.makeText(context, "Bill Printed Successfully", Toast.LENGTH_SHORT).show()
                }

                override fun onPrintFailed(error: String) {
                    onProgressUpdate(false)
                    Log.e(TAG, "Print Failed: $error")
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
