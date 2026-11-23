package com.restroone.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.restroone.app.R

/**
 * Utility for opening legal, support, and account deletion links inside
 * Chrome Custom Tabs with fallback to system web browsers.
 */
object LegalNavigationUtils {

    private const val TAG = "LegalNavigationUtils"

    const val URL_TERMS = "https://restroone.co.in/terms-and-conditions"
    const val URL_PRIVACY = "https://restroone.co.in/privacy-policy"
    const val URL_SUPPORT = "https://restroone.co.in/support"
    const val URL_DELETE_ACCOUNT = "https://restroone.co.in/delete-account"

    fun openWebPage(context: Context, url: String) {
        try {
            val primaryColor = ContextCompat.getColor(context, R.color.mid_app_color)
            val customTabsIntent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(primaryColor)
                        .build()
                )
                .setShowTitle(true)
                .build()

            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            Log.e(TAG, "Custom Tabs launch failed for url: $url", e)
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "External browser launch failed for url: $url", ex)
                Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openTermsAndConditions(context: Context) {
        openWebPage(context, URL_TERMS)
    }

    fun openPrivacyPolicy(context: Context) {
        openWebPage(context, URL_PRIVACY)
    }

    fun openHelpSupport(context: Context) {
        openWebPage(context, URL_SUPPORT)
    }

    fun openAccountDeletion(context: Context) {
        openWebPage(context, URL_DELETE_ACCOUNT)
    }
}
