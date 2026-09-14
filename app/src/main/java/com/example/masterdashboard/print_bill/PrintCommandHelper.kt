package com.example.masterdashboard.print_bill

import android.util.Log
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Helper class to generate ESC/POS byte commands for thermal printers.
 * Formats restaurant bills adhering to Indian GST standards and real-world POS layout.
 */
class PrintCommandHelper {

    companion object {
        private const val TAG = "PrintCommandHelper"
        
        // ESC/POS Commands
        private val ESC = byteArrayOf(0x1B)
        private val GS = byteArrayOf(0x1D)
        
        val ALIGN_LEFT = ESC + byteArrayOf(0x61, 0x00)
        val ALIGN_CENTER = ESC + byteArrayOf(0x61, 0x01)
        val ALIGN_RIGHT = ESC + byteArrayOf(0x61, 0x02)
        
        val BOLD_ON = ESC + byteArrayOf(0x45, 0x01)
        val BOLD_OFF = ESC + byteArrayOf(0x45, 0x00)
        
        val TEXT_SIZE_NORMAL = GS + byteArrayOf(0x21, 0x00)
        val TEXT_SIZE_LARGE = GS + byteArrayOf(0x21, 0x11) // Double height & width
        
        val FEED_LINE = byteArrayOf(0x0A)
        val PAPER_CUT = GS + byteArrayOf(0x56, 0x41, 0x00)
        
        const val LINE_WIDTH = 32 // Standard for 58mm thermal printers
    }

    /**
     * Generates a byte array for a complete bill matching professional restaurant receipts.
     * Respects customized header tagline, footer message, and GSTIN/FSSAI toggles.
     */
    fun generateBillBytes(
        restaurantName: String,
        address: String = "",
        gstin: String = "",
        fssai: String = "",
        cashierName: String = "Cashier",
        gstRate: Double = 5.0,
        headerTagline: String = "",
        footerMessage: String = "Thank You Visit Again",
        showCustomerInfo: Boolean = true,
        order: CashierBillingOrderModel
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        Log.d(TAG, "Generating print bytes for Order: ${order.orderId}")

        // 1. Header (Restaurant Name & Registration Details)
        bytes.addAll(ALIGN_CENTER.toList())
        bytes.addAll(TEXT_SIZE_LARGE.toList())
        bytes.addAll(BOLD_ON.toList())
        bytes.addAll(restaurantName.toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        
        bytes.addAll(TEXT_SIZE_NORMAL.toList())
        bytes.addAll(BOLD_OFF.toList())

        if (headerTagline.isNotEmpty()) {
            bytes.addAll(headerTagline.toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        if (address.isNotEmpty()) {
            bytes.addAll(address.toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        if (gstin.isNotEmpty()) {
            bytes.addAll("GSTIN : $gstin".toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        if (fssai.isNotEmpty()) {
            bytes.addAll("FSSAI : $fssai".toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        // 2. Order Metadata Block
        bytes.addAll(createDoubleSeparator().toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        val sdfDate = SimpleDateFormat("dd-MMM-yy HH.mm", Locale.US)
        val formattedDate = sdfDate.format(order.timestamp.toDate())
        val orderNum = if (order.orderId.length > 6) order.orderId.takeLast(6) else order.orderId

        bytes.addAll(ALIGN_LEFT.toList())
        bytes.addAll(formatTwoColumns("Bill No: $orderNum", "Date: $formattedDate").toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        val stewardName = order.waiterId.ifEmpty { "Cashier" }
        bytes.addAll(formatTwoColumns("TableNo: ${order.tableName}", "Steward: $stewardName").toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        if (showCustomerInfo && order.customerName.isNotEmpty()) {
            bytes.addAll("Customer: ${order.customerName}".toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        // 3. Items Table Header
        bytes.addAll(createDoubleSeparator().toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll("SNo. Description     Qty   Amount".toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(createDoubleSeparator().toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // 4. Items List
        var totalItemCount = 0
        order.items.forEachIndexed { index, item ->
            val sno = (index + 1).toString().padStart(2, ' ')
            val qtyStr = item.quantity.toString()
            val amountStr = String.format(Locale.US, "%.2f", item.rowTotal.toDouble())
            val name = if (item.variantName.isNotEmpty()) "${item.itemName} (${item.variantName})" else item.itemName
            totalItemCount += item.quantity

            bytes.addAll(formatItemLine(sno, name, qtyStr, amountStr).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        bytes.addAll(createSingleSeparator().toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // 5. Totals & Tax Breakdown Section
        bytes.addAll(ALIGN_LEFT.toList())
        bytes.addAll(formatTwoColumns("Total Amount", String.format(Locale.US, "%.2f", order.subtotal)).toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(createSingleSeparator().toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // Service Charge
        val sc = order.serviceChargeAmount
        val scText = if (sc > 0) "SERVICE CHARGE" else "SERVICE CHARGE@0%"
        bytes.addAll(formatTwoColumns(scText, String.format(Locale.US, "%.2f", sc)).toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // GST Tax Breakdown
        if (order.taxAmount > 0) {
            val halfRate = gstRate / 2.0
            val halfTax = order.taxAmount / 2.0
            val sgstLabel = String.format(Locale.US, "State Gst@%.1f%%", halfRate)
            val cgstLabel = String.format(Locale.US, "Central Gst@%.1f%%", halfRate)

            bytes.addAll(formatTwoColumns(sgstLabel, String.format(Locale.US, "%.2f", halfTax)).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
            bytes.addAll(formatTwoColumns(cgstLabel, String.format(Locale.US, "%.2f", halfTax)).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        if (order.discountAmount > 0) {
            bytes.addAll(formatTwoColumns("Discount", String.format(Locale.US, "-%.2f", order.discountAmount)).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        // Round Off Adjustment
        val unroundedGross = order.subtotal + order.serviceChargeAmount + order.taxAmount - order.discountAmount
        val roundedGross = Math.round(unroundedGross).toDouble()
        val roundOff = roundedGross - unroundedGross

        if (Math.abs(roundOff) > 0.001) {
            val roundOffStr = String.format(Locale.US, "%+.2f", roundOff)
            bytes.addAll(formatTwoColumns("Round Off", roundOffStr).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        bytes.addAll(createDoubleSeparator().toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // Gross Amount (Grand Total)
        bytes.addAll(BOLD_ON.toList())
        bytes.addAll(formatTwoColumns("Gross Amount", String.format(Locale.US, "%.2f", roundedGross)).toByteArray().toList())
        bytes.addAll(BOLD_OFF.toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(createDoubleSeparator().toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // 6. Footer Info Block
        bytes.addAll("Total Items    : $totalItemCount".toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll("UserID/Cashier : $cashierName".toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(FEED_LINE.toList())

        val activeFooter = footerMessage.ifEmpty { "Thank You Visit Again" }
        bytes.addAll(ALIGN_CENTER.toList())
        bytes.addAll(activeFooter.toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        bytes.addAll(ALIGN_RIGHT.toList())
        bytes.addAll("Guest Copy".toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(PAPER_CUT.toList())

        Log.i(TAG, "Print bytes generated. Total size: ${bytes.size} bytes")
        return bytes.toByteArray()
    }

    private fun formatTwoColumns(left: String, right: String): String {
        val spaceNeeded = LINE_WIDTH - left.length - right.length
        return if (spaceNeeded > 0) {
            left + " ".repeat(spaceNeeded) + right
        } else {
            left + "\n" + " ".repeat(Math.max(1, LINE_WIDTH - right.length)) + right
        }
    }

    private fun formatItemLine(sno: String, name: String, qty: String, amount: String): String {
        val maxNameLen = 14
        val trimmedName = if (name.length > maxNameLen) name.substring(0, maxNameLen) else name
        return "$sno $trimmedName".padEnd(19) + qty.padStart(3) + amount.padStart(10)
    }

    private fun createDoubleSeparator(): String = "=".repeat(LINE_WIDTH)
    private fun createSingleSeparator(): String = "-".repeat(LINE_WIDTH)

    /**
     * Generates a loggable plain-text preview for debugging.
     */
    fun getLoggablePreview(
        restaurantName: String,
        address: String = "",
        gstin: String = "",
        fssai: String = "",
        cashierName: String = "Cashier",
        headerTagline: String = "",
        footerMessage: String = "Thank You Visit Again",
        order: CashierBillingOrderModel
    ): String {
        val sb = StringBuilder()
        sb.append("\n----------- PRINTER PREVIEW -----------\n")
        sb.append(restaurantName.uppercase().padStart((LINE_WIDTH + restaurantName.length) / 2)).append("\n")
        if (headerTagline.isNotEmpty()) sb.append(headerTagline).append("\n")
        if (address.isNotEmpty()) sb.append(address).append("\n")
        if (gstin.isNotEmpty()) sb.append("GSTIN : $gstin\n")
        if (fssai.isNotEmpty()) sb.append("FSSAI : $fssai\n")
        sb.append(createDoubleSeparator()).append("\n")
        sb.append("Bill No: #${order.orderId.takeLast(6)}".padEnd(16)).append("Table: ${order.tableName}\n")
        sb.append(createDoubleSeparator()).append("\n")
        sb.append("SNo. Description     Qty   Amount\n")
        sb.append(createDoubleSeparator()).append("\n")
        order.items.forEachIndexed { i, it ->
            sb.append(formatItemLine((i + 1).toString(), it.itemName, "x${it.quantity}", String.format(Locale.US, "%.2f", it.rowTotal.toDouble()))).append("\n")
        }
        sb.append(createSingleSeparator()).append("\n")
        val gross = Math.round(order.subtotal + order.serviceChargeAmount + order.taxAmount - order.discountAmount).toDouble()
        sb.append(formatTwoColumns("Gross Amount", String.format(Locale.US, "%.2f", gross))).append("\n")
        sb.append("UserID/Cashier : $cashierName\n")
        sb.append(footerMessage.ifEmpty { "Thank You Visit Again" }).append("\n")
        sb.append("---------------------------------------\n")
        return sb.toString()
    }
}
