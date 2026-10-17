const crypto = require("node:crypto");

const {
    db,
    HttpsError,
    onAppCheckCall,
    getRazorpayKeyId,
    getRazorpayKeySecret,
    numericPrice
} = require("./helpers");

exports.completeRestaurantSetup = onAppCheckCall(async (request) => {
    if (!request.auth || !request.auth.uid) {
        throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const { restaurantId, restaurantName } = request.data || {};
    if (!restaurantId || typeof restaurantName !== "string" || !restaurantName.trim()) {
        throw new HttpsError("invalid-argument", "Restaurant ID and name are required.");
    }

    const userRef = db.collection("users").doc(request.auth.uid);
    const restaurantRef = db.collection("restaurants").doc(restaurantId);
    await db.runTransaction(async (transaction) => {
        const [userSnapshot, restaurantSnapshot] = await Promise.all([
            transaction.get(userRef),
            transaction.get(restaurantRef)
        ]);
        if (!userSnapshot.exists
            || !["owner_single", "owner_multi"].includes(userSnapshot.get("role"))
            || !restaurantSnapshot.exists
            || restaurantSnapshot.get("ownerUid") !== request.auth.uid) {
            throw new HttpsError("permission-denied", "You do not own this restaurant.");
        }

        const setup = {
            isSetupComplete: true,
            restaurantId: restaurantId,
            restaurantName: restaurantName.trim()
        };
        if (!userSnapshot.get("subscription")) {
            const now = Date.now();
            const trial = {
                status: "TRIAL",
                planId: "single_res_trial",
                startDateTimestamp: now,
                expiryDateTimestamp: now + 30 * 24 * 60 * 60 * 1000,
                autoRenew: true
            };
            setup.subscription = trial;
            transaction.set(restaurantRef, { subscription: trial }, { merge: true });
        }
        transaction.set(userRef, setup, { merge: true });
    });
    return { success: true };
});

exports.createSubscriptionPaymentOrder = onAppCheckCall(
    async (request) => {
        const { planId, restaurantId } = request.data || {};
        const plans = {
            single_res_monthly: { billingCycle: "MONTHLY", amountRupees: 299, maxAllowedOutlets: 1 },
            single_res_yearly: { billingCycle: "YEARLY", amountRupees: 2999, maxAllowedOutlets: 1 }
        };
        const plan = plans[planId];
        if (!plan || !restaurantId || !request.auth?.uid) {
            throw new HttpsError("invalid-argument", "A supported plan and restaurant are required.");
        }
        const [user, restaurant] = await Promise.all([
            db.collection("users").doc(request.auth.uid).get(),
            db.collection("restaurants").doc(restaurantId).get()
        ]);
        if (!user.exists || !restaurant.exists
            || !["owner_single", "owner_multi"].includes(user.get("role"))
            || user.get("status") !== "Active"
            || user.get("isVerified") !== true
            || restaurant.get("ownerUid") !== request.auth.uid) {
            throw new HttpsError("permission-denied", "Only the verified restaurant owner can subscribe.");
        }
        const keyId = getRazorpayKeyId();
        const keySecret = getRazorpayKeySecret();
        if (!keyId || !keySecret) {
            throw new HttpsError("failed-precondition", "Razorpay server credentials are not configured.");
        }
        const basic = Buffer.from(`${keyId}:${keySecret}`).toString("base64");
        const response = await fetch("https://api.razorpay.com/v1/orders", {
            method: "POST",
            headers: { Authorization: `Basic ${basic}`, "Content-Type": "application/json" },
            body: JSON.stringify({
                amount: plan.amountRupees * 100,
                currency: "INR",
                receipt: `sub_${request.auth.uid}_${Date.now()}`
            })
        });
        if (!response.ok) {
            throw new HttpsError("unavailable", "Razorpay could not create a payment order.");
        }
        const gatewayOrder = await response.json();
        await db.collection("subscriptionPaymentOrders").doc(gatewayOrder.id).create({
            ownerUid: request.auth.uid,
            restaurantId,
            planId,
            amountPaise: plan.amountRupees * 100,
            currency: "INR",
            gateway: "RAZORPAY",
            status: "CREATED",
            createdAt: Date.now()
        });
        return {
            orderId: gatewayOrder.id,
            keyId: keyId,
            planId,
            restaurantId,
            numericPrice: plan.amountRupees,
            currency: "INR"
        };
    }
);

exports.verifyAndActivateSubscription = onAppCheckCall(
    async (request) => {
        const { paymentId, orderId, signature, gateway, planId: requestedPlanId, restaurantId: requestedRestaurantId } = request.data || {};
        if (gateway !== "RAZORPAY" || !paymentId || !orderId || !signature) {
            throw new HttpsError("failed-precondition", "Only server-verifiable Razorpay payments are supported.");
        }
        const keyId = getRazorpayKeyId();
        const keySecret = getRazorpayKeySecret();
        if (!request.auth?.uid || !keyId || !keySecret) {
            throw new HttpsError("failed-precondition", "Payment verification is not configured.");
        }
        const paymentOrderRef = db.collection("subscriptionPaymentOrders").doc(orderId);
        const paymentOrder = await paymentOrderRef.get();
        if (!paymentOrder.exists
            || paymentOrder.get("ownerUid") !== request.auth.uid
            || paymentOrder.get("status") !== "CREATED"
            || paymentOrder.get("planId") !== requestedPlanId
            || paymentOrder.get("restaurantId") !== requestedRestaurantId) {
            throw new HttpsError("permission-denied", "Payment order is invalid or already consumed.");
        }
        const expected = crypto.createHmac("sha256", keySecret)
            .update(`${orderId}|${paymentId}`).digest("hex");
        const providedBytes = Buffer.from(String(signature));
        const expectedBytes = Buffer.from(expected);
        if (providedBytes.length !== expectedBytes.length
            || !crypto.timingSafeEqual(providedBytes, expectedBytes)) {
            throw new HttpsError("permission-denied", "Razorpay signature verification failed.");
        }
        const basic = Buffer.from(`${keyId}:${keySecret}`).toString("base64");
        const response = await fetch(
            `https://api.razorpay.com/v1/payments/${encodeURIComponent(paymentId)}`,
            { headers: { Authorization: `Basic ${basic}` } }
        );
        if (!response.ok) {
            throw new HttpsError("unavailable", "Razorpay payment status could not be verified.");
        }
        const payment = await response.json();
        if (payment.order_id !== orderId
            || payment.status !== "captured"
            || payment.amount !== paymentOrder.get("amountPaise")
            || payment.currency !== "INR") {
            throw new HttpsError("permission-denied", "Payment is not captured for this subscription order.");
        }
        const plans = {
            single_res_monthly: { billingCycle: "MONTHLY", amountRupees: 299, maxAllowedOutlets: 1 },
            single_res_yearly: { billingCycle: "YEARLY", amountRupees: 2999, maxAllowedOutlets: 1 }
        };
        const planId = paymentOrder.get("planId");
        const plan = plans[planId];
        if (!plan) throw new HttpsError("failed-precondition", "Payment is linked to an unsupported plan.");

        const userRef = db.collection("users").doc(request.auth.uid);
        const restaurantRef = db.collection("restaurants").doc(requestedRestaurantId);
        await db.runTransaction(async (transaction) => {
            const [userSnapshot, restaurantSnapshot] = await Promise.all([
                transaction.get(userRef),
                transaction.get(restaurantRef)
            ]);
            if (!userSnapshot.exists
                || !["owner_single", "owner_multi"].includes(userSnapshot.get("role"))
                || !restaurantSnapshot.exists
                || restaurantSnapshot.get("ownerUid") !== request.auth.uid) {
                throw new HttpsError("permission-denied", "Subscription destination restaurant is invalid.");
            }
            const now = Date.now();
            const durationDays = plan.billingCycle === "YEARLY" ? 365 : 30;
            const subscription = {
                status: "ACTIVE",
                planId,
                billingCycle: plan.billingCycle,
                amountRupees: plan.amountRupees,
                paymentId,
                orderId,
                startDateTimestamp: now,
                expiryDateTimestamp: now + durationDays * 24 * 60 * 60 * 1000,
                activatedAt: now
            };
            transaction.set(restaurantRef, { subscription }, { merge: true });
            transaction.set(userRef, { subscription }, { merge: true });
            transaction.set(paymentOrderRef, { status: "CONSUMED", consumedAt: now }, { merge: true });
        });
        return { success: true };
    }
);
