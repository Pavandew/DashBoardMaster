package com.example.masterdashboard.staff_dash.billing_screens.views

import android.os.Bundle
import com.example.masterdashboard.R
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.table.views.BaseViewCartFragment

/**
 * Cashier specific Cart. Clicking the button proceeds to the payment screen.
 */
class CashierViewCartDetailsFragment : BaseViewCartFragment() {

    override fun onStart() {
        super.onStart()
        (activity as? CashierHomeActivity)?.hideBottomNavigation()
    }

    override fun setupBottomButton() {
        binding.btnSendToKitchen.text = "Proceed to Payment"
        binding.btnSendToKitchen.setOnClickListener {
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
    }
}