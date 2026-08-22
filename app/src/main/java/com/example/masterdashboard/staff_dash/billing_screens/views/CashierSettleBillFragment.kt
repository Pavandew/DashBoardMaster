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
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.utils.AppConstants
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
    private val sessionManager by lazy { SessionManager(requireContext()) }
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

        // Accessing nested binding views
        mBinding.layoutUnpaid.layoutCash.tvPaymentName.text = "Cash"
        mBinding.layoutUnpaid.layoutUpi.tvPaymentName.text = "UPI"
        mBinding.layoutUnpaid.layoutCard.tvPaymentName.text = "Card"
        mBinding.layoutUnpaid.layoutWallet.tvPaymentName.text = "Wallet"
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
            if (order?.orderStatus?.uppercase() == "PAID") {
                navigateToSummary(order)
                return@setOnClickListener
            }
            showPaymentConfirmationDialog()
        }
    }

    private fun navigateToSummary(order: CashierBillingOrderModel) {
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
            .replace(this.id, summaryFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showPaymentConfirmationDialog() {
        val currentOrder = viewModel.activeBillingOrder.value ?: return
        val method = viewModel.getSelectedPaymentMode()
        
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

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            val managerId = sessionManager.getUid()
            viewModel.settleAndCompleteOrder(managerId)
        }
        dialog.show()
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
        mBinding.layoutPaid.tvPaidMethodValue.text = "Paid via ${order.paymentMethod.ifEmpty { "Paid" }}"
        
        val paidAt = order.paidAt?.toDate() ?: order.timestamp.toDate()
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        mBinding.layoutPaid.tvPaidTimeValue.text = "Confirmed at ${sdf.format(paidAt)}"
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                launch {
                    viewModel.activeBillingOrder.collectLatest { order ->
                        if (order != null) {
                            val displayOrderId = if (order.orderId.startsWith("#")) order.orderId else "#${order.orderId}"
                            mBinding.tvHeaderOrderId.text = "Order $displayOrderId"
                            mBinding.tvHeaderTableNo.text = order.tableName

                            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                            mBinding.tvHeaderTimestamp.text = "Order Time: ${sdf.format(order.timestamp.toDate())}"

                            val status = order.orderStatus.uppercase()
                            
                            when (status) {
                                "COMPLETED" -> {
                                    mBinding.tvBillingStatusTag.text = "HANDED OVER"
                                    mBinding.tvBillingStatusTag.setBackgroundResource(R.drawable.bg_status_green)
                                    mBinding.tvBillingStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green))
                                    mBinding.btnSettleAndPrint.text = "Re-Print Bill"
                                    mBinding.btnHandOver.isVisible = false
                                    disableInteraction()
                                    showPaidInfo(order)
                                }
                                "PAID" -> {
                                    mBinding.tvBillingStatusTag.text = "PAID"
                                    mBinding.tvBillingStatusTag.setBackgroundResource(R.drawable.bg_status_green)
                                    mBinding.tvBillingStatusTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green))

                                    val isCounterOrder = order.orderType == "TAKE_AWAY" || order.orderType == "DELIVERY"
                                    mBinding.btnHandOver.isVisible = isCounterOrder
                                    mBinding.btnSettleAndPrint.text = "Re-Print Bill"
                                    
                                    disableInteraction()
                                    showPaidInfo(order)
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
