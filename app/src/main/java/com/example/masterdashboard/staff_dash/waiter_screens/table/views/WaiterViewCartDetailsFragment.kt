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
import kotlinx.coroutines.launch

/**
 * Waiter specific Cart. Clicking the button sends the order to the kitchen.
 */
class WaiterViewCartDetailsFragment : BaseViewCartFragment() {

    override fun onStart() {
        super.onStart()
        (activity as? WaiterHomeActivity)?.hideBottomNavigation()
    }

    override fun setupBottomButton() {
        binding.btnSendToKitchen.text = "Send to Kitchen"
        binding.btnSendToKitchen.setOnClickListener {
            val managerId = sessionManager.getUid()
            val tableId = arguments?.getString("tableId") ?: "N/A"
            val floorId = arguments?.getString("floorId") ?: "N/A"
            val notes = binding.etOrderNotes.text.toString().trim()

            viewModel.submitActiveOrderToKitchen(managerId, floorId, tableId, notes)
        }

        observeUploadStatus()
    }

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
                            binding.btnSendToKitchen.isEnabled = true
                            Toast.makeText(context, "Order sent to kitchen!", Toast.LENGTH_SHORT).show()
                            navigateToSuccess()
                        }
                        is ResourceUiState.Error -> {
                            binding.btnSendToKitchen.isEnabled = true
                            binding.btnSendToKitchen.text = "Send to Kitchen"
                            Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
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
            putInt("totalItems", viewModel.uiState.value.cartSummary.totalItems)
            putDouble("totalPrice", viewModel.uiState.value.cartSummary.totalPrice * 1.05)
            putBoolean("isCashier", false)
        }
        val successFragment = OrderSuccessFragment().apply { arguments = bundle }
        parentFragmentManager.beginTransaction()
            .replace(this.id, successFragment)
            .addToBackStack(null).commit()
    }
}