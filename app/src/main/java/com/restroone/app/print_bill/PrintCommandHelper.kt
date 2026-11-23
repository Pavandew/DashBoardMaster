package com.restroone.app.print_bill

import android.util.Log
import com.restroone.app.staff_dash.billing_screens.model.CashierBillingOrderModel
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
        
        const val DEFAULT_LINE_WIDTH_58MM = 32 // Standard for 58mm (2-inch) thermal paper
        const val DEFAULT_LINE_WIDTH_80MM = 48 // Standard for 80mm (3-inch) thermal paper
    }

    /**
     * Generates a byte array for a complete bill matching professional restaurant receipts.
     * Respects customized header tagline, footer message, GSTIN/FSSAI toggles, and paper width (58mm/80mm).
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
        lineWidth: Int = DEFAULT_LINE_WIDTH_58MM,
        order: CashierBillingOrderModel
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        Log.d(TAG, "Generating print bytes for Order: ${order.orderId} (Paper Width: ${lineWidth} chars)")

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
        bytes.addAll(createDoubleSeparator(lineWidth).toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        val sdfDate = SimpleDateFormat("dd-MMM-yy HH.mm", Locale.US)
        val formattedDate = sdfDate.format(order.timestamp.toDate())
        val orderNum = if (order.orderId.length > 6) order.orderId.takeLast(6) else order.orderId

        bytes.addAll(ALIGN_LEFT.toList())
        bytes.addAll(formatTwoColumns("Bill No: $orderNum", "Date: $formattedDate", lineWidth).toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        val isDineIn = order.orderType.contains("DINE", ignoreCase = true) || order.orderType.contains("TABLE", ignoreCase = true)
        if (isDineIn) {
            val stewardName = order.waiterName.ifEmpty { if (order.waiterId.isNotEmpty()) "Steward" else "Cashier" }
            bytes.addAll(formatTwoColumns("Table: ${order.tableName}", "Steward: $stewardName", lineWidth).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        } else {
            val typeLabel = if (order.orderType.contains("DELIVERY", ignoreCase = true)) "DELIVERY" else "TAKEAWAY"
            bytes.addAll(formatTwoColumns("Type: $typeLabel", "Bill No: #$orderNum", lineWidth).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        if (showCustomerInfo && order.customerName.isNotEmpty()) {
            bytes.addAll("Customer: ${order.customerName}".toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        // 3. Items Table Header
        bytes.addAll(createDoubleSeparator(lineWidth).toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        val headerText = if (lineWidth >= 48) "SNo. Description                     Qty       Amount" else "SNo. Description     Qty   Amount"
        bytes.addAll(headerText.toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(createDoubleSeparator(lineWidth).toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // 4. Items List
        var totalItemCount = 0
        order.items.forEachIndexed { index, item ->
            val sno = (index + 1).toString().padStart(2, ' ')
            val qtyStr = item.quantity.toString()
            val amountStr = String.format(Locale.US, "%.2f", item.rowTotal.toDouble())
            val name = if (item.variantName.isNotEmpty()) "${item.itemName} (${item.variantName})" else item.itemName
            totalItemCount += item.quantity

            bytes.addAll(formatItemLine(sno, name, qtyStr, amountStr, lineWidth).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        bytes.addAll(createSingleSeparator(lineWidth).toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // 5. Totals & Tax Breakdown Section
        bytes.addAll(ALIGN_LEFT.toList())
        bytes.addAll(formatTwoColumns("Total Amount", String.format(Locale.US, "%.2f", order.subtotal), lineWidth).toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(createSingleSeparator(lineWidth).toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // Service Charge
        val sc = order.serviceChargeAmount
        val scText = if (sc > 0) "SERVICE CHARGE" else "SERVICE CHARGE@0%"
        bytes.addAll(formatTwoColumns(scText, String.format(Locale.US, "%.2f", sc), lineWidth).toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // GST Tax Breakdown
        if (order.taxAmount > 0) {
            val halfRate = gstRate / 2.0
            val halfTax = order.taxAmount / 2.0
            val sgstLabel = String.format(Locale.US, "State Gst@%.1f%%", halfRate)
            val cgstLabel = String.format(Locale.US, "Central Gst@%.1f%%", halfRate)

            bytes.addAll(formatTwoColumns(sgstLabel, String.format(Locale.US, "%.2f", halfTax), lineWidth).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
            bytes.addAll(formatTwoColumns(cgstLabel, String.format(Locale.US, "%.2f", halfTax), lineWidth).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        if (order.discountAmount > 0) {
            bytes.addAll(formatTwoColumns("Discount", String.format(Locale.US, "-%.2f", order.discountAmount), lineWidth).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        // Round Off Adjustment
        val unroundedGross = order.subtotal + order.serviceChargeAmount + order.taxAmount - order.discountAmount
        val roundedGross = Math.round(unroundedGross).toDouble()
        val roundOff = roundedGross - unroundedGross

        if (Math.abs(roundOff) > 0.001) {
            val roundOffStr = String.format(Locale.US, "%+.2f", roundOff)
            bytes.addAll(formatTwoColumns("Round Off", roundOffStr, lineWidth).toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        bytes.addAll(createDoubleSeparator(lineWidth).toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // Gross Amount (Grand Total)
        bytes.addAll(BOLD_ON.toList())
        bytes.addAll(formatTwoColumns("Gross Amount", String.format(Locale.US, "%.2f", roundedGross), lineWidth).toByteArray().toList())
        bytes.addAll(BOLD_OFF.toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(createDoubleSeparator(lineWidth).toByteArray().toList())
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

    private fun formatTwoColumns(left: String, right: String, lineWidth: Int = DEFAULT_LINE_WIDTH_58MM): String {
        val spaceNeeded = lineWidth - left.length - right.length
        return if (spaceNeeded > 0) {
            left + " ".repeat(spaceNeeded) + right
        } else {
            left + "\n" + " ".repeat(Math.max(1, lineWidth - right.length)) + right
        }
    }

    private fun formatItemLine(sno: String, name: String, qty: String, amount: String, lineWidth: Int = DEFAULT_LINE_WIDTH_58MM): String {
        val maxNameLen = if (lineWidth >= 48) 28 else 14
        val padName = if (lineWidth >= 48) 33 else 19
        val trimmedName = if (name.length > maxNameLen) name.substring(0, maxNameLen) else name
        return "$sno $trimmedName".padEnd(padName) + qty.padStart(3) + amount.padStart(10)
    }

    private fun createDoubleSeparator(lineWidth: Int = DEFAULT_LINE_WIDTH_58MM): String = "=".repeat(lineWidth)
    private fun createSingleSeparator(lineWidth: Int = DEFAULT_LINE_WIDTH_58MM): String = "-".repeat(lineWidth)

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
        lineWidth: Int = DEFAULT_LINE_WIDTH_58MM,
        order: CashierBillingOrderModel
    ): String {
        val sb = StringBuilder()
        sb.append("\n----------- PRINTER PREVIEW -----------\n")
        sb.append(restaurantName.uppercase().padStart((lineWidth + restaurantName.length) / 2)).append("\n")
        if (headerTagline.isNotEmpty()) sb.append(headerTagline).append("\n")
        if (address.isNotEmpty()) sb.append(address).append("\n")
        if (gstin.isNotEmpty()) sb.append("GSTIN : $gstin\n")
        if (fssai.isNotEmpty()) sb.append("FSSAI : $fssai\n")
        sb.append(createDoubleSeparator(lineWidth)).append("\n")
        val isDineIn = order.orderType.contains("DINE", ignoreCase = true) || order.orderType.contains("TABLE", ignoreCase = true)
        if (isDineIn) {
            val stewardName = order.waiterName.ifEmpty { if (order.waiterId.isNotEmpty()) "Steward" else "Cashier" }
            sb.append("Table: ${order.tableName}".padEnd(16)).append("Steward: $stewardName\n")
        } else {
            val typeLabel = if (order.orderType.contains("DELIVERY", ignoreCase = true)) "DELIVERY" else "TAKEAWAY"
            sb.append("Type: $typeLabel".padEnd(16)).append("Bill No: #${order.orderId.takeLast(6)}\n")
        }
        sb.append(createDoubleSeparator(lineWidth)).append("\n")
        val headerText = if (lineWidth >= 48) "SNo. Description                     Qty       Amount\n" else "SNo. Description     Qty   Amount\n"
        sb.append(headerText)
        sb.append(createDoubleSeparator(lineWidth)).append("\n")
        order.items.forEachIndexed { i, it ->
            sb.append(formatItemLine((i + 1).toString(), it.itemName, "x${it.quantity}", String.format(Locale.US, "%.2f", it.rowTotal.toDouble()), lineWidth)).append("\n")
        }
        sb.append(createSingleSeparator(lineWidth)).append("\n")
        val gross = Math.round(order.subtotal + order.serviceChargeAmount + order.taxAmount - order.discountAmount).toDouble()
        sb.append(formatTwoColumns("Gross Amount", String.format(Locale.US, "%.2f", gross), lineWidth)).append("\n")
        sb.append("UserID/Cashier : $cashierName\n")
        sb.append(footerMessage.ifEmpty { "Thank You Visit Again" }).append("\n")
        sb.append("---------------------------------------\n")
        return sb.toString()
    }
}
