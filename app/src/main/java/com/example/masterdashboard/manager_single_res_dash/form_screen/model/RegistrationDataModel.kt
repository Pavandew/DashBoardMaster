package com.example.masterdashboard.manager_single_res_dash.form_screen.model

data class RegistrationDataModel(
    // Step 1: Owner & Restaurant
    var ownerFullName: String = "",
    var ownerEmail: String = "",
    var ownerMobile: String = "",
    var restaurantName: String = "",
    var businessType: String = "",
    var legalName: String = "",
    var displayName: String = "",

    // Step 2: Address & Contact
    var address: String = "",
    var landmark: String = "",
    var pinCode: String = "",
    var city: String = "",
    var state: String = "",
    var country: String = "India",
    var contactNumber: String = "",
    var contactEmail: String = "",
    var whatsappNumber: String = "",
    var website: String = "",

    // Step 3: Tax & Compliance
    var gstNumber: String = "",
    var panNumber: String = "",
    var chargeTaxOnBills: Boolean = false,
    var defaultTaxRate: String = "",
    var priceIncludesTax: Boolean = false,
    var fssaiNumber: String = "",
    var fssaiExpiryDate: String = "",

    // Step 4: Billing
    var currency: String = "₹ - Indian Rupee (INR)",
    var currencySymbol: String = "₹",
    var language: String = "English",
    var invoicePrefix: String = "INV-",
    var startingInvoiceNumber: String = "1",
    var printSize: String = "80 mm thermal",

    // Step 5: Branding
    var restaurantLogoUri: String? = null,
    var showLogoOnReceipts: Boolean = true,

    // Step 6: Operations (Optional/Future)
    var seatingCapacity: String = "—",
    var openDays: String = "7 / 7",
    var timezone: String = "Asia/Kolkata",

    // Meta
    var ownerUid: String = "",
    var restaurantId: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "ownerFullName" to ownerFullName,
            "ownerEmail" to ownerEmail,
            "ownerMobile" to ownerMobile,
            "restaurantName" to restaurantName,
            "businessType" to businessType,
            "legalName" to legalName,
            "displayName" to displayName,
            "address" to address,
            "landmark" to landmark,
            "pinCode" to pinCode,
            "city" to city,
            "state" to state,
            "country" to country,
            "contactNumber" to contactNumber,
            "contactEmail" to contactEmail,
            "whatsappNumber" to whatsappNumber,
            "website" to website,
            "gstNumber" to gstNumber,
            "panNumber" to panNumber,
            "chargeTaxOnBills" to chargeTaxOnBills,
            "defaultTaxRate" to defaultTaxRate,
            "priceIncludesTax" to priceIncludesTax,
            "fssaiNumber" to fssaiNumber,
            "fssaiExpiryDate" to fssaiExpiryDate,
            "currency" to currency,
            "currencySymbol" to currencySymbol,
            "language" to language,
            "invoicePrefix" to invoicePrefix,
            "startingInvoiceNumber" to startingInvoiceNumber,
            "printSize" to printSize,
            "restaurantLogoUri" to restaurantLogoUri,
            "showLogoOnReceipts" to showLogoOnReceipts,
            "seatingCapacity" to seatingCapacity,
            "openDays" to openDays,
            "timezone" to timezone,
            "ownerUid" to ownerUid,
            "updatedAt" to System.currentTimeMillis()
        )
    }
}
