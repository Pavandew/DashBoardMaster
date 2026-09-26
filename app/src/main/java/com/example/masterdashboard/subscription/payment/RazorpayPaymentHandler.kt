package com.example.masterdashboard.subscription.payment

import android.app.Activity
import android.util.Log
import com.example.masterdashboard.subscription.models.PaymentGateway
import com.example.masterdashboard.subscription.models.PaymentOrderDetails
import com.example.masterdashboard.subscription.models.PaymentResultPayload
import com.example.masterdashboard.utils.AppConstants
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

class RazorpayPaymentHandler(
    private val activity: Activity,
    private val listener: PaymentGatewayListener
) : PaymentResultWithDataListener {

    companion object {
        private const val TAG = "RazorpayPaymentHandler"
        private const val RAZORPAY_KEY_ID = AppConstants.RAZORPAY_TEST_KEY_ID
    }

    fun startPayment(orderDetails: PaymentOrderDetails, userPhone: String = "", userEmail: String = "") {
        try {
            Log.d(TAG, "startPayment: Preparing Razorpay Checkout for Order ID '${orderDetails.orderId}', Amount=${orderDetails.numericPrice}")
            Checkout.preload(activity.applicationContext)

            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY_ID)

            val options = JSONObject().apply {
                put("name", "RestroOne")
                put("description", "Subscription: ${orderDetails.planTitle}")
                put("theme.color", "#1E88E5")
                put("currency", orderDetails.currency)
                put("amount", (orderDetails.numericPrice * 100).toInt())

                if (orderDetails.orderId.isNotEmpty() && !orderDetails.orderId.startsWith("MOCK_ORDER_")) {
                    put("order_id", orderDetails.orderId)
                }

                val prefill = JSONObject().apply {
                    if (userEmail.isNotEmpty()) put("email", userEmail)
                    if (userPhone.isNotEmpty()) put("contact", userPhone)
                }
                put("prefill", prefill)

                val retryObj = JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 2)
                }
                put("retry", retryObj)
            }

            Log.i(TAG, "startPayment: Opening Razorpay Checkout Activity...")
            checkout.open(activity, options)
        } catch (e: Exception) {
            Log.e(TAG, "startPayment: Failed to initiate Razorpay checkout", e)
            listener.onPaymentFailure(PaymentGateway.RAZORPAY, -1, e.message ?: "Initialization error")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val paymentId = razorpayPaymentId ?: paymentData?.paymentId ?: ""
        val orderId = paymentData?.orderId ?: ""
        val signature = paymentData?.signature ?: ""

        Log.i(TAG, "onPaymentSuccess: Payment successful! PaymentID=$paymentId, OrderID=$orderId, SignatureLen=${signature.length}")

        val payload = PaymentResultPayload(
            paymentId = paymentId,
            orderId = orderId,
            signature = signature,
            gateway = PaymentGateway.RAZORPAY,
            rawResponse = paymentData?.data?.toString()
        )
        listener.onPaymentSuccess(payload)
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        Log.e(TAG, "onPaymentError: Code=$code, Response=$response, PaymentID=${paymentData?.paymentId}")
        listener.onPaymentFailure(PaymentGateway.RAZORPAY, code, response)
    }
}
