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
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CashierBillSummaryFragment : Fragment() {

    companion object {
        private const val TAG = "CashierBillSummaryFrag"

        fun newInstance(
            orderId: String,
            totalItems: Int,
            totalAmount: Double,
            method: String,
            received: Double,
            change: Double,
            paidAtMillis: Long = System.currentTimeMillis()
        ): CashierBillSummaryFragment {
            return CashierBillSummaryFragment().apply {
                arguments = Bundle().apply {
                    putString("orderId", orderId)
                    putInt("totalItems", totalItems)
                    putDouble("totalAmount", totalAmount)
                    putString("paymentMethod", method)
                    putDouble("receivedAmount", received)
                    putDouble("change", change)
                    putLong("paidAtTime", paidAtMillis)
                }
            }
        }
    }

    private var _binding: FragmentCashierBillSummaryBinding? = null
    private val mBinding get() = _binding!!

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
        val billNo = arguments?.getString("billNo") ?: "BILL-${(1000..9999).random()}"
        val orderId = arguments?.getString("orderId") ?: "N/A"
        val totalItems = arguments?.getInt("totalItems") ?: 0
        val totalAmount = arguments?.getDouble("totalAmount") ?: 0.0
        val method = arguments?.getString("paymentMethod") ?: "Cash"
        val received = arguments?.getDouble("receivedAmount") ?: 0.0
        val change = arguments?.getDouble("change") ?: 0.0
        val paymentTime = arguments?.getLong("paidAtTime")?.let { Date(it) } ?: Date()
        val dateTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(paymentTime)

        Log.d(TAG, "Populating Bill: No=$billNo, Order=$orderId, Method=$method, Total=₹$totalAmount")

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
            Log.i(TAG, "Print Bill requested. Integration pending.")
            // Integration with printer will come later
            Toast.makeText(context, "Printer not connected", Toast.LENGTH_SHORT).show()
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