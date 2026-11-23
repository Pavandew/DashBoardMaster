package com.restroone.app.subscription.utils

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.restroone.app.databinding.LayoutSubscriptionExpiryBannerBinding
import com.restroone.app.subscription.models.SubscriptionStatus
import com.restroone.app.subscription.repo.SubscriptionRepository
import kotlinx.coroutines.launch

/**
 * Reusable Helper for checking subscription trial expiration and binding
 * the Expiry Warning Banner UI across activities and fragments.
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

                val isExpired = userSub.status == SubscriptionStatus.EXPIRED ||
                        (userSub.status == SubscriptionStatus.TRIAL && userSub.trialDaysRemaining <= 0)

                Log.d(TAG, "checkAndBindExpiryBanner: status=${userSub.status}, daysLeft=${userSub.trialDaysRemaining}, isExpired=$isExpired")

                if (isExpired) {
                    bannerBinding.cardSubscriptionExpiryBanner.visibility = View.VISIBLE
                    bannerBinding.btnRenewSubscriptionNow.setOnClickListener {
                        onUpgradeClicked()
                    }
                } else {
                    bannerBinding.cardSubscriptionExpiryBanner.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check subscription status for banner", e)
            }
        }
    }
}
