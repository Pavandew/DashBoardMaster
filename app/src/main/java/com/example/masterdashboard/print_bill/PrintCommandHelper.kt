package com.example.masterdashboard.print_bill

import android.util.Log
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Helper class to generate ESC/POS byte commands for thermal printers.
 * This class handles formatting the text, alignment, and styling.
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
        
        const val LINE_WIDTH = 32 // Standard for 58mm printers
    }

    /**
     * Generates a byte array for a complete bill based on the order data.
     */
    fun generateBillBytes(restaurantName: String, order: CashierBillingOrderModel): ByteArray {
        val bytes = mutableListOf<Byte>()

        Log.d(TAG, "Generating print bytes for Order: ${order.orderId}")

        // 1. Header (Restaurant Name)
        bytes.addAll(ALIGN_CENTER.toList())
        bytes.addAll(TEXT_SIZE_LARGE.toList())
        bytes.addAll(BOLD_ON.toList())
        bytes.addAll(restaurantName.toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        
        // 2. Subheader (Order Info)
        bytes.addAll(TEXT_SIZE_NORMAL.toList())
        bytes.addAll(BOLD_OFF.toList())
        bytes.addAll("Order: #${order.orderId.takeLast(6)}".toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll("Table: ${order.tableName}".toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(order.timestamp.toDate())
        bytes.addAll("Date: $dateStr".toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        
        bytes.addAll(createSeparator().toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // 3. Table Header
        bytes.addAll(ALIGN_LEFT.toList())
        bytes.addAll(formatLine("Item", "Qty", "Total").toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(createSeparator('-').toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // 4. Items List
        order.items.forEach { item ->
            val name = if (item.variantName.isNotEmpty()) "${item.itemName} (${item.variantName})" else item.itemName
            
            // If name is too long, it might wrap. For now, let's just print it.
            bytes.addAll(formatLine(name, "x${item.quantity}", "₹${item.rowTotal}").toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }

        bytes.addAll(createSeparator('-').toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // 5. Totals
        bytes.addAll(ALIGN_RIGHT.toList())
        bytes.addAll("Subtotal: ₹${order.subtotal}".toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll("Tax: ₹${order.taxAmount}".toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        
        if (order.discountAmount > 0) {
            bytes.addAll("Discount: -₹${order.discountAmount}".toByteArray().toList())
            bytes.addAll(FEED_LINE.toList())
        }
        
        bytes.addAll(BOLD_ON.toList())
        bytes.addAll("GRAND TOTAL: ₹${order.grandTotal}".toByteArray().toList())
        bytes.addAll(BOLD_OFF.toList())
        bytes.addAll(FEED_LINE.toList())
        
        bytes.addAll(createSeparator().toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())

        // 6. Footer
        bytes.addAll(ALIGN_CENTER.toList())
        bytes.addAll("Thank you! Visit Again".toByteArray().toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(FEED_LINE.toList())
        bytes.addAll(FEED_LINE.toList()) // Extra space before cutting
        bytes.addAll(PAPER_CUT.toList())

        Log.i(TAG, "Print bytes generated. Total size: ${bytes.size} bytes")
        return bytes.toByteArray()
    }

    /**
     * Helper to create a line with left, center (optional), and right aligned text.
     */
    private fun formatLine(left: String, center: String, right: String): String {
        val totalWidth = LINE_WIDTH
        val leftPart = if (left.length > 15) left.substring(0, 12) + ".." else left
        val rightPart = right
        
        val spaceNeeded = totalWidth - leftPart.length - rightPart.length - center.length
        val spaceBetweenLeftAndCenter = spaceNeeded / 2
        val spaceBetweenCenterAndRight = spaceNeeded - spaceBetweenLeftAndCenter
        
        val sb = StringBuilder()
        sb.append(leftPart)
        repeat(maxOf(1, spaceBetweenLeftAndCenter)) { sb.append(" ") }
        sb.append(center)
        repeat(maxOf(1, spaceBetweenCenterAndRight)) { sb.append(" ") }
        sb.append(rightPart)
        
        return sb.toString()
    }

    private fun createSeparator(char: Char = '='): String {
        return char.toString().repeat(LINE_WIDTH)
    }
    
    /**
     * Generates a plain text version for debugging in Logcat.
     */
    fun getLoggablePreview(restaurantName: String, order: CashierBillingOrderModel): String {
        val sb = StringBuilder()
        sb.append("\n----------- PRINTER PREVIEW -----------\n")
        sb.append(restaurantName.uppercase().padStart((LINE_WIDTH + restaurantName.length) / 2)).append("\n")
        sb.append("Order: #${order.orderId.takeLast(6)}".padStart((LINE_WIDTH + 14) / 2)).append("\n")
        sb.append("Table: ${order.tableName}".padStart((LINE_WIDTH + 7 + order.tableName.length) / 2)).append("\n")
        sb.append("=".repeat(LINE_WIDTH)).append("\n")
        sb.append(formatLine("Item", "Qty", "Total")).append("\n")
        sb.append("-".repeat(LINE_WIDTH)).append("\n")
        order.items.forEach { 
            sb.append(formatLine(it.itemName, "x${it.quantity}", "₹${it.rowTotal}")).append("\n")
        }
        sb.append("-".repeat(LINE_WIDTH)).append("\n")
        sb.append("GRAND TOTAL: ₹${order.grandTotal}".padStart(LINE_WIDTH)).append("\n")
        sb.append("---------------------------------------\n")
        return sb.toString()
    }
}
