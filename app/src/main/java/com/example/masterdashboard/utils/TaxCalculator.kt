package com.example.masterdashboard.utils

import java.util.Locale

/**
 * Data holder representing a complete tax breakdown for a bill or order.
 */
data class TaxBreakdown(
    val subtotal: Double = 0.0,
    val isTaxEnabled: Boolean = true,
    val taxRate: Double = 5.0,
    val taxLabel: String = "GST",
    val taxAmount: Double = 0.0,
    val halfTax: Double = 0.0,
    val halfRate: Double = 2.5,
    val grandTotal: Double = 0.0,
    val priceIncludesTax: Boolean = false
)

/**
 * Centralized utility class for all GST / Tax calculations across the app.
 * Follows Single Responsibility Principle (SRP) to eliminate duplicate tax logic.
 */
class TaxCalculator(private val sessionManager: SessionManager) {

    /**
     * Calculates tax, CGST/SGST 50-50 splits, and grand totals for a given subtotal.
     * Supports both tax-exclusive and tax-inclusive pricing rules.
     */
    fun calculateTax(
        subtotal: Double,
        serviceCharge: Double = 0.0,
        discount: Double = 0.0
    ): TaxBreakdown {
        val isTaxEnabled = sessionManager.isTaxEnabled()
        val taxRate = if (isTaxEnabled) sessionManager.getGstRate() else 0.0
        val taxLabel = sessionManager.getTaxLabel()
        val priceIncludesTax = sessionManager.isPriceIncludesTax()

        val taxableBase = Math.max(0.0, subtotal + serviceCharge - discount)

        val taxAmount = if (isTaxEnabled && taxRate > 0.0) {
            if (priceIncludesTax) {
                // Inclusive Tax formula: Tax = Base - (Base / (1 + Rate/100))
                taxableBase - (taxableBase / (1.0 + (taxRate / 100.0)))
            } else {
                // Exclusive Tax formula: Tax = Base * (Rate / 100)
                taxableBase * (taxRate / 100.0)
            }
        } else {
            0.0
        }

        val halfRate = taxRate / 2.0
        val halfTax = taxAmount / 2.0

        val grandTotal = if (priceIncludesTax) {
            taxableBase
        } else {
            taxableBase + taxAmount
        }

        return TaxBreakdown(
            subtotal = subtotal,
            isTaxEnabled = isTaxEnabled,
            taxRate = taxRate,
            taxLabel = taxLabel,
            taxAmount = taxAmount,
            halfTax = halfTax,
            halfRate = halfRate,
            grandTotal = grandTotal,
            priceIncludesTax = priceIncludesTax
        )
    }

    /**
     * Helper returning formatted State GST (SGST) label e.g., "State GST @ 2.5%" or "State GST @ 9.0%".
     */
    fun getSgstLabel(): String {
        val halfRate = if (sessionManager.isTaxEnabled()) sessionManager.getGstRate() / 2.0 else 0.0
        return String.format(Locale.US, "State GST @ %.1f%%", halfRate)
    }

    /**
     * Helper returning formatted Central GST (CGST) label e.g., "Central GST @ 2.5%" or "Central GST @ 9.0%".
     */
    fun getCgstLabel(): String {
        val halfRate = if (sessionManager.isTaxEnabled()) sessionManager.getGstRate() / 2.0 else 0.0
        return String.format(Locale.US, "Central GST @ %.1f%%", halfRate)
    }
}
