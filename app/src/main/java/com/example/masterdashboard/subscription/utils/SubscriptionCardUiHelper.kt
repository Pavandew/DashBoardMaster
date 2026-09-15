package com.example.masterdashboard.subscription.utils

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentSubscriptionPlansBinding
import com.example.masterdashboard.subscription.models.BillingCycle
import com.example.masterdashboard.subscription.models.SubscriptionPlan
import com.example.masterdashboard.subscription.models.SubscriptionStatus
import com.example.masterdashboard.subscription.models.UserSubscriptionInfo

/**
 * Production-level UI Helper & Delegate for Subscription Card Styling.
 * Uses color resources from colors.xml and string resources from strings.xml.
 */
class SubscriptionCardUiHelper(private val context: Context) {

    companion object {
        private const val TAG = "SubscriptionCardUiHelper"
    }

    // Color References from colors.xml
    private val colorGreenPrimary = ContextCompat.getColor(context, R.color.sub_green_primary)
    private val colorGreenLightBg = ContextCompat.getColor(context, R.color.sub_green_light_bg)

    private val colorPurplePrimary = ContextCompat.getColor(context, R.color.sub_purple_primary)
    private val colorPurpleLightBg = ContextCompat.getColor(context, R.color.sub_purple_light_bg)

    private val colorWhite = ContextCompat.getColor(context, R.color.white)
    private val colorGrayBorder = ContextCompat.getColor(context, R.color.sub_gray_border)
    private val colorDarkText = ContextCompat.getColor(context, R.color.sub_dark_text)
    private val colorRedExpired = ContextCompat.getColor(context, R.color.sub_red_expired)

    /**
     * Updates top status banner card based on user lifecycle stage (Trial, Active Pro, Expired).
     */
    fun renderStatusHeader(
        binding: FragmentSubscriptionPlansBinding,
        userSub: UserSubscriptionInfo
    ) {
        val formattedExpiry = userSub.getFormattedExpiryDate()
        val isProActive = userSub.isProActive()
        val isExpired = userSub.status == SubscriptionStatus.EXPIRED ||
                (userSub.status == SubscriptionStatus.TRIAL && userSub.trialDaysRemaining <= 0)

        Log.d(
            TAG,
            "renderStatusHeader: Status=${userSub.status}, isProActive=$isProActive, isExpired=$isExpired, Expiry='$formattedExpiry'"
        )

        when {
            isProActive -> {
                // Active PRO Member State
                binding.cardTrialStatus.visibility = View.VISIBLE
                binding.cardTrialStatus.strokeColor = colorPurplePrimary
                binding.cardTrialStatus.strokeWidth = dpToPx(1.5f)

                val planTitle = if (userSub.status == SubscriptionStatus.ACTIVE_YEARLY) {
                    context.getString(R.string.sub_yearly_owner_pass)
                } else {
                    context.getString(R.string.sub_monthly_owner_pass)
                }
                binding.tvTrialTitle.text = context.getString(R.string.sub_pro_member_format, planTitle)
                binding.tvTrialTitle.setTextColor(colorPurplePrimary)

                val expiryText = if (formattedExpiry.isNotEmpty()) {
                    context.getString(R.string.sub_valid_till_format, formattedExpiry)
                } else ""

                binding.tvTrialSubtitle.text = context.getString(
                    R.string.sub_active_expires_format,
                    userSub.trialDaysRemaining,
                    expiryText
                )
                binding.tvTrialSubtitle.setTextColor(colorDarkText)
            }
            isExpired -> {
                // Expired State
                binding.cardTrialStatus.visibility = View.VISIBLE
                binding.cardTrialStatus.strokeColor = colorRedExpired
                binding.cardTrialStatus.strokeWidth = dpToPx(1.5f)

                binding.tvTrialTitle.text = context.getString(R.string.sub_free_trial_expired_title)
                binding.tvTrialTitle.setTextColor(colorRedExpired)

                binding.tvTrialSubtitle.text = context.getString(R.string.sub_free_trial_expired_desc)
                binding.tvTrialSubtitle.setTextColor(colorDarkText)
            }
            userSub.status == SubscriptionStatus.TRIAL -> {
                // Active Trial State
                binding.cardTrialStatus.visibility = View.VISIBLE
                binding.cardTrialStatus.strokeColor = colorGreenPrimary
                binding.cardTrialStatus.strokeWidth = dpToPx(1f)

                binding.tvTrialTitle.text = context.getString(R.string.sub_free_trial_active_title)
                binding.tvTrialTitle.setTextColor(colorGreenPrimary)

                binding.tvTrialSubtitle.text = context.getString(
                    R.string.sub_free_trial_active_desc,
                    userSub.trialDaysRemaining
                )
                binding.tvTrialSubtitle.setTextColor(colorGreenPrimary)
            }
            else -> {
                binding.cardTrialStatus.visibility = View.GONE
            }
        }
    }

    /**
     * Renders cards styling, active plan badges, and button action text for selected plan.
     */
    fun updatePlanCardSelection(
        binding: FragmentSubscriptionPlansBinding,
        selectedCycle: BillingCycle,
        selectedPlan: SubscriptionPlan?,
        userSub: UserSubscriptionInfo
    ) {
        val isProActive = userSub.isProActive()
        val isExpired = userSub.status == SubscriptionStatus.EXPIRED ||
                (userSub.status == SubscriptionStatus.TRIAL && userSub.trialDaysRemaining <= 0)

        Log.d(
            TAG,
            "updatePlanCardSelection: Cycle=$selectedCycle, PlanID='${selectedPlan?.id}', isProActive=$isProActive, isExpired=$isExpired"
        )

        // 1. Manage Free Trial Card visibility and active badges
        if (isProActive) {
            // PRO Members do not need to see Free Trial card
            binding.cardTrialPlan.visibility = View.GONE
        } else {
            binding.cardTrialPlan.visibility = View.VISIBLE
            if (isExpired) {
                binding.cardTrialPlan.isEnabled = false
                binding.cardTrialPlan.isClickable = false
                binding.cardTrialPlan.alpha = 0.5f
                binding.tvBadgeTrial.text = context.getString(R.string.sub_badge_expired)
                binding.tvTitleTrial.text = context.getString(R.string.sub_free_trial_title_expired)
            } else {
                binding.cardTrialPlan.isEnabled = true
                binding.cardTrialPlan.isClickable = true
                binding.cardTrialPlan.alpha = 1.0f
                binding.tvBadgeTrial.text = context.getString(R.string.sub_badge_free)
                binding.tvTitleTrial.text = context.getString(R.string.sub_free_trial_title)
            }
        }

        // 2. Active Badges for Monthly & Yearly plans
        if (userSub.status == SubscriptionStatus.ACTIVE_YEARLY) {
            binding.tvBadgeYearly.text = context.getString(R.string.sub_badge_current_plan)
            binding.tvBadgeYearly.setTextColor(colorGreenPrimary)
            binding.tvBadgeYearly.setBackgroundResource(R.drawable.bg_status_active)
        } else {
            binding.tvBadgeYearly.text = context.getString(R.string.sub_badge_save_16)
            binding.tvBadgeYearly.setTextColor(colorPurplePrimary)
            binding.tvBadgeYearly.setBackgroundResource(R.drawable.bg_role_badge)
        }

        if (userSub.status == SubscriptionStatus.ACTIVE_MONTHLY) {
            binding.tvTitleMonthly.text = context.getString(R.string.sub_monthly_owner_pass_current)
        } else {
            binding.tvTitleMonthly.text = context.getString(R.string.sub_monthly_owner_pass)
        }

        // 3. Selection Styling based on Billing Cycle
        when (selectedCycle) {
            BillingCycle.TRIAL -> {
                // Free Trial Selected
                binding.cardTrialPlan.setCardBackgroundColor(colorGreenLightBg)
                binding.cardTrialPlan.strokeColor = colorGreenPrimary
                binding.cardTrialPlan.strokeWidth = dpToPx(2.5f)

                binding.cardYearlyPlan.setCardBackgroundColor(colorWhite)
                binding.cardYearlyPlan.strokeColor = colorGrayBorder
                binding.cardYearlyPlan.strokeWidth = dpToPx(1f)
                binding.tvPriceYearly.setTextColor(colorDarkText)

                binding.cardMonthlyPlan.setCardBackgroundColor(colorWhite)
                binding.cardMonthlyPlan.strokeColor = colorGrayBorder
                binding.cardMonthlyPlan.strokeWidth = dpToPx(1f)
                binding.tvPriceMonthly.setTextColor(colorDarkText)

                binding.btnSubscribe.text = context.getString(R.string.sub_btn_continue_trial)
                binding.btnSubscribe.backgroundTintList = ColorStateList.valueOf(colorGreenPrimary)
            }
            BillingCycle.YEARLY -> {
                // Yearly Selected
                binding.cardYearlyPlan.setCardBackgroundColor(colorPurpleLightBg)
                binding.cardYearlyPlan.strokeColor = colorPurplePrimary
                binding.cardYearlyPlan.strokeWidth = dpToPx(2.5f)
                binding.tvPriceYearly.setTextColor(colorPurplePrimary)

                binding.cardTrialPlan.setCardBackgroundColor(colorWhite)
                binding.cardTrialPlan.strokeColor = colorGrayBorder
                binding.cardTrialPlan.strokeWidth = dpToPx(1f)

                binding.cardMonthlyPlan.setCardBackgroundColor(colorWhite)
                binding.cardMonthlyPlan.strokeColor = colorGrayBorder
                binding.cardMonthlyPlan.strokeWidth = dpToPx(1f)
                binding.tvPriceMonthly.setTextColor(colorDarkText)

                val price = selectedPlan?.priceText ?: "₹2,999"
                val buttonText = when {
                    userSub.status == SubscriptionStatus.ACTIVE_YEARLY ->
                        context.getString(R.string.sub_btn_extend_yearly_format, price)
                    userSub.status == SubscriptionStatus.ACTIVE_MONTHLY ->
                        context.getString(R.string.sub_btn_upgrade_yearly_format, price)
                    else ->
                        context.getString(R.string.sub_btn_subscribe_yearly_format, price)
                }

                binding.btnSubscribe.text = buttonText
                binding.btnSubscribe.backgroundTintList = ColorStateList.valueOf(colorPurplePrimary)
            }
            BillingCycle.MONTHLY -> {
                // Monthly Selected
                binding.cardMonthlyPlan.setCardBackgroundColor(colorPurpleLightBg)
                binding.cardMonthlyPlan.strokeColor = colorPurplePrimary
                binding.cardMonthlyPlan.strokeWidth = dpToPx(2.5f)
                binding.tvPriceMonthly.setTextColor(colorPurplePrimary)

                binding.cardTrialPlan.setCardBackgroundColor(colorWhite)
                binding.cardTrialPlan.strokeColor = colorGrayBorder
                binding.cardTrialPlan.strokeWidth = dpToPx(1f)

                binding.cardYearlyPlan.setCardBackgroundColor(colorWhite)
                binding.cardYearlyPlan.strokeColor = colorGrayBorder
                binding.cardYearlyPlan.strokeWidth = dpToPx(1f)
                binding.tvPriceYearly.setTextColor(colorDarkText)

                val price = selectedPlan?.priceText ?: "₹299"
                val buttonText = if (userSub.status == SubscriptionStatus.ACTIVE_MONTHLY) {
                    context.getString(R.string.sub_btn_extend_monthly_format, price)
                } else {
                    context.getString(R.string.sub_btn_subscribe_monthly_format, price)
                }

                binding.btnSubscribe.text = buttonText
                binding.btnSubscribe.backgroundTintList = ColorStateList.valueOf(colorPurplePrimary)
            }
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
