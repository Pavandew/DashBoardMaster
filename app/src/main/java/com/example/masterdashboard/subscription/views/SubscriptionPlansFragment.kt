package com.example.masterdashboard.subscription.views

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.databinding.FragmentSubscriptionPlansBinding
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.subscription.adapter.SubscriptionFeatureAdapter
import com.example.masterdashboard.subscription.models.BillingCycle
import com.example.masterdashboard.subscription.models.PaymentGateway
import com.example.masterdashboard.subscription.models.PaymentOrderDetails
import com.example.masterdashboard.subscription.models.PaymentResultPayload
import com.example.masterdashboard.subscription.models.PaymentState
import com.example.masterdashboard.subscription.models.SubscriptionPlan
import com.example.masterdashboard.subscription.models.SubscriptionStatus
import com.example.masterdashboard.subscription.payment.PaymentGatewayListener
import com.example.masterdashboard.subscription.payment.PhonePePaymentHandler
import com.example.masterdashboard.subscription.payment.RazorpayPaymentHandler
import com.example.masterdashboard.subscription.uistate.SubscriptionUiState
import com.example.masterdashboard.subscription.utils.SubscriptionCardUiHelper
import com.example.masterdashboard.subscription.viewModel.SubscriptionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SubscriptionPlansFragment : Fragment(), PaymentGatewayListener {

    companion object {
        private const val TAG = "SubscriptionPlansFragment"
    }

    private var _binding: FragmentSubscriptionPlansBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubscriptionViewModel by viewModels()

    private val cardUiHelper by lazy {
        SubscriptionCardUiHelper(requireContext())
    }

    private var razorpayHandler: RazorpayPaymentHandler? = null
    private var phonePeHandler: PhonePePaymentHandler? = null
    private var activeOrderDetails: PaymentOrderDetails? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Inflating SubscriptionPlansFragment layout")
        _binding = FragmentSubscriptionPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Initializing subscription UI views & payment handlers")

        razorpayHandler = RazorpayPaymentHandler(requireActivity(), this)
        phonePeHandler = PhonePePaymentHandler(requireActivity(), this)

        setupToolbar()
        setupStrikethroughPrice()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarSubscription.setNavigationOnClickListener {
            Log.d(TAG, "Toolbar back button clicked")
            handleBackNavigation()
        }
    }

    private fun handleBackNavigation() {
        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        } else {
            Log.i(TAG, "No fragment backstack, launching ManagerHomeActivity Dashboard")
            val intent = Intent(requireContext(), ManagerHomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        }
    }

    private fun setupStrikethroughPrice() {
        binding.tvOriginalPriceYearly.paintFlags =
            binding.tvOriginalPriceYearly.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    }

    private fun setupListeners() {
        binding.cardTrialPlan.setOnClickListener {
            val state = viewModel.uiState.value
            if (state is SubscriptionUiState.Success) {
                val userSub = state.userSubscription
                if (userSub.status == SubscriptionStatus.EXPIRED || userSub.trialDaysRemaining <= 0) {
                    Log.w(TAG, "User clicked trial plan card but free trial is EXPIRED")
                    Toast.makeText(requireContext(), "Free Trial has expired. Please select a paid plan.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            Log.d(TAG, "User clicked Free Trial Plan card")
            viewModel.selectBillingCycle(BillingCycle.TRIAL)
        }

        binding.cardYearlyPlan.setOnClickListener {
            Log.d(TAG, "User clicked Yearly Plan card")
            viewModel.selectBillingCycle(BillingCycle.YEARLY)
        }

        binding.cardMonthlyPlan.setOnClickListener {
            Log.d(TAG, "User clicked Monthly Plan card")
            viewModel.selectBillingCycle(BillingCycle.MONTHLY)
        }

        binding.btnSubscribe.setOnClickListener {
            val state = viewModel.uiState.value
            if (state is SubscriptionUiState.Success) {
                val selectedPlan = state.selectedPlan
                val selectedCycle = state.selectedBillingCycle

                if (selectedCycle == BillingCycle.TRIAL) {
                    Toast.makeText(
                        requireContext(),
                        "🎉 30-Day Free Trial active! Opening Dashboard...",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(requireContext(), ManagerHomeActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(intent)
                } else if (selectedPlan != null) {
                    showPaymentGatewayPicker(selectedPlan)
                }
            }
        }
    }

    private fun showPaymentGatewayPicker(plan: SubscriptionPlan) {
        val pickerSheet = PaymentGatewayPickerBottomSheet.newInstance(plan) { gateway ->
            Log.i(TAG, "User selected Gateway: ${gateway.name} for Plan: '${plan.title}' (${plan.priceText})")
            viewModel.initiatePayment(gateway)
        }
        pickerSheet.show(childFragmentManager, PaymentGatewayPickerBottomSheet.TAG)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is SubscriptionUiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                            is SubscriptionUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                renderSubscriptionSuccess(state)
                            }
                            is SubscriptionUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.paymentState.collect { paymentState ->
                        handlePaymentState(paymentState)
                    }
                }
            }
        }
    }

    private fun handlePaymentState(state: PaymentState) {
        Log.d(TAG, "handlePaymentState: ${state::class.java.simpleName}")
        when (state) {
            is PaymentState.Idle -> binding.progressBar.visibility = View.GONE
            is PaymentState.InitiatingOrder -> {
                binding.progressBar.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Creating payment order...", Toast.LENGTH_SHORT).show()
            }
            is PaymentState.AwaitingSdk -> {
                binding.progressBar.visibility = View.GONE
                activeOrderDetails = state.orderDetails
                launchPaymentSdk(state.orderDetails)
            }
            is PaymentState.VerifyingPayment -> {
                binding.progressBar.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Verifying payment & activating subscription...", Toast.LENGTH_SHORT).show()
            }
            is PaymentState.Success -> {
                binding.progressBar.visibility = View.GONE
                showSuccessDialog(state.message)
                viewModel.resetPaymentState()
            }
            is PaymentState.Error -> {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Payment Error: ${state.errorMessage}", Toast.LENGTH_LONG).show()
                viewModel.resetPaymentState()
            }
        }
    }

    private fun launchPaymentSdk(orderDetails: PaymentOrderDetails) {
        when (orderDetails.gateway) {
            PaymentGateway.RAZORPAY -> {
                Log.i(TAG, "launchPaymentSdk: Launching Razorpay SDK...")
                razorpayHandler?.startPayment(orderDetails)
            }
            PaymentGateway.PHONEPE -> {
                Log.i(TAG, "launchPaymentSdk: Launching PhonePe SDK...")
                phonePeHandler?.startPayment(orderDetails)
            }
        }
    }

    override fun onPaymentSuccess(payload: PaymentResultPayload) {
        Log.i(TAG, "onPaymentSuccess: Payment ID = '${payload.paymentId}' via ${payload.gateway.name}")
        viewModel.processPaymentResult(payload)
    }

    override fun onPaymentFailure(gateway: PaymentGateway, code: Int, description: String?) {
        Log.e(TAG, "onPaymentFailure: Gateway=${gateway.name}, Code=$code, Desc=$description")
        Toast.makeText(requireContext(), "Payment failed or cancelled: $description", Toast.LENGTH_LONG).show()
        viewModel.resetPaymentState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activeOrderDetails?.let { details ->
            if (details.gateway == PaymentGateway.PHONEPE) {
                phonePeHandler?.handleActivityResult(requestCode, resultCode, data, details)
            }
        }
    }

    private fun showSuccessDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🎉 Subscription Active!")
            .setMessage(message)
            .setPositiveButton("Open Dashboard") { _, _ ->
                val intent = Intent(requireContext(), ManagerHomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            }
            .setCancelable(false)
            .show()
    }

    private fun renderSubscriptionSuccess(state: SubscriptionUiState.Success) {
        val userSub = state.userSubscription

        cardUiHelper.renderStatusHeader(binding, userSub)

        val features = state.plans.firstOrNull()?.features ?: emptyList()
        binding.rvFeatures.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = SubscriptionFeatureAdapter(features)
        }

        cardUiHelper.updatePlanCardSelection(
            binding = binding,
            selectedCycle = state.selectedBillingCycle,
            selectedPlan = state.selectedPlan,
            userSub = userSub
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Clearing ViewBinding reference")
        _binding = null
    }
}
