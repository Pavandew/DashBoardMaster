package com.restroone.app.subscription.uistate

import com.restroone.app.subscription.models.BillingCycle
import com.restroone.app.subscription.models.SubscriptionPlan
import com.restroone.app.subscription.models.UserSubscriptionInfo

sealed class SubscriptionUiState {
    object Loading : SubscriptionUiState()
    
    data class Success(
        val plans: List<SubscriptionPlan>,
        val userSubscription: UserSubscriptionInfo,
        val selectedBillingCycle: BillingCycle = BillingCycle.YEARLY,
        val selectedPlan: SubscriptionPlan? = null
    ) : SubscriptionUiState()

    data class Error(val message: String) : SubscriptionUiState()
}
