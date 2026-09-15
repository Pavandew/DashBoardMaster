package com.example.masterdashboard.subscription.uistate

import com.example.masterdashboard.subscription.models.BillingCycle
import com.example.masterdashboard.subscription.models.SubscriptionPlan
import com.example.masterdashboard.subscription.models.UserSubscriptionInfo

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
