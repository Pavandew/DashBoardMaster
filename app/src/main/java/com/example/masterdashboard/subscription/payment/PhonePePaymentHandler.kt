package com.example.masterdashboard.subscription.payment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.masterdashboard.subscription.models.PaymentGateway
import com.example.masterdashboard.subscription.models.PaymentOrderDetails
import com.example.masterdashboard.subscription.models.PaymentResultPayload
import com.example.masterdashboard.utils.AppConstants

class PhonePePaymentHandler(
    private val activity: Activity,
    private val listener: PaymentGatewayListener,
) {

    companion object {
        private const val TAG = "PhonePePaymentHandler"
        private const val PHONEPE_PACKAGE = "com.phonepe.app"
        private const val MERCHANT_VPA = AppConstants.PHONEPE_TEST_VPA
    }

    fun startPayment(orderDetails: PaymentOrderDetails) {
        try {
            Log.d(
                TAG,
                "startPayment: Initiating PhonePe payment for Order '${orderDetails.orderId}', Amount ₹${orderDetails.numericPrice}"
            )

            val txnId = if (orderDetails.orderId.isNotEmpty()) orderDetails.orderId else "TXN_${System.currentTimeMillis()}"
            val upiUri = Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", MERCHANT_VPA)
                .appendQueryParameter("pn", "RestroOne")
                .appendQueryParameter("mc", "5812")
                .appendQueryParameter("tr", txnId)
                .appendQueryParameter("tn", "Subscription Pass: ${orderDetails.planTitle}")
                .appendQueryParameter("am", "%.2f".format(orderDetails.numericPrice))
                .appendQueryParameter("cu", "INR")
                .build()

            val intent = Intent(Intent.ACTION_VIEW, upiUri).apply {
                setPackage(PHONEPE_PACKAGE)
            }

            if (intent.resolveActivity(activity.packageManager) != null) {
                Log.i(TAG, "startPayment: Launching PhonePe App...")
                activity.startActivityForResult(intent, 2001)
            } else {
                Log.w(TAG, "startPayment: PhonePe app not installed. Fallback to general UPI intent...")
                val generalUpiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                activity.startActivityForResult(Intent.createChooser(generalUpiIntent, "Pay via UPI / PhonePe"), 2001)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startPayment: Failed to launch PhonePe payment", e)
            listener.onPaymentFailure(PaymentGateway.PHONEPE, -1, e.message ?: "PhonePe launch error")
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?, orderDetails: PaymentOrderDetails) {
        if (requestCode == 2001) {
            Log.d(TAG, "handleActivityResult: Received PhonePe callback. ResultCode=$resultCode")
            val response = data?.getStringExtra("response") ?: ""
            val status = parseUpiResponse(response)

            if (resultCode == Activity.RESULT_OK || status.equals("SUCCESS", ignoreCase = true)) {
                val txnId = extractParam(response, "txnId")?.ifEmpty { "PP_${System.currentTimeMillis()}" } ?: "PP_${System.currentTimeMillis()}"
                val payload = PaymentResultPayload(
                    paymentId = txnId,
                    orderId = orderDetails.orderId.ifEmpty { txnId },
                    signature = "PHONEPE_UPI_VERIFIED",
                    gateway = PaymentGateway.PHONEPE,
                    rawResponse = response
                )
                Log.i(TAG, "handleActivityResult: PhonePe payment success. TxnID=$txnId")
                listener.onPaymentSuccess(payload)
            } else {
                Log.e(TAG, "handleActivityResult: PhonePe payment failed or cancelled. Response=$response")
                listener.onPaymentFailure(PaymentGateway.PHONEPE, resultCode, "Payment cancelled or failed")
            }
        }
    }

    private fun parseUpiResponse(response: String): String {
        return extractParam(response, "Status") ?: "FAILED"
    }

    private fun extractParam(response: String, key: String): String? {
        val pairs = response.split("&")
        for (pair in pairs) {
            val parts = pair.split("=")
            if (parts.size == 2 && parts[0].equals(key, ignoreCase = true)) {
                return parts[1]
            }
        }
        return null
    }
}
