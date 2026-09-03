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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.databinding.FragmentCashierBillSummaryBinding
import com.example.masterdashboard.print_bill.PrintBillController
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.billing_screens.viewmodel.CashierBillSummaryViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.utils.NavigationUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CashierBillSummaryFragment : Fragment() {

    companion object {
        private const val TAG = "CashierBillSummaryFrag"
        
        fun newInstance(
            orderDocPath: String,
            orderId: String,
            totalAmount: Double,
            received: Double,
            change: Double,
            discount: Double,
            itemCount: Int,
            method: String,
            dateTime: String
        ): CashierBillSummaryFragment {
            return CashierBillSummaryFragment().apply {
                arguments = Bundle().apply {
                    putString("orderDocPath", orderDocPath)
                    putString("orderId", orderId)
                    putDouble("totalAmount", totalAmount)
                    putDouble("receivedAmount", received)
                    putDouble("change", change)
                    putDouble("discount", discount)
                    putInt("itemCount", itemCount)
                    putString("method", method)
                    putString("dateTime", dateTime)
                }
            }
        }
    }

    private var _binding: FragmentCashierBillSummaryBinding? = null
    private val mBinding get() = _binding!!

    private val viewModel: CashierBillSummaryViewModel by viewModels()

    // Printer Orchestrator
    private lateinit var printController: PrintBillController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printController = PrintBillController(this) { isLoading -> }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCashierBillSummaryBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackPress()
        populateDetails()
        setupActions()
        observeViewModel()
    }

    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startNewBill()
            }
        })
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun populateDetails() {
        val args = arguments ?: return

        val billNo = "BILL-${(1000..9999).random()}"
        val orderId = args.getString("orderId", "N/A")
        val displayOrderId = if (orderId.startsWith("#")) orderId else "#$orderId"
        val totalItems = args.getInt("itemCount", 0)
        val totalAmount = args.getDouble("totalAmount", 0.0)
        val method = args.getString("method", "Paid")
        val received = args.getDouble("receivedAmount", 0.0)
        val change = args.getDouble("change", 0.0)
        val discount = args.getDouble("discount", 0.0)
        val dateTime = args.getString("dateTime", "")

        mBinding.rowBillNumber.tvRowLabel.text = "Bill Number"
        mBinding.rowBillNumber.tvRowValue.text = billNo

        mBinding.rowOrderId.tvRowLabel.text = "Order ID"
        mBinding.rowOrderId.tvRowValue.text = displayOrderId

        mBinding.rowTotalItems.tvRowLabel.text = "Total Items"
        mBinding.rowTotalItems.tvRowValue.text = totalItems.toString()

        mBinding.rowTotalAmount.tvRowLabel.text = "Total Amount"
        mBinding.rowTotalAmount.tvRowValue.text = "₹ ${String.format("%.2f", totalAmount)}"

        mBinding.rowDiscount.tvRowLabel.text = "Discount"
        mBinding.rowDiscount.tvRowValue.text = "₹ ${String.format("%.2f", discount)}"

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
            val docPath = arguments?.getString("orderDocPath") ?: ""
            if (docPath.isNotEmpty()) {
                viewModel.fetchOrderDetails(docPath)
            } else {
                Toast.makeText(context, "Order path error", Toast.LENGTH_SHORT).show()
            }
        }

        mBinding.btnNewBill.setOnClickListener {
            startNewBill()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.orderData.collectLatest { resource ->
                    when (resource) {
                        is ResourceUiState.Success -> {
                            printController.checkAndPrint(resource.data)
                        }
                        is ResourceUiState.Error -> {
                            Toast.makeText(context, "Printer Error: ${resource.message}", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun startNewBill() {
        val manager = parentFragmentManager
        val cashierActivity = activity as? CashierHomeActivity

        if (cashierActivity != null) {
            manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            cashierActivity.openBills()
        } else {
            // Returning from ManagerHomeActivity or SingleResOwnerHomeActivity
            val popped = manager.popBackStackImmediate("settlement_flow", FragmentManager.POP_BACK_STACK_INCLUSIVE)
            if (!popped) {
                val containerId = NavigationUtils.getHostContainerId(activity)
                if (containerId != 0) {
                    manager.popBackStack()
                    manager.beginTransaction()
                        .replace(containerId, CashierBillingFragment())
                        .commit()
                } else {
                    manager.popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}