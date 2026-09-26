package com.example.masterdashboard.subscription.payment

import com.example.masterdashboard.subscription.models.PaymentGateway
import com.example.masterdashboard.subscription.models.PaymentResultPayload

interface PaymentGatewayListener {
    fun onPaymentSuccess(payload: PaymentResultPayload)
    fun onPaymentFailure(gateway: PaymentGateway, code: Int, description: String?)
}
