package com.example.masterdashboard.staff_dash.billing_screens.views

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentCashierSettleBillBinding
import com.example.masterdashboard.databinding.LayoutPaymentMethodItemBinding
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.billing_screens.adapter.BillingItemsAdapter
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.example.masterdashboard.staff_dash.billing_screens.viewmodel.CashierSettleViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class CashierSettleBillFragment : Fragment() {

    companion object {
        private const val TAG = "CashierSettleBillFrag"
        private const val ARG_ORDER = "arg_order"
        
        fun newInstance(order: CashierBillingOrderModel): CashierSettleBillFragment {
            return CashierSettleBillFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ORDER, order)
                }
            }
        }
    }

    private var _binding: FragmentCashierSettleBillBinding? = null
    private val mBinding get() = _binding!!

    private val viewModel: CashierSettleViewModel by viewModels()
    private lateinit var itemsAdapter: BillingItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Inflating settlement screen")
        _binding = FragmentCashierSettleBillBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Initializing order settlement")

        val order = arguments?.getSerializable(ARG_ORDER) as? CashierBillingOrderModel
        if (order != null) {
            Log.d(TAG, "Loaded Order: ${order.orderId} for Table: ${order.tableName}")
            viewModel.setOrder(order)
        }

        // Hide bottom navigation for full checkout focus
        (activity as? CashierHomeActivity)?.hideBottomNavigation()

        setupUI()
        setupListeners()
        observeViewModel()

        // Set default selection UI
        selectPaymentUI("Cash")
    }

    private fun setupUI() {
        mBinding.settleToolbar.tvToolbarTitle.text = "Settle Bill"
        mBinding.settleToolbar.toolbarImgMenu.setOnClickListener {
            Log.v(TAG, "Back button clicked")
            parentFragmentManager.popBackStack()
        }

        itemsAdapter = BillingItemsAdapter()
        mBinding.rvBillingItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = itemsAdapter
        }

        // Set labels for reusable included layouts
        mBinding.layoutCash.tvPaymentName.text = "Cash"
        mBinding.layoutUpi.tvPaymentName.text = "UPI"
        mBinding.layoutCard.tvPaymentName.text = "Card"
        mBinding.layoutWallet.tvPaymentName.text = "Wallet"
    }

    private fun setupListeners() {
        mBinding.layoutCash.root.setOnClickListener { 
            Log.v(TAG, "Method selected: Cash")
            selectPaymentMethod("Cash") 
        }
        mBinding.layoutUpi.root.setOnClickListener { 
            Log.v(TAG, "Method selected: UPI")
            selectPaymentMethod("UPI") 
        }
        mBinding.layoutCard.root.setOnClickListener { 
            Log.v(TAG, "Method selected: Card")
            selectPaymentMethod("Card") 
        }
        mBinding.layoutWallet.root.setOnClickListener { 
            Log.v(TAG, "Method selected: Wallet")
            selectPaymentMethod("Wallet") 
        }

        mBinding.btnApplyDiscount.setOnClickListener {
            val discountInput = mBinding.etDiscountInput.text.toString()
            val discount = discountInput.toDoubleOrNull() ?: 0.0
            Log.i(TAG, "Applying Discount: ₹$discount")
            viewModel.applyDiscount(discount)
        }

        mBinding.btnHandOver.setOnClickListener {
            val order = viewModel.activeBillingOrder.value ?: return@setOnClickListener
            Log.i(TAG, "Hand over clicked for order: ${order.orderId}")
            
            // Reusing the settle process to mark as COMPLETED
            // Or better, let's just trigger the confirm Pickup logic from repository here too.
            // But we need to handle it in SettleViewModel.
            // For now, let's just go back and let them do it from the list if possible, 
            // or add confirmPickup to SettleViewModel.
            
            // I'll add confirmPickup to SettleViewModel for consistency.
            viewModel.confirmPickup()
        }

        mBinding.btnSettleAndPrint.setOnClickListener {
            val order = viewModel.activeBillingOrder.value
            
            // Handle re-printing for already paid orders
            if (order?.orderStatus?.uppercase() == "PAID") {
                Log.i(TAG, "Re-print requested for order: ${order.orderId}")
                val summaryFragment = CashierBillSummaryFragment.newInstance(
                    orderId = order.orderId,
                    totalItems = order.items.sumOf { it.quantity },
                    totalAmount = order.grandTotal,
                    method = order.paymentMethod.ifEmpty { "Paid" },
                    received = order.grandTotal,
                    change = 0.0,
                    paidAtMillis = order.paidAt?.toDate()?.time ?: order.timestamp.toDate().time
                )
                parentFragmentManager.beginTransaction()
                    .replace(this@CashierSettleBillFragment.id, summaryFragment)
                    .addToBackStack(null)
                    .commit()
                return@setOnClickListener
            }

            // Normal Flow: Open confirmation dialog
            showPaymentConfirmationDialog()
        }
    }

    /**
     * Creates and displays a premium confirmation dialog for the selected payment method.
     * This ensures the cashier verifies receipt of funds before marking the order as PAID.
     */
    private fun showPaymentConfirmationDialog() {
        val currentOrder = viewModel.activeBillingOrder.value ?: return
        val method = viewModel.getSelectedPaymentMode()
        
        Log.i("CashierSettleFlow", "💳 [UI] Opening confirmation dialog for Method: $method, Amount: ${currentOrder.grandTotal}")

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment_qr, null)
        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvPaymentTitle)
        val tvSubtitle = dialogView.findViewById<android.widget.TextView>(R.id.tvPaymentSubtitle)
        val ivIcon = dialogView.findViewById<android.widget.ImageView>(R.id.ivPaymentQr)
        val tvAmount = dialogView.findViewById<android.widget.TextView>(R.id.tvAmountToPay)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancelPayment)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirmPayment)

        tvAmount.text = "₹${String.format("%.2f", currentOrder.grandTotal)}"
        tvTitle.text = "$method Payment"

        // Adjust dialog content based on payment type
        when (method) {
            "UPI" -> {
                tvSubtitle.text = "Scan the QR code to pay"
                ivIcon.setImageResource(R.drawable.biling) 
                ivIcon.colorFilter = null
            }
            "Cash" -> {
                tvSubtitle.text = "Confirm cash received from customer"
                ivIcon.setImageResource(R.drawable.ic_payments_24dp)
                ivIcon.setColorFilter(Color.parseColor("#16A34A")) 
            }
            "Card" -> {
                tvSubtitle.text = "Confirm transaction success on POS machine"
                ivIcon.setImageResource(R.drawable.ic_payments_24dp)
                ivIcon.setColorFilter(Color.parseColor("#3554FF")) 
            }
            else -> {
                tvSubtitle.text = "Verify payment receipt on your device"
                ivIcon.setImageResource(R.drawable.ic_inventory_24dp)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false) 
            .create()

        btnCancel.setOnClickListener {
            Log.d("CashierSettleFlow", "❌ Payment confirmation cancelled")
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            Log.i("CashierSettleFlow", "✅ Payment verified. Finalizing in Firestore.")
            dialog.dismiss()
            viewModel.settleAndCompleteOrder()
        }

        dialog.show()
    }

    private fun selectPaymentMethod(method: String) {
        viewModel.setSelectedPaymentMode(method)
        selectPaymentUI(method)
    }

    private fun selectPaymentUI(method: String) {
        // Reset all cards to white
        resetCardUI(mBinding.layoutCash, R.drawable.ic_payments_24dp)
        resetCardUI(mBinding.layoutUpi, R.drawable.ic_payments_24dp)
        resetCardUI(mBinding.layoutCard, R.drawable.ic_payments_24dp)
        resetCardUI(mBinding.layoutWallet, R.drawable.ic_inventory_24dp)

        // Highlight active selection
        when (method) {
            "Cash" -> highlightCardUI(mBinding.layoutCash)
            "UPI" -> highlightCardUI(mBinding.layoutUpi)
            "Card" -> highlightCardUI(mBinding.layoutCard)
            "Wallet" -> highlightCardUI(mBinding.layoutWallet)
        }
    }

    private fun highlightCardUI(itemBinding: LayoutPaymentMethodItemBinding) {
        itemBinding.cardPaymentRoot.setCardBackgroundColor(Color.parseColor("#F0FDF4")) 
        itemBinding.cardPaymentRoot.strokeColor = Color.parseColor("#BBF7D0") 
        itemBinding.cardPaymentRoot.strokeWidth = 3
        itemBinding.ivPaymentIcon.setImageResource(R.drawable.success)
        itemBinding.ivPaymentIcon.colorFilter = null 
    }

    private fun resetCardUI(itemBinding: LayoutPaymentMethodItemBinding, defaultIcon: Int) {
        itemBinding.cardPaymentRoot.setCardBackgroundColor(Color.WHITE)
        itemBinding.cardPaymentRoot.strokeColor = Color.parseColor("#E5E7EB") 
        itemBinding.cardPaymentRoot.strokeWidth = 2
        itemBinding.ivPaymentIcon.setImageResource(defaultIcon)
        itemBinding.ivPaymentIcon.setColorFilter(Color.parseColor("#6B7280")) 
    }

    private fun disableInteraction() {
        mBinding.layoutCash.root.isEnabled = false
        mBinding.layoutUpi.root.isEnabled = false
        mBinding.layoutCard.root.isEnabled = false
        mBinding.layoutWallet.root.isEnabled = false
        mBinding.btnApplyDiscount.isEnabled = false
        mBinding.etDiscountInput.isEnabled = false
    }

    private fun enableInteraction() {
        mBinding.layoutCash.root.isEnabled = true
        mBinding.layoutUpi.root.isEnabled = true
        mBinding.layoutCard.root.isEnabled = true
        mBinding.layoutWallet.root.isEnabled = true
        mBinding.btnApplyDiscount.isEnabled = true
        mBinding.etDiscountInput.isEnabled = true
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                // Pipeline 1: Track order details and update UI labels
                launch {
                    viewModel.activeBillingOrder.collectLatest { order ->
                        if (order != null) {
                            val displayOrderId = if (order.orderId.startsWith("#")) order.orderId else "#${order.orderId}"
                            mBinding.tvHeaderOrderId.text = "Order $displayOrderId"
                            mBinding.tvHeaderTableNo.text = order.tableName

                            // Format and display the order creation timestamp
                            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                            val dateStr = sdf.format(order.timestamp.toDate())
                            mBinding.tvHeaderTimestamp.text = "Order Time: $dateStr"

                            val status = order.orderStatus.uppercase()
                            Log.v(TAG, "UI Sync: Order Status is $status")
                            
                            when (status) {
                                "COMPLETED" -> {
                                    mBinding.tvBillingStatusTag.text = "HANDED OVER"
                                    mBinding.tvBillingStatusTag.setBackgroundResource(R.drawable.bg_status_green)
                                    mBinding.tvBillingStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green))
                                    mBinding.btnSettleAndPrint.text = "Re-Print Bill"
                                    mBinding.btnHandOver.isVisible = false
                                    disableInteraction()
                                }
                                "PAID" -> {
                                    mBinding.tvBillingStatusTag.text = "PAID"
                                    mBinding.tvBillingStatusTag.setBackgroundResource(R.drawable.bg_status_green)
                                    mBinding.tvBillingStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green))

                                    // Show Hand Over button for Takeaway orders that are PAID
                                    val isCounterOrder = order.orderType == "TAKE_AWAY" || order.orderType == "DELIVERY"
                                    mBinding.btnHandOver.isVisible = isCounterOrder

                                    mBinding.btnSettleAndPrint.text = "Re-Print Bill"
                                    
                                    disableInteraction()
                                }
                                "BILLING" -> {
                                    mBinding.tvBillingStatusTag.text = "READY FOR BILL"
                                    mBinding.tvBillingStatusTag.setBackgroundResource(R.drawable.bg_status_amber)
                                    mBinding.tvBillingStatusTag.setTextColor(Color.parseColor("#C2410C"))
                                    mBinding.btnSettleAndPrint.text = "Settle & Print Bill"
                                    mBinding.btnHandOver.isVisible = false
                                    enableInteraction()
                                }
                                else -> {
                                    mBinding.tvBillingStatusTag.text = "RUNNING BILL"
                                    mBinding.tvBillingStatusTag.setBackgroundResource(R.drawable.bg_status_blue)
                                    mBinding.tvBillingStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                                    mBinding.btnSettleAndPrint.text = "Settle & Print Bill"
                                    mBinding.btnHandOver.isVisible = false
                                    enableInteraction()
                                }
                            }

                            itemsAdapter.submitList(order.items)

                            mBinding.tvSubtotalValue.text = "₹${order.subtotal}"
                            mBinding.tvTaxValue.text = "₹${order.taxAmount}"
                            mBinding.tvDiscountValue.text = "- ₹${order.discountAmount}"
                            mBinding.tvGrandTotalValue.text = "₹${order.grandTotal}"
                        }
                    }
                }

                // Pipeline 2: Track Firestore transaction status
                launch {
                    viewModel.settleState.collectLatest { state ->
                        when (state) {
                            is ResourceUiState.Loading -> {
                                mBinding.progressBar.isVisible = true
                                mBinding.btnSettleAndPrint.isEnabled = false
                            }
                            is ResourceUiState.Success -> {
                                Log.i(TAG, "🚀 Settlement Success: ${state.data}. Navigating to Summary.")
                                mBinding.progressBar.isVisible = false
                                Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()

                                val order = viewModel.activeBillingOrder.value
                                if (order != null) {
                                    val summaryFragment = CashierBillSummaryFragment.newInstance(
                                        orderId = order.orderId,
                                        totalItems = order.items.sumOf { it.quantity },
                                        totalAmount = order.grandTotal,
                                        method = viewModel.getSelectedPaymentMode(),
                                        received = order.grandTotal, 
                                        change = 0.0,
                                        paidAtMillis = System.currentTimeMillis()
                                    )
                                    parentFragmentManager.beginTransaction()
                                        .replace(this@CashierSettleBillFragment.id, summaryFragment)
                                        .addToBackStack(null)
                                        .commit()
                                } else {
                                    parentFragmentManager.popBackStack()
                                }
                            }
                            is ResourceUiState.Error -> {
                                Log.e(TAG, "❌ Settlement Error: ${state.message}")
                                mBinding.progressBar.isVisible = false
                                mBinding.btnSettleAndPrint.isEnabled = true
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                mBinding.progressBar.isVisible = false
                                mBinding.btnSettleAndPrint.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Re-enabling home navigation")
        (activity as? CashierHomeActivity)?.showBottomNavigation()
        _binding = null
    }
}