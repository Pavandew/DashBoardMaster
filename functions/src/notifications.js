const { FieldValue } = require("firebase-admin/firestore");

const {
    admin,
    db,
    HttpsError,
    onAppCheckCall,
    normalizeOrderPath,
    assertRestaurantRole
} = require("./helpers");

async function getNotificationTokens(restaurantId, spec) {
    if (spec.targetStaffId) {
        const snapshot = await db.collection("restaurants").doc(restaurantId)
            .collection("staff").doc(spec.targetStaffId).get();
        return snapshot.exists && snapshot.get("status") === "Active"
            ? [snapshot.get("fcmToken")].filter((token) => typeof token === "string" && token)
            : [];
    }

    if (spec.targetRole === "manager") {
        const [restaurantSnapshot, staffSnapshot] = await Promise.all([
            db.collection("restaurants").doc(restaurantId).get(),
            db.collection("restaurants").doc(restaurantId).collection("staff")
                .where("role", "in", ["manager", "Manager"])
                .get()
        ]);
        const ownerUid = restaurantSnapshot.get("ownerUid");
        const ownerSnapshot = ownerUid
            ? await db.collection("users").doc(ownerUid).get()
            : null;
        return [
            restaurantSnapshot.get("fcmToken"),
            ownerSnapshot?.get("fcmToken"),
            ...staffSnapshot.docs
                .filter((staff) => staff.get("status") === "Active")
                .map((staff) => staff.get("fcmToken"))
        ].filter((token) => typeof token === "string" && token);
    }

    const rolesByTarget = {
        kitchen: ["chef", "Chef", "master chef", "Master Chef", "kitchen", "Kitchen", "chef_staff"],
        billing: ["cashier", "Cashier", "billing", "Billing"],
        waiter: ["waiter", "Waiter", "head waiter", "Head Waiter", "waiter_staff"]
    };
    const roles = rolesByTarget[spec.targetRole];
    if (!roles) {
        throw new HttpsError("internal", "Notification target role is invalid.");
    }
    const snapshot = await db.collection("restaurants").doc(restaurantId).collection("staff")
        .where("role", "in", roles)
        .get();
    return snapshot.docs
        .filter((staff) => staff.get("status") === "Active")
        .map((staff) => staff.get("fcmToken"))
        .filter((token) => typeof token === "string" && token);
}

exports.createStaffNotification = onAppCheckCall(async (request) => {
    const { restaurantId, event, orderDocPath, inventoryId } = request.data || {};
    if (typeof restaurantId !== "string" || !restaurantId) {
        throw new HttpsError("invalid-argument", "Restaurant ID is required.");
    }

    const rolesByEvent = {
        KITCHEN_NEW_ORDER: ["waiter", "head waiter", "waiter_staff"],
        BILL_REQUEST: ["waiter", "head waiter", "waiter_staff"],
        ORDER_READY: ["chef", "master chef", "kitchen", "chef_staff"],
        INVENTORY_UPDATE: ["chef", "master chef", "kitchen", "chef_staff"],
        PAYMENT_SUCCESS: ["cashier", "billing"]
    };
    const allowedRoles = rolesByEvent[event];
    if (!allowedRoles) {
        throw new HttpsError("invalid-argument", "Notification event is invalid.");
    }
    const actor = await assertRestaurantRole(request, restaurantId, allowedRoles);
    if (actor.role === "owner" || actor.role === "manager") {
        throw new HttpsError("permission-denied", "This notification event is reserved for its operational role.");
    }

    const notificationsRef = db.collection("restaurants").doc(restaurantId)
        .collection("notifications");
    let notificationSpecs;
    if (event === "INVENTORY_UPDATE") {
        if (typeof inventoryId !== "string" || !inventoryId || inventoryId.includes("/")) {
            throw new HttpsError("invalid-argument", "Inventory item ID is required.");
        }
        const itemSnapshot = await db.collection("restaurants").doc(restaurantId)
            .collection("inventory").doc(inventoryId).get();
        if (!itemSnapshot.exists) {
            throw new HttpsError("not-found", "Inventory item not found.");
        }
        const item = itemSnapshot.data();
        const quantity = Number(item.itemQuantity);
        if (!Number.isFinite(quantity)) {
            throw new HttpsError("failed-precondition", "Inventory quantity is invalid.");
        }
        const status = item.manualStatus && item.manualStatus !== "AUTO"
            ? String(item.manualStatus)
            : quantity <= 0
                ? "Out of Stock"
                : quantity <= Number(item.minThreshold || 0)
                    ? "Low Stock"
                    : "In Stock";
        const updatedAt = String(item.lastUpdated || "");
        const notificationId = `inventory_${Buffer.from(
            `${inventoryId}:${updatedAt}`
        ).toString("base64url")}`;
        notificationSpecs = [{
            id: notificationId,
            title: `Inventory ${status}: ${String(item.itemName || "item")}`,
            message: `${String(actor.user.fullName || "Kitchen staff")} updated ${String(item.itemName || "an item")}: ${quantity} ${String(item.itemUnit || "")}.`,
            type: "INFORMATIONAL",
            targetRole: "manager",
            targetStaffId: "",
            tableName: "",
            orderId: "",
            orderDocPath: ""
        }];
    } else {
        if (typeof orderDocPath !== "string") {
            throw new HttpsError("invalid-argument", "Order path is required.");
        }
        const isPaymentEvent = event === "PAYMENT_SUCCESS";
        const orderRef = isPaymentEvent
            ? db.doc(orderDocPath)
            : normalizeOrderPath(orderDocPath, restaurantId);
        if (isPaymentEvent
            && (!orderDocPath.startsWith(`restaurants/${restaurantId}/completed_orders/`)
                || orderDocPath.slice(`restaurants/${restaurantId}/completed_orders/`.length).includes("/"))) {
            throw new HttpsError("permission-denied", "Completed-order path is outside this restaurant.");
        }
        const orderSnapshot = await orderRef.get();
        if (!orderSnapshot.exists || orderSnapshot.get("restaurantId") !== restaurantId) {
            throw new HttpsError("not-found", "Restaurant order not found.");
        }
        const order = orderSnapshot.data();
        const orderId = String(order.orderId || orderRef.id);
        const tableName = String(order.tableName || "Order");
        const notificationId = `order_${Buffer.from(`${event}:${orderId}`).toString("base64url")}`;
        if (event === "KITCHEN_NEW_ORDER" || event === "BILL_REQUEST") {
            const validStatus = event === "KITCHEN_NEW_ORDER"
                ? ["PENDING", "PREPARING", "READY"].includes(String(order.orderStatus || "").toUpperCase())
                : String(order.orderStatus || "").toUpperCase() === "BILLING";
            if (order.waiterId !== request.auth.uid || !validStatus) {
                throw new HttpsError("failed-precondition", "Order state does not authorize this notification.");
            }
            notificationSpecs = [{
                id: notificationId,
                title: event === "KITCHEN_NEW_ORDER" ? "New Order Received" : `Bill Requested: ${tableName}`,
                message: event === "KITCHEN_NEW_ORDER"
                    ? `New order for ${tableName} (#${orderId.slice(-4)}).`
                    : `Bill requested for ${tableName}.`,
                type: "ACTIONABLE_REQUEST",
                targetRole: event === "KITCHEN_NEW_ORDER" ? "kitchen" : "billing",
                targetStaffId: "",
                tableName,
                orderId,
                orderDocPath: orderRef.path
            }];
        } else if (event === "ORDER_READY") {
            if (String(order.orderStatus || "").toUpperCase() !== "READY"
                || typeof order.waiterId !== "string" || !order.waiterId) {
                throw new HttpsError("failed-precondition", "Order is not ready for waiter notification.");
            }
            const waiterSnapshot = await db.collection("users").doc(order.waiterId).get();
            if (!waiterSnapshot.exists
                || waiterSnapshot.get("restaurantId") !== restaurantId
                || waiterSnapshot.get("status") !== "Active"
                || !waiterSnapshot.get("staffDocId")) {
                throw new HttpsError("failed-precondition", "Order waiter is no longer an active restaurant staff member.");
            }
            notificationSpecs = [{
                id: notificationId,
                title: "Order Ready!",
                message: `Order for ${tableName} is ready for pick-up.`,
                type: "INFORMATIONAL",
                targetRole: "waiter",
                targetStaffId: waiterSnapshot.get("staffDocId"),
                tableName,
                orderId,
                orderDocPath: orderRef.path
            }];
        } else {
            const completedPrefix = `restaurants/${restaurantId}/completed_orders/`;
            if (!orderDocPath.startsWith(completedPrefix)
                || orderDocPath.slice(completedPrefix.length).includes("/")
                || String(order.orderStatus || "").toUpperCase() !== "PAID") {
                throw new HttpsError("failed-precondition", "Only settled orders can generate payment notifications.");
            }
            const waiterSnapshot = typeof order.waiterId === "string"
                ? await db.collection("users").doc(order.waiterId).get()
                : null;
            const waiterStaffDocId = waiterSnapshot?.exists
                && waiterSnapshot.get("restaurantId") === restaurantId
                && waiterSnapshot.get("status") === "Active"
                ? String(waiterSnapshot.get("staffDocId") || "")
                : "";
            notificationSpecs = [{
                id: `${notificationId}_manager`,
                title: `Payment Received: ${tableName}`,
                message: `Bill of ${String(order.grandTotal || 0)} for ${tableName} has been paid.`,
                type: "INFORMATIONAL",
                targetRole: "manager",
                targetStaffId: "",
                tableName,
                orderId,
                orderDocPath: orderRef.path
            }];
            if (waiterStaffDocId) {
                notificationSpecs.push({
                    id: `${notificationId}_waiter`,
                    title: `Table Free: ${tableName}`,
                    message: `${tableName} is now free.`,
                    type: "INFORMATIONAL",
                    targetRole: "waiter",
                    targetStaffId: waiterStaffDocId,
                    tableName,
                    orderId,
                    orderDocPath: orderRef.path
                });
            }
        }
    }

    let createdSpecs = [];
    await db.runTransaction(async (transaction) => {
        const refs = notificationSpecs.map((spec) => notificationsRef.doc(spec.id));
        const snapshots = await Promise.all(refs.map((ref) => transaction.get(ref)));
        createdSpecs = [];
        notificationSpecs.forEach((spec, index) => {
            if (!snapshots[index].exists) {
                transaction.create(refs[index], {
                    ...spec,
                    timestamp: FieldValue.serverTimestamp(),
                    isRead: false,
                    status: "PENDING"
                });
                createdSpecs.push(spec);
            }
        });
    });

    const pushType = {
        KITCHEN_NEW_ORDER: "NEW_ORDER",
        BILL_REQUEST: "BILL_REQUESTED",
        ORDER_READY: "ORDER_READY",
        INVENTORY_UPDATE: "INVENTORY_UPDATE",
        PAYMENT_SUCCESS: "PAYMENT_SUCCESS"
    }[event];
    await Promise.all(createdSpecs.map(async (spec) => {
        const tokens = [...new Set(await getNotificationTokens(restaurantId, spec))].slice(0, 500);
        if (tokens.length === 0) {
            return;
        }
        await admin.messaging().sendEachForMulticast({
            tokens,
            notification: { title: spec.title, body: spec.message },
            data: { type: pushType }
        });
    }));
    return { success: true };
});

exports.updateStaffNotificationStatus = onAppCheckCall(async (request) => {
    const { restaurantId, notificationId, status } = request.data || {};
    if (typeof notificationId !== "string" || !notificationId || notificationId.includes("/")
        || !["ACCEPTED", "DONE"].includes(status)) {
        throw new HttpsError("invalid-argument", "Notification ID and valid status are required.");
    }
    const actor = await assertRestaurantRole(request, restaurantId, [
        "manager", "waiter", "head waiter", "waiter_staff",
        "chef", "master chef", "kitchen", "chef_staff", "cashier", "billing"
    ]);
    const notificationRef = db.collection("restaurants").doc(restaurantId)
        .collection("notifications").doc(notificationId);
    await db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(notificationRef);
        if (!snapshot.exists || snapshot.get("type") !== "ACTIONABLE_REQUEST") {
            throw new HttpsError("not-found", "Actionable notification not found.");
        }
        const notification = snapshot.data();
        const rolesByTarget = {
            kitchen: ["chef", "master chef", "kitchen", "chef_staff"],
            billing: ["cashier", "billing"],
            waiter: ["waiter", "head waiter", "waiter_staff"]
        };
        const actorCanRespond = actor.role === "owner"
            || actor.role === "manager"
            || (rolesByTarget[notification.targetRole] || []).includes(actor.role)
                && (!notification.targetStaffId
                    || notification.targetStaffId === actor.staffDocId);
        const validTransition = notification.status === "PENDING"
            || notification.status === "ACCEPTED" && status === "DONE";
        if (!actorCanRespond || !validTransition) {
            throw new HttpsError("permission-denied", "This notification cannot be updated by this account.");
        }
        transaction.update(notificationRef, { status, isRead: true });
    });
    return { success: true };
});
