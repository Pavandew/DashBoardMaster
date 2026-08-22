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
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment handling the payment selection and finalization for counter orders.
 * Supports Cash, UPI, Card, and Wallet payment methods.
 */
class OrderPaymentFragment : Fragment() {

    companion object {
        private const val TAG = "OrderPaymentFrag"
    }

    private var _binding: FragmentOrderPaymentBinding? = null
    private val binding get() = _binding!!

    // Uses activity-scoped ViewModel to finalize the cart and process the bill
    private val viewModel: OrderTakingViewModel by activityViewModels()
    private val sessionManager by lazy { SessionManager(requireContext()) }

    private var selectedMethod = "Cash"
    private var totalAmount = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrderPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Initializing Payment Flow.")

        setupToolbar()
        setupPaymentMethods()
        setupReceivedAmountLogic()
        observeOrderData()
        setupGenerateBillAction()
    }

    private fun setupToolbar() {
        binding.paymentToolbar.tvToolbarTitle.text = "Payment"
        binding.paymentToolbar.toolbarImgMenu.setOnClickListener {
            Log.v(TAG, "Back clicked: Returning to cart.")
            parentFragmentManager.popBackStack()
        }
        binding.paymentToolbar.toolbarImgNotification.isVisible = false
    }

    private fun setupPaymentMethods() {
        // Initialize static labels for payment cards
        binding.layoutCash.tvPaymentName.text = "Cash"
        binding.layoutUpi.tvPaymentName.text = "UPI"
        binding.layoutCard.tvPaymentName.text = "Card"
        binding.layoutWallet.tvPaymentName.text = "Wallet"

        // Set default icons
        binding.layoutCash.ivPaymentIcon.setImageResource(R.drawable.ic_payments_24dp)
        binding.layoutUpi.ivPaymentIcon.setImageResource(R.drawable.ic_payments_24dp)
        binding.layoutCard.ivPaymentIcon.setImageResource(R.drawable.ic_payments_24dp)
        binding.layoutWallet.ivPaymentIcon.setImageResource(R.drawable.ic_inventory_24dp)

        // Register click listeners for mode selection
        binding.layoutCash.root.setOnClickListener { updateSelectedMethod("Cash") }
        binding.layoutUpi.root.setOnClickListener { updateSelectedMethod("UPI") }
        binding.layoutCard.root.setOnClickListener { updateSelectedMethod("Card") }
        binding.layoutWallet.root.setOnClickListener { updateSelectedMethod("Wallet") }

        // Start with Cash as default
        updateSelectedMethod("Cash")
    }

    /**
     * Updates UI visuals when a payment method is changed.
     */
    private fun updateSelectedMethod(method: String) {
        Log.d(TAG, "Selection: Payment mode changed to $method")
        selectedMethod = method
        
        // Reset all cards to standard state
        resetMethodUI(binding.layoutCash, R.drawable.ic_payments_24dp)
        resetMethodUI(binding.layoutUpi, R.drawable.ic_payments_24dp)
        resetMethodUI(binding.layoutCard, R.drawable.ic_payments_24dp)
        resetMethodUI(binding.layoutWallet, R.drawable.ic_inventory_24dp)

        // Show/Hide cash-specific "Amount Received" input
        binding.llCashDetailsContainer.isVisible = (method == "Cash")

        // Highlight the selected card
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
     * Calculates the "Change" to return to customer when Cash is selected.
     */
    @SuppressLint("DefaultLocale")
    private fun setupReceivedAmountLogic() {
        binding.etReceivedAmount.addTextChangedListener {
            val received = it.toString().toDoubleOrNull() ?: 0.0
            val change = maxOf(0.0, received - totalAmount)
            binding.tvChangeAmount.text = String.format("₹ %.2f", change)
        }
    }

    /**
     * Observes the cart totals and the upload status from the ViewModel.
     */
    @SuppressLint("DefaultLocale")
    private fun observeOrderData() {
        // Pipeline 1: Listen for price changes in the session cart
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cartSummary.collectLatest { summary ->
                    val subtotal = summary.totalPrice.toDouble()
                    // Total = Subtotal + 5% GST
                    totalAmount = subtotal * 1.05
                    Log.d(TAG, "Sync: Bill total updated to ₹$totalAmount")
                    binding.tvTotalPayable.text = String.format("₹ %.2f", totalAmount)
                }
            }
        }
        
        // Pipeline 2: Track order submission result (Firestore write)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.orderUploadStatus.collect { resource ->
                    when (resource) {
                        is ResourceUiState.Loading -> {
                            Log.d(TAG, "Submission: Finalizing record in Firestore...")
                            binding.progressBar.isVisible = true
                            binding.btnGenerateBill.isEnabled = false
                        }
                        is ResourceUiState.Success -> {
                            Log.i(TAG, "Submission: Success! Navigating to Summary screen.")
                            binding.progressBar.isVisible = false
                            
                            val receivedAmount = if (selectedMethod == "Cash") {
                                binding.etReceivedAmount.text.toString().toDoubleOrNull() ?: 0.0
                            } else {
                                totalAmount
                            }
                            
                            // Open Receipt Summary Fragment
                            val summaryFragment = CashierBillSummaryFragment.newInstance(
                                orderId = viewModel.lastOrderId ?: "N/A",
                                totalItems = viewModel.cartSummary.value.totalItems,
                                totalAmount = totalAmount,
                                method = selectedMethod,
                                received = receivedAmount,
                                change = maxOf(0.0, receivedAmount - totalAmount),
                                paidAtMillis = System.currentTimeMillis()
                            )

                            parentFragmentManager.beginTransaction()
                                .replace(this@OrderPaymentFragment.id, summaryFragment)
                                .addToBackStack(null)
                                .commit()
                        }
                        is ResourceUiState.Error -> {
                            Log.e(TAG, "Submission: Failed - ${resource.message}")
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
            Log.d(TAG, "Action: Generate Bill clicked for mode: $selectedMethod")
            if (selectedMethod == "Cash") {
                proceedWithOrderFinalization()
            } else {
                // Show QR/Verification dialog for digital payments
                showPaymentConfirmationDialog()
            }
        }
    }

    /**
     * Logic for UPI/Card/Wallet confirmation.
     */
    private fun showPaymentConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment_qr, null)
        val tvAmount = dialogView.findViewById<android.widget.TextView>(R.id.tvAmountToPay)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelPayment)
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmPayment)

        tvAmount.text = String.format("₹ %.2f", totalAmount)
        
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            Log.i(TAG, "Digital Payment: User confirmed transaction success.")
            dialog.dismiss()
            proceedWithOrderFinalization()
        }
        dialog.show()
    }

    /**
     * Triggers the actual Firestore write via the ViewModel.
     */
    private fun proceedWithOrderFinalization() {
        val managerId = sessionManager.getUid()
        val waiterId = sessionManager.getStaffDocId() // Stores current staff ID
        val tableId = arguments?.getString("tableId") ?: "N/A"
        val floorId = arguments?.getString("floorId") ?: "N/A"

        Log.i(TAG, "Process: Finalizing PAID order for $tableId")
        viewModel.setPaymentMethod(selectedMethod)
        viewModel.submitActiveOrderToKitchen(managerId, floorId, tableId, "", "PAID", waiterId = waiterId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.v(TAG, "onDestroyView: Cleaning up view binding.")
        _binding = null
    }
}
