package com.restroone.app.subscription.utils

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.restroone.app.R
import com.restroone.app.databinding.LayoutSubscriptionExpiryBannerBinding
import com.restroone.app.subscription.models.SubscriptionStatus
import com.restroone.app.subscription.repo.SubscriptionRepository
import kotlinx.coroutines.launch

/**
 * Reusable Helper for checking subscription trial/active expiration and binding
 * the 3-Tier Reminder Banner UI across activities and fragments.
 */
object SubscriptionExpiryUiHelper {

    private const val TAG = "SubExpiryUiHelper"

    fun checkAndBindExpiryBanner(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        bannerBinding: LayoutSubscriptionExpiryBannerBinding,
        onUpgradeClicked: () -> Unit
    ) {
        lifecycleOwner.lifecycleScope.launch {
            try {
                val repository = SubscriptionRepository(context)
                val userSub = repository.getUserSubscriptionInfo()

                val now = System.currentTimeMillis()
                val diffMillis = userSub.expiryDateTimestamp - now
                val calculatedDaysLeft = if (diffMillis <= 0) 0 else (diffMillis / (1000L * 60 * 60 * 24)).toInt()

                val daysRemaining = if (userSub.status == SubscriptionStatus.TRIAL) {
                    kotlin.math.min(userSub.trialDaysRemaining, calculatedDaysLeft)
                } else {
                    calculatedDaysLeft
                }

                val isExpired = userSub.status == SubscriptionStatus.EXPIRED || daysRemaining <= 0

                Log.d(TAG, "checkAndBindExpiryBanner: status=${userSub.status}, daysRemaining=$daysRemaining, isExpired=$isExpired")

                when {
                    isExpired -> {
                        // TIER 3: EXPIRED (Dark Red Banner)
                        bannerBinding.cardSubscriptionExpiryBanner.visibility = View.VISIBLE
                        bannerBinding.cardSubscriptionExpiryBanner.setCardBackgroundColor(Color.parseColor("#2A1515"))
                        bannerBinding.cardSubscriptionExpiryBanner.strokeColor = Color.parseColor("#EF4444")
                        bannerBinding.tvBannerTitle.text = context.getString(R.string.sub_reminder_expired_title)
                        bannerBinding.tvBannerTitle.setTextColor(Color.parseColor("#EF4444"))
                        bannerBinding.tvBannerDesc.text = context.getString(R.string.sub_reminder_expired_desc)
                        bannerBinding.btnRenewSubscriptionNow.text = context.getString(R.string.sub_reminder_btn_upgrade)
                        bannerBinding.btnRenewSubscriptionNow.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#DC2626"))
                    }

                    daysRemaining == 1 -> {
                        // TIER 2: URGENT LAST DAY (Crimson Red Banner)
                        bannerBinding.cardSubscriptionExpiryBanner.visibility = View.VISIBLE
                        bannerBinding.cardSubscriptionExpiryBanner.setCardBackgroundColor(Color.parseColor("#381515"))
                        bannerBinding.cardSubscriptionExpiryBanner.strokeColor = Color.parseColor("#EF4444")
                        bannerBinding.tvBannerTitle.text = context.getString(R.string.sub_reminder_urgent_title)
                        bannerBinding.tvBannerTitle.setTextColor(Color.parseColor("#F87171"))
                        bannerBinding.tvBannerDesc.text = context.getString(R.string.sub_reminder_urgent_desc)
                        bannerBinding.btnRenewSubscriptionNow.text = context.getString(R.string.sub_reminder_btn_renew_now)
                        bannerBinding.btnRenewSubscriptionNow.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#DC2626"))
                    }

                    daysRemaining in 2..5 -> {
                        // TIER 1: WARNING 2-5 DAYS LEFT (Amber / Dark Gold Banner)
                        bannerBinding.cardSubscriptionExpiryBanner.visibility = View.VISIBLE
                        bannerBinding.cardSubscriptionExpiryBanner.setCardBackgroundColor(Color.parseColor("#2A2110"))
                        bannerBinding.cardSubscriptionExpiryBanner.strokeColor = Color.parseColor("#F59E0B")
                        bannerBinding.tvBannerTitle.text = context.getString(R.string.sub_reminder_warning_title, daysRemaining)
                        bannerBinding.tvBannerTitle.setTextColor(Color.parseColor("#FBBF24"))
                        bannerBinding.tvBannerDesc.text = context.getString(R.string.sub_reminder_warning_desc, daysRemaining)
                        bannerBinding.btnRenewSubscriptionNow.text = context.getString(R.string.sub_reminder_btn_renew)
                        bannerBinding.btnRenewSubscriptionNow.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#D97706"))
                    }

                    else -> {
                        // HEALTHY > 5 DAYS LEFT (Hidden)
                        bannerBinding.cardSubscriptionExpiryBanner.visibility = View.GONE
                    }
                }

                bannerBinding.btnRenewSubscriptionNow.setOnClickListener {
                    onUpgradeClicked()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to check subscription status for banner", e)
            }
        }
    }
}
