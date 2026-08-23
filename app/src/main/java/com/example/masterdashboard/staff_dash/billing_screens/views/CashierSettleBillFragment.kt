package com.example.masterdashboard.staff_dash.billing_screens.views

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.masterdashboard.print_bill.PrintBillController
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.billing_screens.adapter.BillingItemsAdapter
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.example.masterdashboard.staff_dash.billing_screens.viewmodel.CashierSettleViewModel
import com.example.masterdashboard.staff_dash.utils.PaymentDialogHelper
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.staff_dash.utils.StatusUIUtils
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
    private val sessionManager by lazy { SessionManager(requireContext()) }
    private lateinit var itemsAdapter: BillingItemsAdapter

    // Printer Orchestrator
    private lateinit var printController: PrintBillController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printController = PrintBillController(this) { isLoading ->
            mBinding.progressBar.isVisible = isLoading
        }
    }

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

        val order = arguments?.getSerializable(ARG_ORDER) as? CashierBillingOrderModel
        if (order != null) {
            Log.d(TAG, "Loaded Order: ${order.orderId} for Table: ${order.tableName}")
            viewModel.setOrder(order)
        }

        (activity as? CashierHomeActivity)?.hideBottomNavigation()

        setupUI()
        setupListeners()
        observeViewModel()

        selectPaymentUI("Cash")
    }

    private fun setupUI() {
        mBinding.settleToolbar.tvToolbarTitle.text = getString(R.string.settle_bill_title)
        mBinding.settleToolbar.toolbarImgMenu.setOnClickListener {
            Log.v(TAG, "Back button clicked")
            parentFragmentManager.popBackStack()
        }

        itemsAdapter = BillingItemsAdapter()
        mBinding.rvBillingItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = itemsAdapter
        }

        // Accessing nested binding views
        mBinding.layoutUnpaid.layoutCash.tvPaymentName.text = getString(R.string.payment_cash)
        mBinding.layoutUnpaid.layoutUpi.tvPaymentName.text = getString(R.string.payment_upi)
        mBinding.layoutUnpaid.layoutCard.tvPaymentName.text = getString(R.string.payment_card)
        mBinding.layoutUnpaid.layoutWallet.tvPaymentName.text = getString(R.string.payment_wallet)
    }

    private fun setupListeners() {
        mBinding.layoutUnpaid.layoutCash.root.setOnClickListener { selectPaymentMethod("Cash") }
        mBinding.layoutUnpaid.layoutUpi.root.setOnClickListener { selectPaymentMethod("UPI") }
        mBinding.layoutUnpaid.layoutCard.root.setOnClickListener { selectPaymentMethod("Card") }
        mBinding.layoutUnpaid.layoutWallet.root.setOnClickListener { selectPaymentMethod("Wallet") }

        mBinding.layoutUnpaid.btnApplyDiscount.setOnClickListener {
            val discountInput = mBinding.layoutUnpaid.etDiscountInput.text.toString()
            val discount = discountInput.toDoubleOrNull() ?: 0.0
            Log.i(TAG, "Applying Discount: ₹$discount")
            viewModel.applyDiscount(discount)
        }

        mBinding.btnHandOver.setOnClickListener {
            viewModel.confirmPickup()
        }

        mBinding.btnSettleAndPrint.setOnClickListener {
            val order = viewModel.activeBillingOrder.value
            if (order?.orderStatus?.uppercase() == "PAID" || order?.orderStatus?.uppercase() == "COMPLETED") {
                Log.d(TAG, "Re-printing bill for Order: ${order.orderId}")
                order?.let { printController.checkAndPrint(it) }
                return@setOnClickListener
            }
            showPaymentConfirmationDialog()
        }
    }

    private fun navigateToSummary(order: CashierBillingOrderModel) {
        val summaryFragment = CashierBillSummaryFragment.newInstance(
            order = order,
            received = order.grandTotal,
            change = 0.0
        )
        parentFragmentManager.beginTransaction()
            .replace(this.id, summaryFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showPaymentConfirmationDialog() {
        val currentOrder = viewModel.activeBillingOrder.value ?: return
        val method = viewModel.getSelectedPaymentMode()

        PaymentDialogHelper.showPaymentConfirmation(
            context = requireContext(),
            amount = currentOrder.grandTotal,
            paymentMethod = method,
            onConfirm = {
                val managerId = sessionManager.getUid()
                viewModel.settleAndCompleteOrder(managerId)
                
                // Auto-trigger print after settlement
                currentOrder.let { printController.checkAndPrint(it) }
            }
        )
    }

    private fun selectPaymentMethod(method: String) {
        viewModel.setSelectedPaymentMode(method)
        selectPaymentUI(method)
    }

    private fun selectPaymentUI(method: String) {
        resetCardUI(mBinding.layoutUnpaid.layoutCash, R.drawable.ic_payments_24dp)
        resetCardUI(mBinding.layoutUnpaid.layoutUpi, R.drawable.ic_payments_24dp)
        resetCardUI(mBinding.layoutUnpaid.layoutCard, R.drawable.ic_payments_24dp)
        resetCardUI(mBinding.layoutUnpaid.layoutWallet, R.drawable.ic_inventory_24dp)

        when (method) {
            "Cash" -> highlightCardUI(mBinding.layoutUnpaid.layoutCash)
            "UPI" -> highlightCardUI(mBinding.layoutUnpaid.layoutUpi)
            "Card" -> highlightCardUI(mBinding.layoutUnpaid.layoutCard)
            "Wallet" -> highlightCardUI(mBinding.layoutUnpaid.layoutWallet)
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
        mBinding.layoutUnpaid.root.isVisible = false
    }

    private fun enableInteraction() {
        mBinding.layoutUnpaid.root.isVisible = true
        mBinding.layoutPaid.root.isVisible = false
    }

    private fun showPaidInfo(order: CashierBillingOrderModel) {
        mBinding.layoutPaid.root.isVisible = true
        val paidMethod = order.paymentMethod.ifEmpty { getString(R.string.paid) }
        mBinding.layoutPaid.tvPaidMethodValue.text = getString(R.string.paid_via_format, paidMethod)
        
        val paidAt = order.paidAt?.toDate() ?: order.timestamp.toDate()
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        mBinding.layoutPaid.tvPaidTimeValue.text = getString(R.string.confirmed_at_format, sdf.format(paidAt))
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                launch {
                    viewModel.activeBillingOrder.collectLatest { order ->
                        if (order != null) {
                            val displayOrderId = if (order.orderId.startsWith("#")) order.orderId else "#${order.orderId}"
                            mBinding.tvHeaderOrderId.text = getString(R.string.order_label_format, displayOrderId)
                            mBinding.tvHeaderTableNo.text = order.tableName

                            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                            mBinding.tvHeaderTimestamp.text = getString(R.string.order_time_format, sdf.format(order.timestamp.toDate()))

                            val status = order.orderStatus.uppercase()
                            
                            // Apply centralized status styling
                            StatusUIUtils.applyStatusUI(requireContext(), mBinding.tvBillingStatusTag, status)

                            when (status) {
                                "COMPLETED" -> {
                                    mBinding.btnSettleAndPrint.text = getString(R.string.reprint_bill)
                                    mBinding.btnHandOver.isVisible = false
                                    disableInteraction()
                                    showPaidInfo(order)
                                }
                                "PAID" -> {
                                    val isCounterOrder = order.orderType == "TAKE_AWAY" || order.orderType == "DELIVERY"
                                    mBinding.btnHandOver.isVisible = isCounterOrder
                                    mBinding.btnSettleAndPrint.text = getString(R.string.reprint_bill)
                                    
                                    disableInteraction()
                                    showPaidInfo(order)
                                }
                                "BILLING" -> {
                                    mBinding.btnSettleAndPrint.text = getString(R.string.settle_and_print_bill)
                                    mBinding.btnHandOver.isVisible = false
                                    enableInteraction()
                                }
                                else -> {
                                    mBinding.btnSettleAndPrint.text = getString(R.string.settle_and_print_bill)
                                    mBinding.btnHandOver.isVisible = false
                                    enableInteraction()
                                }
                            }

                            itemsAdapter.submitList(order.items)

                            mBinding.tvSubtotalValue.text = getString(R.string.amount_format, String.format("%.2f", order.subtotal))
                            mBinding.tvTaxValue.text = getString(R.string.amount_format, String.format("%.2f", order.taxAmount))
                            mBinding.tvDiscountValue.text = getString(R.string.discount_format, String.format("%.2f", order.discountAmount))
                            mBinding.tvGrandTotalValue.text = getString(R.string.amount_format, String.format("%.2f", order.grandTotal))
                        }
                    }
                }

                launch {
                    viewModel.settleState.collectLatest { state ->
                        when (state) {
                            is ResourceUiState.Loading -> {
                                mBinding.progressBar.isVisible = true
                                mBinding.btnSettleAndPrint.isEnabled = false
                            }
                            is ResourceUiState.Success -> {
                                mBinding.progressBar.isVisible = false
                                Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()

                                val order = viewModel.activeBillingOrder.value
                                if (order != null) {
                                    val summaryFragment = CashierBillSummaryFragment.newInstance(
                                        order = order,
                                        received = order.grandTotal,
                                        change = 0.0
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
        (activity as? CashierHomeActivity)?.showBottomNavigation()
        _binding = null
    }
}
