package com.example.masterdashboard.staff_dash.waiter_screens.table.views

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.R
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Waiter-specific implementation of the Cart. 
 * Clicking the main button sends a KOT (Kitchen Order Ticket) to the chefs.
 */
class WaiterViewCartDetailsFragment : BaseViewCartFragment() {

    override fun onStart() {
        super.onStart()
        // Ensure bottom navigation is hidden when viewing the cart
        (activity as? WaiterHomeActivity)?.hideBottomNavigation()
    }

    override fun setupBottomButton() {
        binding.btnSendToKitchen.text = "Send to Kitchen"
        binding.btnSendToKitchen.setOnClickListener {
            val managerId = sessionManager.getUid()
            val waiterId = sessionManager.getStaffDocId() 
            val tableId = arguments?.getString("tableId") ?: "N/A"
            val floorId = arguments?.getString("floorId") ?: "N/A"
            val notes = binding.etOrderNotes.text.toString().trim()

            Log.i("WaiterViewCart", "User action: Sending order for Table $tableId to Kitchen.")
            viewModel.submitActiveOrderToKitchen(managerId, floorId, tableId, notes, waiterId = waiterId)
        }

        observeUploadStatus()
    }

    /**
     * Observes the submission process and updates button states accordingly.
     */
    private fun observeUploadStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.orderUploadStatus.collect { state ->
                    when (state) {
                        is ResourceUiState.Loading -> {
                            binding.btnSendToKitchen.isEnabled = false
                            binding.btnSendToKitchen.text = "Sending..."
                        }
                        is ResourceUiState.Success -> {
                            Log.d("WaiterViewCart", "Order successfully accepted by server.")
                            binding.btnSendToKitchen.isEnabled = true
                            Toast.makeText(context, "Order sent to kitchen!", Toast.LENGTH_SHORT).show()
                            navigateToSuccess()
                        }
                        is ResourceUiState.Error -> {
                            Log.e("WaiterViewCart", "Order submission failed: ${state.message}")
                            binding.btnSendToKitchen.isEnabled = true
                            binding.btnSendToKitchen.text = "Send to Kitchen"
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
            putBoolean("isCashier", false)
        }
        
        Log.i("WaiterViewCart", "Navigation: Routing to Success screen for order: ${viewModel.lastOrderId}")
        val successFragment = OrderSuccessFragment().apply { arguments = bundle }
        parentFragmentManager.beginTransaction()
            .replace(this.id, successFragment)
            .addToBackStack(null).commit()
    }
}
