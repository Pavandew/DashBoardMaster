package com.example.masterdashboard.manager_single_res_dash.form_screen.model

import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

@IgnoreExtraProperties
data class RegistrationDataModel(
    // Step 1: Owner & Restaurant
    @get:PropertyName("ownerFullName")
    @set:PropertyName("ownerFullName")
    var ownerFullName: String = "",

    @get:PropertyName("ownerEmail")
    @set:PropertyName("ownerEmail")
    var ownerEmail: String = "",

    @get:PropertyName("ownerMobile")
    @set:PropertyName("ownerMobile")
    var ownerMobile: String = "",

    // These match the common fields used in toMap() for backward/cross compatibility
    @get:PropertyName(AppConstants.FIELD_FULL_NAME)
    @set:PropertyName(AppConstants.FIELD_FULL_NAME)
    var fullName: String = "",

    @get:PropertyName(AppConstants.FIELD_EMAIL)
    @set:PropertyName(AppConstants.FIELD_EMAIL)
    var email: String = "",

    @get:PropertyName(AppConstants.FIELD_MOBILE)
    @set:PropertyName(AppConstants.FIELD_MOBILE)
    var mobile: String = "",

    @get:PropertyName("phone")
    @set:PropertyName("phone")
    var phone: String = "",

    @get:PropertyName(AppConstants.FIELD_RESTAURANT_NAME)
    @set:PropertyName(AppConstants.FIELD_RESTAURANT_NAME)
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
    @get:PropertyName(AppConstants.FIELD_UID)
    @set:PropertyName(AppConstants.FIELD_UID)
    var ownerUid: String = "",

    var restaurantId: String = ""
) : Serializable {

    /**
     * Unified getters to handle data regardless of which field name was used during save.
     */
    fun getUnifiedFullName(): String = ownerFullName.ifEmpty { fullName }
    fun getUnifiedEmail(): String = ownerEmail.ifEmpty { email }
    fun getUnifiedMobile(): String = ownerMobile.ifEmpty { mobile.ifEmpty { phone } }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            // Save both for safety/compatibility
            "ownerFullName" to ownerFullName.ifEmpty { fullName },
            "ownerEmail" to ownerEmail.ifEmpty { email },
            "ownerMobile" to ownerMobile.ifEmpty { mobile.ifEmpty { phone } },
            
            // Core unified fields
            AppConstants.FIELD_FULL_NAME to getUnifiedFullName(),
            AppConstants.FIELD_EMAIL to getUnifiedEmail(),
            AppConstants.FIELD_MOBILE to getUnifiedMobile(),
            AppConstants.FIELD_RESTAURANT_NAME to restaurantName,

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
            AppConstants.FIELD_UID to ownerUid,
            "updatedAt" to System.currentTimeMillis()
        )
    }
}
