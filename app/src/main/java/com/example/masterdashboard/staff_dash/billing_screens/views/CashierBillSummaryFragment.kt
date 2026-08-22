package com.example.masterdashboard.staff_dash.billing_screens.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.masterdashboard.databinding.FragmentCashierBillSummaryBinding
import com.example.masterdashboard.print_bill.PrintBillController
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import java.text.SimpleDateFormat
import java.util.Locale

class CashierBillSummaryFragment : Fragment() {

    companion object {
        private const val TAG = "CashierBillSummaryFrag"
        private const val ARG_ORDER = "arg_order"

        fun newInstance(
            order: CashierBillingOrderModel,
            received: Double,
            change: Double
        ): CashierBillSummaryFragment {
            return CashierBillSummaryFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ORDER, order)
                    putDouble("receivedAmount", received)
                    putDouble("change", change)
                }
            }
        }
    }

    private var _binding: FragmentCashierBillSummaryBinding? = null
    private val mBinding get() = _binding!!

    // Printer Orchestrator
    private lateinit var printController: PrintBillController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printController = PrintBillController(this) { isLoading ->
            // Update progress indicator if available in summary layout
            // For now, we'll just log or use a global loading if needed
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Inflating Bill Summary layout")
        _binding = FragmentCashierBillSummaryBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Displaying final bill summary")

        setupBackPress()
        populateDetails()
        setupActions()
    }

    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "System back pressed. Redirecting to New Bill/Home.")
                startNewBill()
            }
        })
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun populateDetails() {
        val order = arguments?.getSerializable(ARG_ORDER) as? CashierBillingOrderModel
        if (order == null) {
            Log.e(TAG, "No order data found in arguments!")
            return
        }

        val billNo = "BILL-${(1000..9999).random()}"
        val orderId = if (order.orderId.startsWith("#")) order.orderId else "#${order.orderId}"
        val totalItems = order.items.sumOf { it.quantity }
        val totalAmount = order.grandTotal
        val method = order.paymentMethod.ifEmpty { "Paid" }
        val received = arguments?.getDouble("receivedAmount") ?: 0.0
        val change = arguments?.getDouble("change") ?: 0.0
        val paymentTime = order.paidAt?.toDate() ?: order.timestamp.toDate()
        val dateTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(paymentTime)

        Log.d(TAG, "Populating Bill Summary: No=$billNo, Order=$orderId, Method=$method, Total=₹$totalAmount")

        mBinding.rowBillNumber.tvRowLabel.text = "Bill Number"
        mBinding.rowBillNumber.tvRowValue.text = billNo

        mBinding.rowOrderId.tvRowLabel.text = "Order ID"
        mBinding.rowOrderId.tvRowValue.text = orderId

        mBinding.rowTotalItems.tvRowLabel.text = "Total Items"
        mBinding.rowTotalItems.tvRowValue.text = totalItems.toString()

        mBinding.rowTotalAmount.tvRowLabel.text = "Total Amount"
        mBinding.rowTotalAmount.tvRowValue.text = "₹ ${String.format("%.2f", totalAmount)}"

        mBinding.rowPaymentMethod.tvRowLabel.text = "Payment Method"
        mBinding.rowPaymentMethod.tvRowValue.text = method

        mBinding.rowReceivedAmount.tvRowLabel.text = "Received Amount"
        mBinding.rowReceivedAmount.tvRowValue.text = "₹ ${String.format("%.2f", received)}"

        mBinding.rowChange.tvRowLabel.text = "Change"
        mBinding.rowChange.tvRowValue.text = "₹ ${String.format("%.2f", change)}"

        mBinding.rowDateTime.tvRowLabel.text = "Date & Time"
        mBinding.rowDateTime.tvRowValue.text = dateTime
    }

    private fun setupActions() {
        mBinding.btnPrintBill.setOnClickListener {
            val order = arguments?.getSerializable(ARG_ORDER) as? CashierBillingOrderModel
            if (order != null) {
                Log.i(TAG, "Print Bill clicked from Summary for order: ${order.orderId}")
                printController.checkAndPrint(order)
            } else {
                Log.w(TAG, "Cannot print: Order data is missing")
                Toast.makeText(context, "Order data error", Toast.LENGTH_SHORT).show()
            }
        }

        mBinding.btnNewBill.setOnClickListener {
            Log.i(TAG, "New Bill button clicked. Navigating back to start.")
            startNewBill()
        }
    }

    private fun startNewBill() {
        Log.d(TAG, "Cleaning up session and returning to Billing list.")
        // Clear backstack and go to main billing screen
        parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        (activity as? CashierHomeActivity)?.openBills()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}