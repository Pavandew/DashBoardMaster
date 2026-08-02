package com.example.masterdashboard.staff_dash.billing_screens.views

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentCashierOrderBinding
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.WaiterOrderTakingFragment // Not using old one anymore

class CashierOrderFragment : Fragment() {

    companion object {
        private const val TAG = "CashierOrderFragment"
    }

    private var _binding: FragmentCashierOrderBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel to pass data to OrderTaking flow
    private val viewModel: OrderTakingViewModel by activityViewModels {
        OrderTakingViewModel.OrderViewModelFactory(OrderTakingRepository())
    }

    private var selectedOrderType = "DINE_IN"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Inflating layout for Cashier New Order screen")
        _binding = FragmentCashierOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Initializing New Order form for Cashier")

        setupToolbar()
        setupClickListeners()
        selectOrderType("TAKE_AWAY") // Default selection
    }

    private fun setupToolbar() {
        binding.orderToolbar.tvToolbarTitle.text = "New Order"
        binding.orderToolbar.toolbarImgMenu.visibility = View.GONE
        binding.orderToolbar.toolbarImgNotification.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        Log.v(TAG, "onStart: Ensuring bottom navigation is visible")
        (activity as? CashierHomeActivity)?.showBottomNavigation()
    }

    private fun setupClickListeners() {
        binding.cardDineIn.setOnClickListener { 
            Log.d(TAG, "User selected DINE_IN order type")
            selectOrderType("DINE_IN") 
        }
        binding.cardTakeAway.setOnClickListener { 
            Log.d(TAG, "User selected TAKE_AWAY order type")
            selectOrderType("TAKE_AWAY") 
        }
        binding.cardDelivery.setOnClickListener { 
            Log.d(TAG, "User selected DELIVERY order type")
            selectOrderType("DELIVERY") 
        }

        binding.btnStartOrdering.setOnClickListener {
            val name = binding.etCustomerName.text.toString().trim()
            val phone = binding.etPhoneNumber.text.toString().trim()
            
            Log.i(TAG, "Start Ordering Clicked: Name='$name', Phone='$phone', Type='$selectedOrderType'")
            
            /**
             * [SHARED STATE]
             * Store details in Shared ViewModel for OrderTaking flow to pick up later.
             * This avoids passing complex objects through bundles.
             */
            viewModel.setCustomerDetails(name, phone, selectedOrderType)
            viewModel.clearCart() // Reset previous session items if any

            navigateToOrderTaking()
        }
    }

    /**
     * Updates the UI selection state for order type cards
     */
    private fun selectOrderType(type: String) {
        selectedOrderType = type
        
        // Reset all visual states
        resetCardUI(binding.cardDineIn, binding.ivDineIn, binding.tvDineIn)
        resetCardUI(binding.cardTakeAway, binding.ivTakeAway, binding.tvTakeAway)
        resetCardUI(binding.cardDelivery, binding.ivDelivery, binding.tvDelivery)

        // Highlight the active selection
        when (type) {
            "DINE_IN" -> highlightCardUI(binding.cardDineIn, binding.ivDineIn, binding.tvDineIn)
            "TAKE_AWAY" -> highlightCardUI(binding.cardTakeAway, binding.ivTakeAway, binding.tvTakeAway)
            "DELIVERY" -> highlightCardUI(binding.cardDelivery, binding.ivDelivery, binding.tvDelivery)
        }
    }

    private fun highlightCardUI(card: com.google.android.material.card.MaterialCardView, icon: android.widget.ImageView, text: android.widget.TextView) {
        card.setCardBackgroundColor(Color.parseColor("#F5F3FF"))
        card.strokeColor = Color.parseColor("#6366F1")
        card.strokeWidth = 4
        icon.setColorFilter(Color.parseColor("#6366F1"))
        text.setTextColor(Color.parseColor("#111827"))
    }

    private fun resetCardUI(card: com.google.android.material.card.MaterialCardView, icon: android.widget.ImageView, text: android.widget.TextView) {
        card.setCardBackgroundColor(Color.WHITE)
        card.strokeColor = Color.parseColor("#E5E7EB")
        card.strokeWidth = 2
        icon.setColorFilter(Color.parseColor("#6B7280"))
        text.setTextColor(Color.parseColor("#6B7280"))
    }

    /**
     * Navigates to the item selection screen using a special Table ID for Counter orders.
     */
    private fun navigateToOrderTaking() {
        // HIDE IMMEDIATELY
//        (activity as? CashierHomeActivity)?.hideBottomNavigation()

        val bundle = Bundle().apply {
            putString("tableId", "COUNTER_ORDER") 
            putString("tableName", "Counter")
            putString("status", "FREE")
        }

        val orderTakingFragment = CashierOrderTakingFragment().apply {
            arguments = bundle
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.billing_fragment_container, orderTakingFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}