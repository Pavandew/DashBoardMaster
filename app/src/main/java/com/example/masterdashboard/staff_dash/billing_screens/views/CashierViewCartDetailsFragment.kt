package com.example.masterdashboard.staff_dash.billing_screens.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.R
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.BaseViewCartFragment
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.OrderSuccessFragment
import kotlinx.coroutines.launch

/**
 * Cashier-specific implementation of the Cart. 
 * Provides options to either pay immediately (for Take-away) or send to kitchen and pay later.
 */
class CashierViewCartDetailsFragment : BaseViewCartFragment() {

    override fun onStart() {
        super.onStart()
        // Ensure bottom navigation is hidden when viewing the cart
        (activity as? CashierHomeActivity)?.hideBottomNavigation()
    }

    override fun setupBottomButton() {
        val type = viewModel.orderType.uppercase()
        val isCounterOrder = type == "TAKE_AWAY" || type == "DELIVERY"

        // Counter orders (Take-away/Delivery) allow "Pay Later" to send the KOT immediately
        if (isCounterOrder) {
            binding.btnPayLater.visibility = View.VISIBLE
            binding.btnPayLater.text = "Send KOT (Pay Later)"
            binding.btnPayLater.setOnClickListener {
                Log.i("CashierViewCart", "User action: Pay Later selected for counter order.")
                executePayLater()
            }
            
            binding.btnSendToKitchen.text = "Proceed to Payment"
        } else {
            binding.btnPayLater.visibility = View.GONE
            binding.btnSendToKitchen.text = "Proceed to Payment"
        }

        binding.btnSendToKitchen.setOnClickListener {
            Log.d("CashierViewCart", "Navigation: Proceeding to Payment selection.")
            navigateToPayment()
        }
        
        observeUploadStatus()
    }

    /**
     * Sends the order to the kitchen without immediate payment.
     */
    private fun executePayLater() {
        val managerId = sessionManager.getUid()
        val waiterId = sessionManager.getStaffDocId()
        val tableId = arguments?.getString("tableId") ?: "COUNTER_ORDER"
        val floorId = arguments?.getString("floorId") ?: "N/A"
        val notes = binding.etOrderNotes.text.toString().trim()

        Log.i("CashierViewCart", "Process: Executing Pay Later for $tableId.")
        viewModel.submitActiveOrderToKitchen(managerId, floorId, tableId, notes, waiterId = waiterId)
    }

    private fun navigateToPayment() {
        val bundle = Bundle().apply {
            putString("tableId", arguments?.getString("tableId"))
            putString("tableName", arguments?.getString("tableName"))
            putString("floorId", arguments?.getString("floorId"))
        }

        val paymentFragment = OrderPaymentFragment().apply { arguments = bundle }
        parentFragmentManager.beginTransaction()
            .replace(R.id.billing_fragment_container, paymentFragment)
            .addToBackStack(null).commit()
    }

    /**
     * Observes the background submission process.
     */
    private fun observeUploadStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.orderUploadStatus.collect { state ->
                    when (state) {
                        is ResourceUiState.Loading -> {
                            binding.btnPayLater.isEnabled = false
                            binding.btnSendToKitchen.isEnabled = false
                        }
                        is ResourceUiState.Success -> {
                            Log.d("CashierViewCart", "KOT successfully submitted via Pay Later.")
                            Toast.makeText(context, "Order sent to kitchen!", Toast.LENGTH_SHORT).show()
                            navigateToSuccess()
                        }
                        is ResourceUiState.Error -> {
                            Log.e("CashierViewCart", "Pay Later submission failed: ${state.message}")
                            binding.btnPayLater.isEnabled = true
                            binding.btnSendToKitchen.isEnabled = true
                            Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetUploadStatus()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun navigateToSuccess() {
        val bundle = Bundle().apply {
            putString("tableId", arguments?.getString("tableId"))
            putString("tableName", arguments?.getString("tableName"))
            putString("orderId", viewModel.lastOrderId)
            putInt("totalItems", viewModel.cartSummary.value.totalItems)
            putDouble("totalPrice", viewModel.cartSummary.value.totalPrice * 1.05)
            putBoolean("isCashier", true)
        }
        
        Log.i("CashierViewCart", "Navigation: Routing to Success screen.")
        val successFragment = OrderSuccessFragment().apply { arguments = bundle }
        parentFragmentManager.beginTransaction()
            .replace(this.id, successFragment)
            .addToBackStack(null).commit()
    }
}
