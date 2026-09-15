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
import com.example.masterdashboard.subscription.models.SubscriptionStatus
import com.example.masterdashboard.subscription.uistate.SubscriptionUiState
import com.example.masterdashboard.subscription.utils.SubscriptionCardUiHelper
import com.example.masterdashboard.subscription.viewModel.SubscriptionViewModel
import kotlinx.coroutines.launch

class SubscriptionPlansFragment : Fragment() {

    companion object {
        private const val TAG = "SubscriptionPlansFragment"
    }

    private var _binding: FragmentSubscriptionPlansBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubscriptionViewModel by viewModels()

    private val cardUiHelper by lazy {
        SubscriptionCardUiHelper(requireContext())
    }

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
        Log.i(TAG, "onViewCreated: Initializing subscription UI views & listeners")

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
                val userStatus = state.userSubscription.status
                val daysLeft = state.userSubscription.trialDaysRemaining

                Log.i(
                    TAG,
                    "ACTION CLICKED -> SelectedCycle=$selectedCycle, PlanID='${selectedPlan?.id}', Title='${selectedPlan?.title}', Price='${selectedPlan?.priceText}', UserStatus=$userStatus, TrialDaysLeft=$daysLeft"
                )

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
                } else {
                    val price = selectedPlan?.priceText ?: "₹2,999"
                    val planName = selectedPlan?.title ?: "Yearly Owner Pass"

                    Toast.makeText(
                        requireContext(),
                        "🎉 Selected $planName ($price). Payment gateway integration ready!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Log.w(TAG, "Subscribe clicked but UI state is not Success: $state")
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d(TAG, "observeViewModel: Received UI State -> ${state::class.java.simpleName}")
                    when (state) {
                        is SubscriptionUiState.Loading -> {
                            Log.d(TAG, "Subscription UI State: Loading")
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is SubscriptionUiState.Success -> {
                            Log.i(
                                TAG,
                                "Subscription UI State: Success -> UserStatus=${state.userSubscription.status}, SelectedCycle=${state.selectedBillingCycle}, SelectedPlan='${state.selectedPlan?.title}'"
                            )
                            binding.progressBar.visibility = View.GONE
                            renderSubscriptionSuccess(state)
                        }
                        is SubscriptionUiState.Error -> {
                            Log.e(TAG, "Subscription UI State: Error -> ${state.message}")
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun renderSubscriptionSuccess(state: SubscriptionUiState.Success) {
        val userSub = state.userSubscription

        // 1. Delegate Status Header rendering to UI Helper
        cardUiHelper.renderStatusHeader(binding, userSub)

        // 2. Render Features list
        val features = state.plans.firstOrNull()?.features ?: emptyList()
        binding.rvFeatures.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = SubscriptionFeatureAdapter(features)
        }

        // 3. Delegate Card Selections and Action Button Text to UI Helper
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
