package com.example.masterdashboard.staff_dash.waiter_screens.table.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentOrderSuccesBinding
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderSuccessFragment : Fragment() {

    companion object {
        private const val TAG = "OrderSuccessFragment"
    }

    private var _binding: FragmentOrderSuccesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderTakingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderSuccesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: OrderSuccessFragment Opened")
        Log.d(TAG, "onViewCreated: Displaying success screen.")

        (activity as? WaiterHomeActivity)?.hideBottomNavigation()

        // NEW: Clear the cart and reset the upload status now that we are safely on the success screen.
        // This prevents the cart's "auto-pop" logic or "Sending..." states from interfering with navigation.
        viewModel.clearCart()
        viewModel.resetUploadStatus()

        setupSystemBackPress() // FIXED: Added custom hardware back press interceptor
        populateReceiptForm()
        setupNavigationActions()
    }

    // FIXED: Intercepts the phone's physical back button or gesture swipe
    private fun setupSystemBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d(TAG, "Back pressed: Navigating back to Tables screen.")
                    navigateToTablesScreenCleanly()
                }
            }
        )
    }

    private fun populateReceiptForm() {
        val tableName = arguments?.getString("tableName") ?: "N/A"
        val orderId = arguments?.getString("orderId") ?: "#ORD-0000"
        val totalItems = arguments?.getInt("totalItems") ?: 0
        val totalPrice = arguments?.getDouble("totalPrice") ?: 0.0

        val currentTimestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        binding.tvSuccessTableId.text = "T-$tableName"
        binding.tvSuccessOrderId.text = orderId
        binding.tvSuccessTotalItems.text = totalItems.toString()
        binding.tvSuccessTotalAmount.text = getString(R.string.currency_symbol) + " ${String.format("%.2f", totalPrice)}"
        binding.tvSuccessTimestamp.text = currentTimestamp
    }

    private fun setupNavigationActions() {
        binding.btnViewActiveOrders.setOnClickListener {
            Log.d(TAG, "Navigating to Active Orders screen.")
            // 1. Clear out the shared cart counter state arrays
            viewModel.clearCart()
            // 2. Clear out the entire ordering fragment session history
            parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            // 3. Open Orders through the activity to handle bottom nav and currentTag correctly
            (activity as? WaiterHomeActivity)?.openOrders()
        }

        binding.btnBackToTables.setOnClickListener {
            Log.d(TAG, "Back to Tables clicked.")
            navigateToTablesScreenCleanly()
        }
    }

    // FIXED: Centralized clean navigation utility function
    private fun navigateToTablesScreenCleanly() {
        // 1. Clear out the shared cart counter state arrays
        viewModel.clearCart()

        // 2. Clear out the entire ordering fragment session history
        parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}