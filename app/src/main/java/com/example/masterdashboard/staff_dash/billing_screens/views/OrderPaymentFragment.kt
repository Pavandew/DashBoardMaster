package com.example.masterdashboard.staff_dash.billing_screens.views

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentOrderPaymentBinding
import com.example.masterdashboard.databinding.LayoutPaymentMethodItemBinding
import com.example.masterdashboard.staff_dash.billing_screens.views.CashierBillSummaryFragment
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OrderPaymentFragment : Fragment() {

    companion object {
        private const val TAG = "OrderPaymentFragment"
    }

    private var _binding: FragmentOrderPaymentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderTakingViewModel by activityViewModels()
    private val sessionManager by lazy { SessionManager(requireContext()) }

    private var selectedMethod = "Cash"
    private var totalAmount = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Inflating payment fragment layout")
        _binding = FragmentOrderPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Starting payment flow for new counter order")

        setupToolbar()
        setupPaymentMethods()
        setupReceivedAmountLogic()
        observeOrderData()
        setupGenerateBillAction()
    }

    private fun setupToolbar() {
        binding.paymentToolbar.tvToolbarTitle.text = "Payment"
        binding.paymentToolbar.toolbarImgMenu.setOnClickListener {
            Log.v(TAG, "Navigation back clicked")
            parentFragmentManager.popBackStack()
        }
        binding.paymentToolbar.toolbarImgNotification.isVisible = false
    }

    private fun setupPaymentMethods() {
        // Initialize payment option labels
        binding.layoutCash.tvPaymentName.text = "Cash"
        binding.layoutUpi.tvPaymentName.text = "UPI"
        binding.layoutCard.tvPaymentName.text = "Card"
        binding.layoutWallet.tvPaymentName.text = "Wallet"

        // Set icons for each method
        binding.layoutCash.ivPaymentIcon.setImageResource(R.drawable.ic_payments_24dp)
        binding.layoutUpi.ivPaymentIcon.setImageResource(R.drawable.ic_payments_24dp)
        binding.layoutCard.ivPaymentIcon.setImageResource(R.drawable.ic_payments_24dp)
        binding.layoutWallet.ivPaymentIcon.setImageResource(R.drawable.ic_inventory_24dp)

        // Register tap listeners for selection
        binding.layoutCash.root.setOnClickListener { updateSelectedMethod("Cash") }
        binding.layoutUpi.root.setOnClickListener { updateSelectedMethod("UPI") }
        binding.layoutCard.root.setOnClickListener { updateSelectedMethod("Card") }
        binding.layoutWallet.root.setOnClickListener { updateSelectedMethod("Wallet") }

        // Initialize with default method
        updateSelectedMethod("Cash")
    }

    /**
     * Updates the local selection state and refreshes card visuals.
     */
    private fun updateSelectedMethod(method: String) {
        Log.d(TAG, "Method selected: $method")
        selectedMethod = method
        
        // Reset all cards to default (white)
        resetMethodUI(binding.layoutCash, R.drawable.ic_payments_24dp)
        resetMethodUI(binding.layoutUpi, R.drawable.ic_payments_24dp)
        resetMethodUI(binding.layoutCard, R.drawable.ic_payments_24dp)
        resetMethodUI(binding.layoutWallet, R.drawable.ic_inventory_24dp)

        // Highlight selected card and toggle cash-only views
        binding.llCashDetailsContainer.isVisible = (method == "Cash")

        when (method) {
            "Cash" -> highlightMethodUI(binding.layoutCash)
            "UPI" -> highlightMethodUI(binding.layoutUpi)
            "Card" -> highlightMethodUI(binding.layoutCard)
            "Wallet" -> highlightMethodUI(binding.layoutWallet)
        }
    }

    private fun highlightMethodUI(itemBinding: LayoutPaymentMethodItemBinding) {
        itemBinding.cardPaymentRoot.setCardBackgroundColor(Color.parseColor("#F0FDF4")) 
        itemBinding.cardPaymentRoot.strokeColor = Color.parseColor("#BBF7D0")
        itemBinding.cardPaymentRoot.strokeWidth = 3
        itemBinding.ivPaymentIcon.setImageResource(R.drawable.success)
        itemBinding.ivPaymentIcon.colorFilter = null
    }

    private fun resetMethodUI(itemBinding: LayoutPaymentMethodItemBinding, iconRes: Int) {
        itemBinding.cardPaymentRoot.setCardBackgroundColor(Color.WHITE)
        itemBinding.cardPaymentRoot.strokeColor = Color.parseColor("#E5E7EB")
        itemBinding.cardPaymentRoot.strokeWidth = 2
        itemBinding.ivPaymentIcon.setImageResource(iconRes)
        itemBinding.ivPaymentIcon.setColorFilter(Color.parseColor("#6B7280"))
    }

    /**
     * Real-time calculation of change to be returned to customer
     */
    @SuppressLint("DefaultLocale")
    private fun setupReceivedAmountLogic() {
        binding.etReceivedAmount.addTextChangedListener {
            val receivedStr = it.toString()
            val received = receivedStr.toDoubleOrNull() ?: 0.0
            val change = maxOf(0.0, received - totalAmount)
            Log.v(TAG, "Change updated: received=$received, total=$totalAmount, change=$change")
            binding.tvChangeAmount.text = String.format("₹ %.2f", change)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun observeOrderData() {
        // Pipeline 1: Collect totals from current cart
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    val subtotal = state.cartSummary.totalPrice.toDouble()
                    val gst = subtotal * 0.05
                    totalAmount = subtotal + gst
                    Log.d(TAG, "Total updated from cart: ₹$totalAmount")
                    binding.tvTotalPayable.text = String.format("₹ %.2f", totalAmount)
                }
            }
        }
        
        // Pipeline 2: Track Firestore upload operation for order finalization
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.orderUploadStatus.collect { resource ->
                    when (resource) {
                        is ResourceUiState.Loading -> {
                            Log.d(TAG, "Operation -> Finalizing Bill in Firestore...")
                            binding.progressBar.isVisible = true
                            binding.btnGenerateBill.isEnabled = false
                        }
                        is ResourceUiState.Success -> {
                            Log.i(TAG, "🚀 Bill successfully created in Firestore. Moving to Summary.")
                            binding.progressBar.isVisible = false
                            
                            val receivedAmount = if (selectedMethod == "Cash") {
                                binding.etReceivedAmount.text.toString().toDoubleOrNull() ?: 0.0
                            } else {
                                totalAmount
                            }
                            val change = maxOf(0.0, receivedAmount - totalAmount)
                            
                            // Forward all details to Summary screen
                            val summaryFragment = CashierBillSummaryFragment.newInstance(
                                orderId = viewModel.lastOrderId ?: "N/A",
                                totalItems = viewModel.uiState.value.cartSummary.totalItems,
                                totalAmount = totalAmount,
                                method = selectedMethod,
                                received = receivedAmount,
                                change = change,
                                paidAtMillis = System.currentTimeMillis()
                            )

                            parentFragmentManager.beginTransaction()
                                .replace(this@OrderPaymentFragment.id, summaryFragment)
                                .addToBackStack(null)
                                .commit()
                        }
                        is ResourceUiState.Error -> {
                            Log.e(TAG, "❌ Bill creation failed: ${resource.message}")
                            binding.progressBar.isVisible = false
                            binding.btnGenerateBill.isEnabled = true
                            Toast.makeText(context, "Error: ${resource.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetUploadStatus()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupGenerateBillAction() {
        binding.btnGenerateBill.setOnClickListener {
            if (selectedMethod == "Cash") {
                proceedWithOrderFinalization()
            } else {
                showPaymentConfirmationDialog()
            }
        }
    }

    /**
     * Reusing the premium confirmation dialog for non-cash payments.
     */
    private fun showPaymentConfirmationDialog() {
        val method = selectedMethod
        Log.i("CashierSettleFlow", "💳 [UI] Opening confirmation dialog for Method: $method, Amount: $totalAmount")

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment_qr, null)
        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvPaymentTitle)
        val tvSubtitle = dialogView.findViewById<android.widget.TextView>(R.id.tvPaymentSubtitle)
        val ivIcon = dialogView.findViewById<android.widget.ImageView>(R.id.ivPaymentQr)
        val tvAmount = dialogView.findViewById<android.widget.TextView>(R.id.tvAmountToPay)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelPayment)
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmPayment)

        tvAmount.text = String.format("₹ %.2f", totalAmount)
        tvTitle.text = "$method Payment"

        when (method) {
            "UPI" -> {
                tvSubtitle.text = "Scan the QR code to pay"
                ivIcon.setImageResource(R.drawable.biling)
                ivIcon.colorFilter = null
            }
            "Card" -> {
                tvSubtitle.text = "Confirm transaction success on POS machine"
                ivIcon.setImageResource(R.drawable.ic_payments_24dp)
                ivIcon.setColorFilter(Color.parseColor("#3554FF"))
            }
            else -> {
                tvSubtitle.text = "Verify payment receipt on your device"
                ivIcon.setImageResource(R.drawable.ic_inventory_24dp)
                ivIcon.setColorFilter(Color.parseColor("#6B7280"))
            }
        }

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener {
            Log.i("CashierSettleFlow", "❌ Payment confirmation cancelled")
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            Log.i("CashierSettleFlow", "✅ Payment verified. Finalizing order.")
            dialog.dismiss()
            proceedWithOrderFinalization()
        }

        dialog.show()
    }

    private fun proceedWithOrderFinalization() {
        val managerId = sessionManager.getUid()
        val tableId = arguments?.getString("tableId") ?: "N/A"
        val floorId = arguments?.getString("floorId") ?: "N/A"

        Log.i(TAG, "Finalizing Bill: Manager=$managerId, Method=$selectedMethod")

        // Set the separate payment method field, keeping orderType (Takeaway) intact
        viewModel.setPaymentMethod(selectedMethod)
        
        // Finalize order as PAID immediately for Cashier/Counter flow
        viewModel.submitActiveOrderToKitchen(managerId, floorId, tableId, "", "PAID")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}