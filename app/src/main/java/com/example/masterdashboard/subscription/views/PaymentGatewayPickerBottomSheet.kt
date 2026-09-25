package com.example.masterdashboard.subscription.views

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.masterdashboard.databinding.BottomSheetPaymentGatewayPickerBinding
import com.example.masterdashboard.subscription.models.PaymentGateway
import com.example.masterdashboard.subscription.models.SubscriptionPlan
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PaymentGatewayPickerBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "PaymentGatewayPickerBottomSheet"
        private const val ARG_PLAN_TITLE = "arg_plan_title"
        private const val ARG_PLAN_PRICE = "arg_plan_price"

        fun newInstance(
            selectedPlan: SubscriptionPlan,
            onGatewaySelected: (PaymentGateway) -> Unit
        ): PaymentGatewayPickerBottomSheet {
            val sheet = PaymentGatewayPickerBottomSheet()
            sheet.onGatewaySelectedListener = onGatewaySelected
            sheet.arguments = Bundle().apply {
                putString(ARG_PLAN_TITLE, selectedPlan.title)
                putString(ARG_PLAN_PRICE, selectedPlan.priceText)
            }
            return sheet
        }
    }

    private var _binding: BottomSheetPaymentGatewayPickerBinding? = null
    private val binding get() = _binding!!

    private var selectedGateway: PaymentGateway = PaymentGateway.RAZORPAY
    private var onGatewaySelectedListener: ((PaymentGateway) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPaymentGatewayPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val planTitle = arguments?.getString(ARG_PLAN_TITLE) ?: "Subscription Plan"
        val planPrice = arguments?.getString(ARG_PLAN_PRICE) ?: "₹2,999"

        binding.tvPlanSummary.text = "$planTitle • $planPrice"
        binding.btnProceedPayment.text = "PROCEED TO PAY $planPrice"

        setupSelectionListeners()
        updateUiState()
    }

    private fun setupSelectionListeners() {
        binding.cardOptionRazorpay.setOnClickListener {
            selectedGateway = PaymentGateway.RAZORPAY
            updateUiState()
        }

        binding.cardOptionPhonePe.setOnClickListener {
            selectedGateway = PaymentGateway.PHONEPE
            updateUiState()
        }

        binding.btnProceedPayment.setOnClickListener {
            dismiss()
            onGatewaySelectedListener?.invoke(selectedGateway)
        }
    }

    private fun updateUiState() {
        val activeBorderColor = Color.parseColor("#2563EB")
        val inactiveBorderColor = Color.parseColor("#E5E7EB")

        if (selectedGateway == PaymentGateway.RAZORPAY) {
            binding.rbRazorpay.isChecked = true
            binding.rbPhonePe.isChecked = false

            binding.cardOptionRazorpay.strokeColor = activeBorderColor
            binding.cardOptionRazorpay.strokeWidth = 4

            binding.cardOptionPhonePe.strokeColor = inactiveBorderColor
            binding.cardOptionPhonePe.strokeWidth = 2
        } else {
            binding.rbRazorpay.isChecked = false
            binding.rbPhonePe.isChecked = true

            binding.cardOptionRazorpay.strokeColor = inactiveBorderColor
            binding.cardOptionRazorpay.strokeWidth = 2

            binding.cardOptionPhonePe.strokeColor = activeBorderColor
            binding.cardOptionPhonePe.strokeWidth = 4
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
