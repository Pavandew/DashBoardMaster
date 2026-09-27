package com.restroone.app.subscription.payment

import com.restroone.app.subscription.models.PaymentGateway
import com.restroone.app.subscription.models.PaymentResultPayload

interface PaymentGatewayListener {
    fun onPaymentSuccess(payload: PaymentResultPayload)
    fun onPaymentFailure(gateway: PaymentGateway, code: Int, description: String?)
}
