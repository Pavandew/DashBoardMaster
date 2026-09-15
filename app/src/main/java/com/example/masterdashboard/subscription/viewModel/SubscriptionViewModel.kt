package com.example.masterdashboard.subscription.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.subscription.models.BillingCycle
import com.example.masterdashboard.subscription.models.SubscriptionPlan
import com.example.masterdashboard.subscription.models.SubscriptionStatus
import com.example.masterdashboard.subscription.repo.SubscriptionRepository
import com.example.masterdashboard.subscription.uistate.SubscriptionUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SubscriptionViewModel"
    }

    private val repository = SubscriptionRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Loading)
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized. Loading subscription data...")
        loadSubscriptionData()
    }

    fun loadSubscriptionData() {
        viewModelScope.launch {
            Log.d(TAG, "loadSubscriptionData: Setting state to Loading")
            _uiState.value = SubscriptionUiState.Loading
            try {
                val plans = repository.getAvailablePlans()
                val userSub = repository.getUserSubscriptionInfo()

                val defaultCycle = when (userSub.status) {
                    SubscriptionStatus.ACTIVE_MONTHLY -> BillingCycle.MONTHLY
                    SubscriptionStatus.ACTIVE_YEARLY -> BillingCycle.YEARLY
                    SubscriptionStatus.TRIAL -> {
                        if (userSub.trialDaysRemaining > 0) BillingCycle.TRIAL else BillingCycle.YEARLY
                    }
                    else -> BillingCycle.YEARLY
                }
                val defaultSelectedPlan = plans.find { it.billingCycle == defaultCycle } ?: plans.firstOrNull()

                Log.i(
                    TAG,
                    "loadSubscriptionData: Success. PlansCount=${plans.size}, UserStatus=${userSub.status}, DaysLeft=${userSub.trialDaysRemaining}, DefaultCycle=$defaultCycle, DefaultPlan='${defaultSelectedPlan?.title}'"
                )

                _uiState.value = SubscriptionUiState.Success(
                    plans = plans,
                    userSubscription = userSub,
                    selectedBillingCycle = defaultCycle,
                    selectedPlan = defaultSelectedPlan
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadSubscriptionData: Failed to load subscription plans", e)
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Failed to load subscription plans")
            }
        }
    }

    fun selectBillingCycle(cycle: BillingCycle) {
        val currentState = _uiState.value
        Log.d(TAG, "selectBillingCycle: Requested cycle=$cycle, CurrentState=${currentState::class.java.simpleName}")

        if (currentState is SubscriptionUiState.Success) {
            val matchingPlan = currentState.plans.find { it.billingCycle == cycle } ?: currentState.selectedPlan
            Log.i(TAG, "selectBillingCycle: Switched cycle to $cycle. Selected plan='${matchingPlan?.title}' (${matchingPlan?.priceText})")

            _uiState.value = currentState.copy(
                selectedBillingCycle = cycle,
                selectedPlan = matchingPlan
            )
        }
    }

    fun selectPlan(plan: SubscriptionPlan) {
        val currentState = _uiState.value
        Log.d(TAG, "selectPlan: Selected plan ID='${plan.id}', Title='${plan.title}', Price='${plan.priceText}'")

        if (currentState is SubscriptionUiState.Success) {
            _uiState.value = currentState.copy(
                selectedPlan = plan,
                selectedBillingCycle = plan.billingCycle
            )
        }
    }
}
