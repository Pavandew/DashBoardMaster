package com.example.masterdashboard.subscription.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.subscription.models.BillingCycle
import com.example.masterdashboard.subscription.models.PaymentGateway
import com.example.masterdashboard.subscription.models.PaymentResultPayload
import com.example.masterdashboard.subscription.models.PaymentState
import com.example.masterdashboard.subscription.models.SubscriptionPlan
import com.example.masterdashboard.subscription.models.SubscriptionStatus
import com.example.masterdashboard.subscription.models.UserSubscriptionInfo
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

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized. Emitting instant local plan state...")
        val defaultPlans = repository.getAvailablePlans()
        val defaultTrialInfo = UserSubscriptionInfo()
        val initialPlan = defaultPlans.find { it.billingCycle == BillingCycle.TRIAL } ?: defaultPlans.firstOrNull()

        _uiState.value = SubscriptionUiState.Success(
            plans = defaultPlans,
            userSubscription = defaultTrialInfo,
            selectedBillingCycle = BillingCycle.TRIAL,
            selectedPlan = initialPlan
        )

        loadSubscriptionData()
    }

    fun loadSubscriptionData() {
        viewModelScope.launch {
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

    fun initiatePayment(gateway: PaymentGateway) {
        val currentState = _uiState.value
        if (currentState !is SubscriptionUiState.Success) {
            Log.w(TAG, "initiatePayment: Cannot initiate payment, UI state is not Success")
            return
        }

        val plan = currentState.selectedPlan ?: currentState.plans.firstOrNull()
        if (plan == null) {
            Log.e(TAG, "initiatePayment: No plan selected")
            _paymentState.value = PaymentState.Error("Please select a valid subscription plan")
            return
        }

        viewModelScope.launch {
            _paymentState.value = PaymentState.InitiatingOrder
            try {
                val orderDetails = repository.createPaymentOrder(plan, gateway)
                Log.i(TAG, "initiatePayment: Created order '${orderDetails.orderId}' for Gateway ${gateway.name}")
                _paymentState.value = PaymentState.AwaitingSdk(orderDetails)
            } catch (e: Exception) {
                Log.e(TAG, "initiatePayment: Failed to create order", e)
                _paymentState.value = PaymentState.Error(e.message ?: "Failed to initiate payment order")
            }
        }
    }

    fun processPaymentResult(payload: PaymentResultPayload) {
        val currentState = _uiState.value
        if (currentState !is SubscriptionUiState.Success) {
            Log.w(TAG, "processPaymentResult: Cannot process payment, UI state is not Success")
            return
        }

        val plan = currentState.selectedPlan ?: currentState.plans.firstOrNull()
        if (plan == null) {
            Log.e(TAG, "processPaymentResult: No plan selected")
            _paymentState.value = PaymentState.Error("Invalid plan selected for verification")
            return
        }

        viewModelScope.launch {
            _paymentState.value = PaymentState.VerifyingPayment
            try {
                Log.i(TAG, "processPaymentResult: Verifying payment '${payload.paymentId}' via Gateway ${payload.gateway.name}...")
                val success = repository.verifyAndActivateSubscription(plan, payload)
                if (success) {
                    Log.i(TAG, "processPaymentResult: Payment verified and subscription activated successfully!")
                    _paymentState.value = PaymentState.Success(
                        message = "🎉 🎉 Congratulations! Your ${plan.title} is now active!",
                        subscriptionId = payload.paymentId
                    )
                    loadSubscriptionData() // Refresh status from Firestore
                } else {
                    Log.e(TAG, "processPaymentResult: Failed to activate subscription in Firestore")
                    _paymentState.value = PaymentState.Error("Payment was successful but activation failed. Please contact support.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "processPaymentResult: Exception during payment verification", e)
                _paymentState.value = PaymentState.Error(e.message ?: "Error verifying payment")
            }
        }
    }

    fun resetPaymentState() {
        Log.d(TAG, "resetPaymentState: Resetting payment state to Idle")
        _paymentState.value = PaymentState.Idle
    }
}
