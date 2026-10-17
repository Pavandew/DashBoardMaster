package com.restroone.app.manager_single_res_dash.registration_form_screen.model

import com.restroone.app.utils.AppConstants
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

    fun getCleaned(): OwnerProfile {
        val cleanDigits = ownerMobile.replace("[^0-9+]".toRegex(), "")
        val formattedMobile = if (cleanDigits.length == 10) "+91$cleanDigits" else if (cleanDigits.startsWith("91") && cleanDigits.length == 12) "+$cleanDigits" else cleanDigits.trim()
        return OwnerProfile(
            ownerFullName = ownerFullName.trim(),
            ownerEmail = ownerEmail.trim().lowercase(),
            ownerMobile = formattedMobile
        )
    }

    fun toMap(): Map<String, Any> {
        val clean = getCleaned()
        return mapOf(
            "ownerFullName" to clean.ownerFullName,
            "ownerEmail" to clean.ownerEmail,
            "ownerMobile" to clean.ownerMobile
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

    fun getCleaned(): RestaurantProfile {
        return RestaurantProfile(
            restaurantName = restaurantName.trim(),
            businessType = businessType.trim(),
            legalName = legalName.trim(),
            displayName = displayName.trim(),
            seatingCapacity = seatingCapacity.trim().ifEmpty { "—" },
            openDays = openDays.trim().ifEmpty { "7 / 7" },
            timezone = timezone.trim().ifEmpty { "Asia/Kolkata" }
        )
    }

    fun toMap(): Map<String, Any> {
        val clean = getCleaned()
        return mapOf(
            AppConstants.FIELD_RESTAURANT_NAME to clean.restaurantName,
            "businessType" to clean.businessType,
            "legalName" to clean.legalName,
            "displayName" to clean.displayName,
            "seatingCapacity" to clean.seatingCapacity,
            "openDays" to clean.openDays,
            "timezone" to clean.timezone
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

    fun getCleaned(): AddressInfo {
        val cleanDigits = contactNumber.replace("[^0-9+]".toRegex(), "")
        val formattedContact = if (cleanDigits.length == 10) "+91$cleanDigits" else if (cleanDigits.startsWith("91") && cleanDigits.length == 12) "+$cleanDigits" else cleanDigits.trim()

        val waDigits = whatsappNumber.replace("[^0-9+]".toRegex(), "")
        val formattedWhatsapp = if (waDigits.length == 10) "+91$waDigits" else if (waDigits.startsWith("91") && waDigits.length == 12) "+$waDigits" else waDigits.trim()

        return AddressInfo(
            address = address.trim(),
            landmark = landmark.trim(),
            pinCode = pinCode.trim(),
            city = city.trim(),
            state = state.trim(),
            country = country.trim().ifEmpty { "India" },
            contactNumber = formattedContact,
            contactEmail = contactEmail.trim().lowercase(),
            whatsappNumber = formattedWhatsapp,
            website = website.trim()
        )
    }

    fun toMap(): Map<String, Any> {
        val clean = getCleaned()
        return mapOf(
            "address" to clean.address,
            "landmark" to clean.landmark,
            "pinCode" to clean.pinCode,
            "city" to clean.city,
            "state" to clean.state,
            "country" to clean.country,
            "contactNumber" to clean.contactNumber,
            "contactEmail" to clean.contactEmail,
            "whatsappNumber" to clean.whatsappNumber,
            "website" to clean.website
        )
    }
}

@IgnoreExtraProperties
data class TaxSettings(
    @get:PropertyName("chargeTaxOnBills") @set:PropertyName("chargeTaxOnBills")
    var chargeTaxOnBills: Boolean = false,

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

    fun getCleaned(): TaxSettings {
        return TaxSettings(
            chargeTaxOnBills = chargeTaxOnBills,
            defaultTaxRate = defaultTaxRate,
            gstNumber = gstNumber.trim().uppercase(),
            panNumber = panNumber.trim().uppercase(),
            fssaiNumber = fssaiNumber.trim(),
            fssaiExpiryDate = fssaiExpiryDate.trim(),
            taxLabel = taxLabel.trim().ifEmpty { "GST" },
            priceIncludesTax = priceIncludesTax
        )
    }

    fun toMap(): Map<String, Any> {
        val clean = getCleaned()
        return mapOf(
            "chargeTaxOnBills" to clean.chargeTaxOnBills,
            "defaultTaxRate" to clean.defaultTaxRate,
            "gstNumber" to clean.gstNumber,
            "panNumber" to clean.panNumber,
            "fssaiNumber" to clean.fssaiNumber,
            "fssaiExpiryDate" to clean.fssaiExpiryDate,
            "taxLabel" to clean.taxLabel,
            "priceIncludesTax" to clean.priceIncludesTax
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
    var showLogoOnReceipts: Boolean = true,

    @get:PropertyName("customHeaderTagline") @set:PropertyName("customHeaderTagline")
    var customHeaderTagline: String = "",

    @get:PropertyName("customFooterMessage") @set:PropertyName("customFooterMessage")
    var customFooterMessage: String = "Thank You Visit Again",

    @get:PropertyName("showGstin") @set:PropertyName("showGstin")
    var showGstin: Boolean = true,

    @get:PropertyName("showFssai") @set:PropertyName("showFssai")
    var showFssai: Boolean = true,

    @get:PropertyName("showCustomerInfo") @set:PropertyName("showCustomerInfo")
    var showCustomerInfo: Boolean = true
) : Serializable {

    fun getCleaned(): BillingPrinterSettings {
        return BillingPrinterSettings(
            currency = currency.trim(),
            currencySymbol = currencySymbol.trim(),
            language = language.trim(),
            invoicePrefix = invoicePrefix.trim().ifEmpty { "INV-" },
            startingInvoiceNumber = startingInvoiceNumber.trim().ifEmpty { "1" },
            printSize = printSize.trim(),
            restaurantLogoUri = restaurantLogoUri?.trim(),
            showLogoOnReceipts = showLogoOnReceipts,
            customHeaderTagline = customHeaderTagline.trim(),
            customFooterMessage = customFooterMessage.trim(),
            showGstin = showGstin,
            showFssai = showFssai,
            showCustomerInfo = showCustomerInfo
        )
    }

    fun toMap(): Map<String, Any?> {
        val clean = getCleaned()
        return mapOf(
            "currency" to clean.currency,
            "currencySymbol" to clean.currencySymbol,
            "language" to clean.language,
            "invoicePrefix" to clean.invoicePrefix,
            "startingInvoiceNumber" to clean.startingInvoiceNumber,
            "printSize" to clean.printSize,
            "restaurantLogoUri" to clean.restaurantLogoUri,
            "showLogoOnReceipts" to clean.showLogoOnReceipts,
            "customHeaderTagline" to clean.customHeaderTagline,
            "customFooterMessage" to clean.customFooterMessage,
            "showGstin" to clean.showGstin,
            "showFssai" to clean.showFssai,
            "showCustomerInfo" to clean.showCustomerInfo
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

    fun getCleaned(): ServiceChargeSettings {
        return ServiceChargeSettings(
            enabled = enabled,
            percent = percent,
            label = label.trim().ifEmpty { "Service Charge" },
            dineInOnly = dineInOnly,
            applyTax = applyTax
        )
    }

    fun toMap(): Map<String, Any> {
        val clean = getCleaned()
        return mapOf(
            "enabled" to clean.enabled,
            "percent" to clean.percent,
            "label" to clean.label,
            "dineInOnly" to clean.dineInOnly,
            "applyTax" to clean.applyTax
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

    fun getCleaned(): UpiSettings {
        return UpiSettings(
            upiId = upiId.trim(),
            upiQrUrl = upiQrUrl.trim()
        )
    }

    fun toMap(): Map<String, Any> {
        val clean = getCleaned()
        return mapOf(
            "upiId" to clean.upiId,
            "upiQrUrl" to clean.upiQrUrl
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
            return ownerProfile.getCleaned()
        }
        return OwnerProfile(
            ownerFullName = getUnifiedFullName(),
            ownerEmail = getUnifiedEmail(),
            ownerMobile = getUnifiedMobile()
        ).getCleaned()
    }

    fun getEffectiveRestaurantProfile(): RestaurantProfile {
        if (restaurantProfile.restaurantName.isNotEmpty()) {
            return restaurantProfile.getCleaned()
        }
        return RestaurantProfile(
            restaurantName = restaurantName,
            businessType = businessType,
            legalName = legalName,
            displayName = displayName,
            seatingCapacity = seatingCapacity,
            openDays = openDays,
            timezone = timezone
        ).getCleaned()
    }

    fun getEffectiveAddressInfo(): AddressInfo {
        if (addressInfo.address.isNotEmpty() || addressInfo.city.isNotEmpty() || addressInfo.state.isNotEmpty()) {
            return addressInfo.getCleaned()
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
        ).getCleaned()
    }

    fun getEffectiveTaxSettings(): TaxSettings {
        if (taxSettings.gstNumber.isNotEmpty() || taxSettings.defaultTaxRate > 0) {
            return taxSettings.getCleaned()
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
        ).getCleaned()
    }

    fun getEffectiveBillingPrinterSettings(): BillingPrinterSettings {
        if (billingPrinterSettings.invoicePrefix.isNotEmpty()) {
            return billingPrinterSettings.getCleaned()
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
        ).getCleaned()
    }

    fun getUnifiedFullName(): String = ownerFullName.ifEmpty { fullName }.trim()
    fun getUnifiedEmail(): String = ownerEmail.ifEmpty { email }.trim().lowercase()
    fun getUnifiedMobile(): String {
        val raw = ownerMobile.ifEmpty { mobile.ifEmpty { phone } }
        val cleanDigits = raw.replace("[^0-9+]".toRegex(), "")
        return if (cleanDigits.length == 10) "+91$cleanDigits" else if (cleanDigits.startsWith("91") && cleanDigits.length == 12) "+$cleanDigits" else cleanDigits.trim()
    }

    fun toMap(): Map<String, Any?> {
        val effectiveOwner = getEffectiveOwnerProfile()
        val effectiveRestaurant = getEffectiveRestaurantProfile()
        val effectiveAddress = getEffectiveAddressInfo()
        val effectiveTax = getEffectiveTaxSettings()
        val effectiveBilling = getEffectiveBillingPrinterSettings()
        val effectiveService = serviceChargeSettings.getCleaned()
        val effectiveUpi = upiSettings.getCleaned()

        val cleanUid = ownerUid.trim()
        val cleanResId = if (restaurantId.trim().isNotEmpty()) restaurantId.trim() else cleanUid

        return mapOf(
            // Top-Level Restaurant Identifiers
            AppConstants.FIELD_RESTAURANT_NAME to effectiveRestaurant.restaurantName,
            "ownerName" to effectiveOwner.ownerFullName,
            AppConstants.FIELD_UID to cleanUid,
            "restaurantId" to cleanResId,
            "updatedAt" to System.currentTimeMillis(),

            // --- CLEAN GROUPED NESTED MAPS IN FIRESTORE ---
            "ownerProfile" to effectiveOwner.toMap(),
            "restaurantProfile" to effectiveRestaurant.toMap(),
            "addressInfo" to effectiveAddress.toMap(),
            "taxSettings" to effectiveTax.toMap(),
            "billingPrinterSettings" to effectiveBilling.toMap(),
            "serviceChargeSettings" to effectiveService.toMap(),
            "upiSettings" to effectiveUpi.toMap()
        )
    }
}
