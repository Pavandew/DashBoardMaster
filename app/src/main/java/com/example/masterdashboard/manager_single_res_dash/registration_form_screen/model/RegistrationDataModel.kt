package com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model

import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

@IgnoreExtraProperties
data class OwnerProfile(
    @get:PropertyName("ownerFullName") @set:PropertyName("ownerFullName")
    var ownerFullName: String = "",

    @get:PropertyName("ownerEmail") @set:PropertyName("ownerEmail")
    var ownerEmail: String = "",

    @get:PropertyName("ownerMobile") @set:PropertyName("ownerMobile")
    var ownerMobile: String = ""
) : Serializable {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "ownerFullName" to ownerFullName,
            "ownerEmail" to ownerEmail,
            "ownerMobile" to ownerMobile
        )
    }
}

@IgnoreExtraProperties
data class RestaurantProfile(
    @get:PropertyName(AppConstants.FIELD_RESTAURANT_NAME) @set:PropertyName(AppConstants.FIELD_RESTAURANT_NAME)
    var restaurantName: String = "",

    @get:PropertyName("businessType") @set:PropertyName("businessType")
    var businessType: String = "",

    @get:PropertyName("legalName") @set:PropertyName("legalName")
    var legalName: String = "",

    @get:PropertyName("displayName") @set:PropertyName("displayName")
    var displayName: String = "",

    @get:PropertyName("seatingCapacity") @set:PropertyName("seatingCapacity")
    var seatingCapacity: String = "—",

    @get:PropertyName("openDays") @set:PropertyName("openDays")
    var openDays: String = "7 / 7",

    @get:PropertyName("timezone") @set:PropertyName("timezone")
    var timezone: String = "Asia/Kolkata"
) : Serializable {
    fun toMap(): Map<String, Any> {
        return mapOf(
            AppConstants.FIELD_RESTAURANT_NAME to restaurantName,
            "businessType" to businessType,
            "legalName" to legalName,
            "displayName" to displayName,
            "seatingCapacity" to seatingCapacity,
            "openDays" to openDays,
            "timezone" to timezone
        )
    }
}

@IgnoreExtraProperties
data class AddressInfo(
    @get:PropertyName("address") @set:PropertyName("address")
    var address: String = "",

    @get:PropertyName("landmark") @set:PropertyName("landmark")
    var landmark: String = "",

    @get:PropertyName("pinCode") @set:PropertyName("pinCode")
    var pinCode: String = "",

    @get:PropertyName("city") @set:PropertyName("city")
    var city: String = "",

    @get:PropertyName("state") @set:PropertyName("state")
    var state: String = "",

    @get:PropertyName("country") @set:PropertyName("country")
    var country: String = "India",

    @get:PropertyName("contactNumber") @set:PropertyName("contactNumber")
    var contactNumber: String = "",

    @get:PropertyName("contactEmail") @set:PropertyName("contactEmail")
    var contactEmail: String = "",

    @get:PropertyName("whatsappNumber") @set:PropertyName("whatsappNumber")
    var whatsappNumber: String = "",

    @get:PropertyName("website") @set:PropertyName("website")
    var website: String = ""
) : Serializable {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "address" to address,
            "landmark" to landmark,
            "pinCode" to pinCode,
            "city" to city,
            "state" to state,
            "country" to country,
            "contactNumber" to contactNumber,
            "contactEmail" to contactEmail,
            "whatsappNumber" to whatsappNumber,
            "website" to website
        )
    }
}

@IgnoreExtraProperties
data class TaxSettings(
    @get:PropertyName("chargeTaxOnBills") @set:PropertyName("chargeTaxOnBills")
    var chargeTaxOnBills: Boolean = true,

    @get:PropertyName("defaultTaxRate") @set:PropertyName("defaultTaxRate")
    var defaultTaxRate: Double = 5.0,

    @get:PropertyName("gstNumber") @set:PropertyName("gstNumber")
    var gstNumber: String = "",

    @get:PropertyName("panNumber") @set:PropertyName("panNumber")
    var panNumber: String = "",

    @get:PropertyName("fssaiNumber") @set:PropertyName("fssaiNumber")
    var fssaiNumber: String = "",

    @get:PropertyName("fssaiExpiryDate") @set:PropertyName("fssaiExpiryDate")
    var fssaiExpiryDate: String = "",

    @get:PropertyName("taxLabel") @set:PropertyName("taxLabel")
    var taxLabel: String = "GST",

    @get:PropertyName("priceIncludesTax") @set:PropertyName("priceIncludesTax")
    var priceIncludesTax: Boolean = false
) : Serializable {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "chargeTaxOnBills" to chargeTaxOnBills,
            "defaultTaxRate" to defaultTaxRate,
            "gstNumber" to gstNumber,
            "panNumber" to panNumber,
            "fssaiNumber" to fssaiNumber,
            "fssaiExpiryDate" to fssaiExpiryDate,
            "taxLabel" to taxLabel,
            "priceIncludesTax" to priceIncludesTax
        )
    }
}

@IgnoreExtraProperties
data class BillingPrinterSettings(
    @get:PropertyName("currency") @set:PropertyName("currency")
    var currency: String = "₹ - Indian Rupee (INR)",

    @get:PropertyName("currencySymbol") @set:PropertyName("currencySymbol")
    var currencySymbol: String = "₹",

    @get:PropertyName("language") @set:PropertyName("language")
    var language: String = "English",

    @get:PropertyName("invoicePrefix") @set:PropertyName("invoicePrefix")
    var invoicePrefix: String = "INV-",

    @get:PropertyName("startingInvoiceNumber") @set:PropertyName("startingInvoiceNumber")
    var startingInvoiceNumber: String = "1",

    @get:PropertyName("printSize") @set:PropertyName("printSize")
    var printSize: String = "80 mm thermal",

    @get:PropertyName("restaurantLogoUri") @set:PropertyName("restaurantLogoUri")
    var restaurantLogoUri: String? = null,

    @get:PropertyName("showLogoOnReceipts") @set:PropertyName("showLogoOnReceipts")
    var showLogoOnReceipts: Boolean = true
) : Serializable {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "currency" to currency,
            "currencySymbol" to currencySymbol,
            "language" to language,
            "invoicePrefix" to invoicePrefix,
            "startingInvoiceNumber" to startingInvoiceNumber,
            "printSize" to printSize,
            "restaurantLogoUri" to restaurantLogoUri,
            "showLogoOnReceipts" to showLogoOnReceipts
        )
    }
}

@IgnoreExtraProperties
data class ServiceChargeSettings(
    @get:PropertyName("enabled") @set:PropertyName("enabled")
    var enabled: Boolean = true,

    @get:PropertyName("percent") @set:PropertyName("percent")
    var percent: Double = 5.0,

    @get:PropertyName("label") @set:PropertyName("label")
    var label: String = "Service Charge",

    @get:PropertyName("dineInOnly") @set:PropertyName("dineInOnly")
    var dineInOnly: Boolean = false,

    @get:PropertyName("applyTax") @set:PropertyName("applyTax")
    var applyTax: Boolean = false
) : Serializable {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "enabled" to enabled,
            "percent" to percent,
            "label" to label,
            "dineInOnly" to dineInOnly,
            "applyTax" to applyTax
        )
    }
}

@IgnoreExtraProperties
data class UpiSettings(
    @get:PropertyName("upiId") @set:PropertyName("upiId")
    var upiId: String = "",

    @get:PropertyName("upiQrUrl") @set:PropertyName("upiQrUrl")
    var upiQrUrl: String = ""
) : Serializable {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "upiId" to upiId,
            "upiQrUrl" to upiQrUrl
        )
    }
}

@IgnoreExtraProperties
data class RegistrationDataModel(
    // Step 1: Owner Profile & Restaurant
    @get:PropertyName("ownerFullName") @set:PropertyName("ownerFullName")
    var ownerFullName: String = "",

    @get:PropertyName("ownerEmail") @set:PropertyName("ownerEmail")
    var ownerEmail: String = "",

    @get:PropertyName("ownerMobile") @set:PropertyName("ownerMobile")
    var ownerMobile: String = "",

    @get:PropertyName(AppConstants.FIELD_FULL_NAME) @set:PropertyName(AppConstants.FIELD_FULL_NAME)
    var fullName: String = "",

    @get:PropertyName(AppConstants.FIELD_EMAIL) @set:PropertyName(AppConstants.FIELD_EMAIL)
    var email: String = "",

    @get:PropertyName(AppConstants.FIELD_MOBILE) @set:PropertyName(AppConstants.FIELD_MOBILE)
    var mobile: String = "",

    @get:PropertyName("phone") @set:PropertyName("phone")
    var phone: String = "",

    @get:PropertyName(AppConstants.FIELD_RESTAURANT_NAME) @set:PropertyName(AppConstants.FIELD_RESTAURANT_NAME)
    var restaurantName: String = "",

    var businessType: String = "",
    var legalName: String = "",
    var displayName: String = "",

    // Step 2: Address & Contact (Legacy Flat Fields)
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

    // Step 3: Tax & Compliance (Legacy Flat Fields)
    var gstNumber: String = "",
    var panNumber: String = "",
    var chargeTaxOnBills: Boolean = false,
    var defaultTaxRate: String = "",
    var priceIncludesTax: Boolean = false,
    var fssaiNumber: String = "",
    var fssaiExpiryDate: String = "",

    // Step 4: Billing & Print (Legacy Flat Fields)
    var currency: String = "₹ - Indian Rupee (INR)",
    var currencySymbol: String = "₹",
    var language: String = "English",
    var invoicePrefix: String = "INV-",
    var startingInvoiceNumber: String = "1",
    var printSize: String = "80 mm thermal",

    // Step 5: Branding
    var restaurantLogoUri: String? = null,
    var showLogoOnReceipts: Boolean = true,

    // Step 6: Operations
    var seatingCapacity: String = "—",
    var openDays: String = "7 / 7",
    var timezone: String = "Asia/Kolkata",

    // --- GROUPED NESTED MAPS ---
    var ownerProfile: OwnerProfile = OwnerProfile(),
    var restaurantProfile: RestaurantProfile = RestaurantProfile(),
    var addressInfo: AddressInfo = AddressInfo(),
    var taxSettings: TaxSettings = TaxSettings(),
    var billingPrinterSettings: BillingPrinterSettings = BillingPrinterSettings(),
    var serviceChargeSettings: ServiceChargeSettings = ServiceChargeSettings(),
    var upiSettings: UpiSettings = UpiSettings(),

    // Meta
    @get:PropertyName(AppConstants.FIELD_UID) @set:PropertyName(AppConstants.FIELD_UID)
    var ownerUid: String = "",

    var restaurantId: String = ""
) : Serializable {

    fun getEffectiveOwnerProfile(): OwnerProfile {
        if (ownerProfile.ownerFullName.isNotEmpty() || ownerProfile.ownerEmail.isNotEmpty()) {
            return ownerProfile
        }
        return OwnerProfile(
            ownerFullName = getUnifiedFullName(),
            ownerEmail = getUnifiedEmail(),
            ownerMobile = getUnifiedMobile()
        )
    }

    fun getEffectiveRestaurantProfile(): RestaurantProfile {
        if (restaurantProfile.restaurantName.isNotEmpty()) {
            return restaurantProfile
        }
        return RestaurantProfile(
            restaurantName = restaurantName,
            businessType = businessType,
            legalName = legalName,
            displayName = displayName,
            seatingCapacity = seatingCapacity,
            openDays = openDays,
            timezone = timezone
        )
    }

    fun getEffectiveAddressInfo(): AddressInfo {
        if (addressInfo.address.isNotEmpty() || addressInfo.city.isNotEmpty() || addressInfo.state.isNotEmpty()) {
            return addressInfo
        }
        return AddressInfo(
            address = address,
            landmark = landmark,
            pinCode = pinCode,
            city = city,
            state = state,
            country = country.ifEmpty { "India" },
            contactNumber = contactNumber,
            contactEmail = contactEmail,
            whatsappNumber = whatsappNumber,
            website = website
        )
    }

    fun getEffectiveTaxSettings(): TaxSettings {
        if (taxSettings.gstNumber.isNotEmpty() || taxSettings.defaultTaxRate > 0) {
            return taxSettings
        }
        val parsedRate = defaultTaxRate.toDoubleOrNull() ?: 5.0
        return TaxSettings(
            chargeTaxOnBills = chargeTaxOnBills,
            defaultTaxRate = parsedRate,
            gstNumber = gstNumber,
            panNumber = panNumber,
            fssaiNumber = fssaiNumber,
            fssaiExpiryDate = fssaiExpiryDate,
            taxLabel = "GST",
            priceIncludesTax = priceIncludesTax
        )
    }

    fun getEffectiveBillingPrinterSettings(): BillingPrinterSettings {
        if (billingPrinterSettings.invoicePrefix.isNotEmpty()) {
            return billingPrinterSettings
        }
        return BillingPrinterSettings(
            currency = currency,
            currencySymbol = currencySymbol,
            language = language,
            invoicePrefix = invoicePrefix,
            startingInvoiceNumber = startingInvoiceNumber,
            printSize = printSize,
            restaurantLogoUri = restaurantLogoUri,
            showLogoOnReceipts = showLogoOnReceipts
        )
    }

    fun getUnifiedFullName(): String = ownerFullName.ifEmpty { fullName }
    fun getUnifiedEmail(): String = ownerEmail.ifEmpty { email }
    fun getUnifiedMobile(): String = ownerMobile.ifEmpty { mobile.ifEmpty { phone } }

    fun toMap(): Map<String, Any?> {
        val effectiveOwner = getEffectiveOwnerProfile()
        val effectiveRestaurant = getEffectiveRestaurantProfile()
        val effectiveAddress = getEffectiveAddressInfo()
        val effectiveTax = getEffectiveTaxSettings()
        val effectiveBilling = getEffectiveBillingPrinterSettings()

        return mapOf(
            // Flat Root Fields (for backward compatibility)
            "ownerFullName" to effectiveOwner.ownerFullName,
            "ownerEmail" to effectiveOwner.ownerEmail,
            "ownerMobile" to effectiveOwner.ownerMobile,
            
            AppConstants.FIELD_FULL_NAME to effectiveOwner.ownerFullName,
            AppConstants.FIELD_EMAIL to effectiveOwner.ownerEmail,
            AppConstants.FIELD_MOBILE to effectiveOwner.ownerMobile,
            AppConstants.FIELD_RESTAURANT_NAME to effectiveRestaurant.restaurantName,

            "businessType" to effectiveRestaurant.businessType,
            "legalName" to effectiveRestaurant.legalName,
            "displayName" to effectiveRestaurant.displayName,

            "address" to effectiveAddress.address,
            "landmark" to effectiveAddress.landmark,
            "pinCode" to effectiveAddress.pinCode,
            "city" to effectiveAddress.city,
            "state" to effectiveAddress.state,
            "country" to effectiveAddress.country,
            "contactNumber" to effectiveAddress.contactNumber,
            "contactEmail" to effectiveAddress.contactEmail,
            "whatsappNumber" to effectiveAddress.whatsappNumber,
            "website" to effectiveAddress.website,

            "gstNumber" to effectiveTax.gstNumber,
            "panNumber" to effectiveTax.panNumber,
            "chargeTaxOnBills" to effectiveTax.chargeTaxOnBills,
            "defaultTaxRate" to effectiveTax.defaultTaxRate.toString(),
            "priceIncludesTax" to effectiveTax.priceIncludesTax,
            "fssaiNumber" to effectiveTax.fssaiNumber,
            "fssaiExpiryDate" to effectiveTax.fssaiExpiryDate,

            "currency" to effectiveBilling.currency,
            "currencySymbol" to effectiveBilling.currencySymbol,
            "language" to effectiveBilling.language,
            "invoicePrefix" to effectiveBilling.invoicePrefix,
            "startingInvoiceNumber" to effectiveBilling.startingInvoiceNumber,
            "printSize" to effectiveBilling.printSize,
            "restaurantLogoUri" to effectiveBilling.restaurantLogoUri,
            "showLogoOnReceipts" to effectiveBilling.showLogoOnReceipts,

            "seatingCapacity" to effectiveRestaurant.seatingCapacity,
            "openDays" to effectiveRestaurant.openDays,
            "timezone" to effectiveRestaurant.timezone,

            // --- GROUPED NESTED MAPS IN FIRESTORE ---
            "ownerProfile" to effectiveOwner.toMap(),
            "restaurantProfile" to effectiveRestaurant.toMap(),
            "addressInfo" to effectiveAddress.toMap(),
            "taxSettings" to effectiveTax.toMap(),
            "billingPrinterSettings" to effectiveBilling.toMap(),
            "serviceChargeSettings" to serviceChargeSettings.toMap(),
            "upiSettings" to upiSettings.toMap(),

            AppConstants.FIELD_UID to ownerUid,
            "updatedAt" to System.currentTimeMillis()
        )
    }
}
