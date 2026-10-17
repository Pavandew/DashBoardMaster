const { FieldValue } = require("firebase-admin/firestore");

const {
    admin,
    db,
    HttpsError,
    onAppCheckCall,
    normalizeOrderPath,
    assertRestaurantRole,
    numericPrice,
    calculateOrderTax,
    trustedMenuItem
} = require("./helpers");

exports.submitRestaurantOrder = onAppCheckCall(async (request) => {
    const { restaurantId, floorId, tableId, order, existingOrderDocId } = request.data || {};
    if (!restaurantId || !floorId || !tableId || !order
        || !Array.isArray(order.items) || order.items.length === 0 || order.items.length > 30) {
        throw new HttpsError("invalid-argument", "Restaurant, table, and order items are required.");
    }
    const requestedOrderId = String(order.orderId || "");
    if (requestedOrderId.length > 120 || requestedOrderId.includes("/")) {
        throw new HttpsError("invalid-argument", "Order ID is invalid.");
    }
    const actor = await assertRestaurantRole(request, restaurantId, [
        "waiter", "head waiter", "waiter_staff", "manager"
    ]);
    const restaurantRef = db.collection("restaurants").doc(restaurantId);
    const tableRef = restaurantRef.collection("res_floors").doc(floorId)
        .collection("floor_tables").doc(tableId);
    const orderCollection = tableRef.collection("active_orders");
    const orderRef = existingOrderDocId
        ? orderCollection.doc(String(existingOrderDocId))
        : orderCollection.doc();
    const userRef = db.collection("users").doc(request.auth.uid);
    const staffRef = actor.staffDocId
        ? restaurantRef.collection("staff").doc(actor.staffDocId)
        : null;

    await db.runTransaction(async (transaction) => {
        const [userSnapshot, restaurantSnapshot, tableSnapshot, existingSnapshot, staffSnapshot] =
            await Promise.all([
                transaction.get(userRef),
                transaction.get(restaurantRef),
                transaction.get(tableRef),
                transaction.get(orderRef),
                staffRef ? transaction.get(staffRef) : Promise.resolve(null)
            ]);
        const role = String(userSnapshot.get("role") || "").toLowerCase();
        const owner = ["owner_single", "owner_multi"].includes(role)
            && userSnapshot.get("status") === "Active"
            && userSnapshot.get("isVerified") === true
            && restaurantSnapshot.get("ownerUid") === request.auth.uid;
        const staffAllowed = userSnapshot.get("restaurantId") === restaurantId
            && userSnapshot.get("status") === "Active"
            && staffSnapshot?.exists
            && userSnapshot.get("staffDocId") === actor.staffDocId
            && staffSnapshot.get("staffId") === userSnapshot.get("staffId")
            && staffSnapshot.get("status") === "Active"
            && String(staffSnapshot.get("role") || "").toLowerCase() === role
            && ["waiter", "head waiter", "waiter_staff", "manager"].includes(role);
        if (!restaurantSnapshot.exists || !tableSnapshot.exists
            || (!owner && !staffAllowed)
            || (existingOrderDocId && !existingSnapshot.exists)) {
            throw new HttpsError("permission-denied", "Order actor, table, or existing order is not authorized.");
        }

        const oldItems = existingSnapshot.exists && Array.isArray(existingSnapshot.get("items"))
            ? existingSnapshot.get("items")
            : [];
        const prepared = [];
        const requestedKeys = new Set();
        const trustedMenu = await Promise.all(
            order.items.map((candidate) => trustedMenuItem(transaction, restaurantId, candidate))
        );
        for (const [index, candidate] of order.items.entries()) {
            const quantity = Number(candidate.quantity);
            if (!Number.isInteger(quantity) || quantity < 1 || quantity > 500) {
                throw new HttpsError("invalid-argument", "Order quantities must be between 1 and 500.");
            }
            const menu = trustedMenu[index];
            const expectedItemId = `${menu.catalogItemId}${menu.variantName ? `_${menu.variantName}` : ""}`;
            if (menu.itemId !== menu.catalogItemId && menu.itemId !== expectedItemId) {
                throw new HttpsError("invalid-argument", "Order line identifier does not match its menu item.");
            }
            const addonKey = menu.addonIds.slice().sort().join(",");
            const key = `${menu.catalogItemId}|${menu.variantName}|${addonKey}`;
            if (requestedKeys.has(key)) {
                throw new HttpsError("invalid-argument", "Duplicate order lines must be combined.");
            }
            requestedKeys.add(key);
            const previous = oldItems.find((item) =>
                String(item.catalogItemId || "") === menu.catalogItemId
                && String(item.variantName || "") === menu.variantName
                && [...(item.addonIds || [])].sort().join(",") === addonKey
            );
            if (previous && quantity < Number(previous.quantity || 0)) {
                throw new HttpsError("permission-denied", "Waiters cannot reduce existing ordered quantities.");
            }
            prepared.push({
                itemId: menu.itemId || `${menu.catalogItemId}${menu.variantName ? `_${menu.variantName}` : ""}`,
                catalogItemId: menu.catalogItemId,
                categoryId: menu.categoryId,
                itemName: menu.itemName,
                variantName: menu.variantName,
                addonIds: menu.addonIds,
                price: menu.price,
                quantity,
                rowTotal: menu.price * quantity,
                orderedQuantity: previous ? Number(previous.orderedQuantity || 0) : 0,
                readyQuantity: previous ? Number(previous.readyQuantity || 0) : 0,
                itemStatus: previous ? String(previous.itemStatus || "PENDING") : "PENDING"
            });
        }
        for (const oldItem of oldItems) {
            const oldKey = `${oldItem.catalogItemId}|${oldItem.variantName || ""}|${[...(oldItem.addonIds || [])].sort().join(",")}`;
            if (!requestedKeys.has(oldKey)) {
                throw new HttpsError("permission-denied", "Waiters cannot remove existing order lines.");
            }
        }
        const subtotal = prepared.reduce((total, item) => total + item.rowTotal, 0);
        const tax = calculateOrderTax(subtotal, restaurantSnapshot.data());
        const now = FieldValue.serverTimestamp();
        transaction.set(orderRef, {
            orderId: existingSnapshot.exists
                ? String(existingSnapshot.get("orderId") || orderRef.id)
                : String(requestedOrderId || orderRef.id),
            tableId,
            floorId,
            tableName: String(order.tableName || tableSnapshot.get("tableName") || ""),
            customerName: String(order.customerName || ""),
            customerMobile: String(order.customerMobile || ""),
            orderType: String(order.orderType || "DINE_IN"),
            items: prepared,
            specialNotes: String(order.specialNotes || "").slice(0, 1000),
            subtotal,
            gst: tax.gst,
            grandTotal: tax.grandTotal,
            orderStatus: oldItems.length ? String(existingSnapshot.get("orderStatus") || "PENDING") : "PENDING",
            paymentMethod: "",
            restaurantId,
            waiterId: request.auth.uid,
            orderSource: "WAITER",
            orderedBy: String(actor.user.fullName || actor.user.staffName || ""),
            timestamp: existingSnapshot.exists ? existingSnapshot.get("timestamp") || now : now,
            orderDocPath: orderRef.path
        });
        transaction.set(tableRef, {
            status: "OCCUPIED",
            currentBillAmount: `₹${Math.trunc(tax.grandTotal)}`,
            customerName: String(order.customerName || "")
        }, { merge: true });
    });
    return { success: true, orderDocPath: orderRef.path, orderDocId: orderRef.id };
});

exports.updateKitchenOrder = onAppCheckCall(async (request) => {
    const { restaurantId, docPath, action, orderStatus, rejectionReason, items } = request.data || {};
    await assertRestaurantRole(request, restaurantId, [
        "chef", "master chef", "kitchen", "chef_staff", "manager"
    ]);
    const orderRef = normalizeOrderPath(docPath, restaurantId);
    const restaurantRef = db.collection("restaurants").doc(restaurantId);
    const userRef = db.collection("users").doc(request.auth.uid);
    const actor = await assertRestaurantRole(request, restaurantId, [
        "chef", "master chef", "kitchen", "chef_staff", "manager"
    ]);
    const staffRef = actor.staffDocId
        ? restaurantRef.collection("staff").doc(actor.staffDocId)
        : null;
    await db.runTransaction(async (transaction) => {
        const [orderSnapshot, restaurantSnapshot, userSnapshot, staffSnapshot] = await Promise.all([
            transaction.get(orderRef),
            transaction.get(restaurantRef),
            transaction.get(userRef),
            staffRef ? transaction.get(staffRef) : Promise.resolve(null)
        ]);
        const role = String(userSnapshot.get("role") || "").toLowerCase();
        const owner = ["owner_single", "owner_multi"].includes(role)
            && userSnapshot.get("status") === "Active"
            && userSnapshot.get("isVerified") === true
            && restaurantSnapshot.get("ownerUid") === request.auth.uid;
        const kitchenStaff = userSnapshot.get("restaurantId") === restaurantId
            && userSnapshot.get("status") === "Active"
            && staffSnapshot?.exists
            && userSnapshot.get("staffDocId") === actor.staffDocId
            && staffSnapshot.get("staffId") === userSnapshot.get("staffId")
            && staffSnapshot.get("status") === "Active"
            && String(staffSnapshot.get("role") || "").toLowerCase() === role
            && ["chef", "master chef", "kitchen", "chef_staff", "manager"].includes(role);
        if (!orderSnapshot.exists || (!owner && !kitchenStaff)
            || orderSnapshot.get("restaurantId") !== restaurantId) {
            throw new HttpsError("permission-denied", "This kitchen order is not authorized.");
        }
        const oldItems = Array.isArray(orderSnapshot.get("items")) ? orderSnapshot.get("items") : [];
        if (action === "status") {
            const status = String(orderStatus || "").toUpperCase();
            if (!["PREPARING", "READY", "REJECTED"].includes(status)) {
                throw new HttpsError("invalid-argument", "Kitchen order status is invalid.");
            }
            const updates = { orderStatus: status };
            if (rejectionReason) updates.rejectionReason = String(rejectionReason).slice(0, 500);
            if (status === "PREPARING") {
                updates.items = oldItems.map((item) => ({
                    ...item,
                    orderedQuantity: Number(item.quantity || 0)
                }));
            }
            transaction.update(orderRef, updates);
            return;
        }
        if (action === "ready") {
            if (!Array.isArray(items) || items.length !== oldItems.length) {
                throw new HttpsError("invalid-argument", "Kitchen item update is incomplete.");
            }
            const updatesById = new Map(items.map((item) => [String(item.itemId || ""), item]));
            const mappedItems = oldItems.map((oldItem) => {
                const update = updatesById.get(String(oldItem.itemId || ""));
                const ordered = Number(oldItem.orderedQuantity || oldItem.quantity || 0);
                const ready = Number(update?.readyQuantity);
                if (!update || !Number.isInteger(ready)
                    || ready < Number(oldItem.readyQuantity || 0) || ready > ordered) {
                    throw new HttpsError("permission-denied", "Ready quantity or item identity is invalid.");
                }
                return { ...oldItem, readyQuantity: ready };
            });
            transaction.update(orderRef, { items: mappedItems });
            return;
        }
        if (action === "reject") {
            const ids = request.data?.remainingItemIds;
            if (!Array.isArray(ids) || ids.some((id) => typeof id !== "string")
                || new Set(ids).size !== ids.length
                || ids.some((id) => !oldItems.some((item) => String(item.itemId || "") === id))) {
                throw new HttpsError("invalid-argument", "Remaining order item identifiers are invalid.");
            }
            const keep = new Set(ids);
            const trustedItems = await Promise.all(
                oldItems.map((item) => trustedMenuItem(transaction, restaurantId, item))
            );
            const verifiedItems = oldItems.map((item, index) => {
                const menu = trustedItems[index];
                const quantity = Number(item.quantity);
                const expectedItemId = `${menu.catalogItemId}${menu.variantName ? `_${menu.variantName}` : ""}`;
                if (!Number.isInteger(quantity) || quantity < 1 || quantity > 500
                    || (String(item.itemId || "") !== menu.catalogItemId
                        && String(item.itemId || "") !== expectedItemId)
                    || Number(item.price) !== menu.price
                    || Number(item.rowTotal) !== menu.price * quantity) {
                    throw new HttpsError(
                        "failed-precondition",
                        "Order prices do not match the trusted menu. Reopen and resubmit the order first."
                    );
                }
                return item;
            });
            const remaining = verifiedItems.filter((item) => keep.has(String(item.itemId || "")));
            const subtotal = remaining.reduce((total, item) =>
                total + Number(item.price) * Number(item.quantity), 0);
            const restaurantData = restaurantSnapshot.data();
            const serviceCharge = Number(orderSnapshot.get("serviceChargeAmount") || 0);
            const serviceTaxable = restaurantData.serviceChargeApplyTax === true;
            const tax = calculateOrderTax(
                subtotal + (serviceTaxable ? serviceCharge : 0),
                restaurantData
            );
            const grandTotal = tax.grandTotal + (serviceTaxable ? 0 : serviceCharge);
            const pathParts = orderRef.path.split("/");
            const tableRef = pathParts.length === 8
                ? db.collection("restaurants").doc(restaurantId)
                    .collection("res_floors").doc(pathParts[3])
                    .collection("floor_tables").doc(pathParts[5])
                : null;
            const tableSnapshot = tableRef ? await transaction.get(tableRef) : null;
            transaction.update(orderRef, {
                items: remaining,
                subtotal,
                gst: tax.gst,
                grandTotal,
                orderStatus: remaining.length === 0 ? "REJECTED" : orderSnapshot.get("orderStatus"),
                rejectionReason: String(rejectionReason || "").slice(0, 500)
            });
            if (tableRef && tableSnapshot?.exists) {
                transaction.set(tableRef, {
                    currentBillAmount: `₹${Math.trunc(grandTotal)}`
                }, { merge: true });
            }
            return;
        }
        throw new HttpsError("invalid-argument", "Unknown kitchen order operation.");
    });
    return { success: true };
});

exports.updateWaiterOrder = onAppCheckCall(async (request) => {
    const { restaurantId, docPath, action } = request.data || {};
    await assertRestaurantRole(request, restaurantId, [
        "waiter", "head waiter", "waiter_staff", "manager"
    ]);
    if (!["served", "billing"].includes(action)) {
        throw new HttpsError("invalid-argument", "Waiter order operation is invalid.");
    }
    const orderRef = normalizeOrderPath(docPath, restaurantId);
    const restaurantRef = db.collection("restaurants").doc(restaurantId);
    const userRef = db.collection("users").doc(request.auth.uid);
    const actor = await assertRestaurantRole(request, restaurantId, [
        "waiter", "head waiter", "waiter_staff", "manager"
    ]);
    const staffRef = actor.staffDocId
        ? restaurantRef.collection("staff").doc(actor.staffDocId)
        : null;
    await db.runTransaction(async (transaction) => {
        const [order, restaurant, user, staff] = await Promise.all([
            transaction.get(orderRef),
            transaction.get(restaurantRef),
            transaction.get(userRef),
            staffRef ? transaction.get(staffRef) : Promise.resolve(null)
        ]);
        const role = String(user.get("role") || "").toLowerCase();
        const owner = ["owner_single", "owner_multi"].includes(role)
            && user.get("status") === "Active"
            && user.get("isVerified") === true
            && restaurant.get("ownerUid") === request.auth.uid;
        const waiter = user.get("restaurantId") === restaurantId
            && user.get("status") === "Active"
            && staff?.exists
            && user.get("staffDocId") === actor.staffDocId
            && staff.get("staffId") === user.get("staffId")
            && staff.get("status") === "Active"
            && String(staff.get("role") || "").toLowerCase() === role
            && ["waiter", "head waiter", "waiter_staff", "manager"].includes(role);
        if (!order.exists || order.get("restaurantId") !== restaurantId || (!owner && !waiter)) {
            throw new HttpsError("permission-denied", "This waiter order is not authorized.");
        }
        if (action === "billing") {
            transaction.update(orderRef, { orderStatus: "BILLING" });
            return;
        }
        const items = Array.isArray(order.get("items")) ? order.get("items") : [];
        transaction.update(orderRef, {
            orderStatus: "SERVED",
            items: items.map((item) => String(item.itemStatus || "").toUpperCase() === "REJECTED"
                ? item
                : {
                    ...item,
                    orderedQuantity: Number(item.quantity || 0),
                    readyQuantity: Number(item.quantity || 0),
                    itemStatus: "SERVED"
                })
        });
    });
    return { success: true };
});

exports.settleRestaurantOrder = onAppCheckCall(async (request) => {
    const { restaurantId, docPath, paymentMethod, discountAmount, applyServiceCharge } = request.data || {};
    await assertRestaurantRole(request, restaurantId, ["cashier", "billing", "manager"]);
    if (!["Cash", "UPI", "Card", "Wallet"].includes(paymentMethod)) {
        throw new HttpsError("invalid-argument", "Payment method is invalid.");
    }
    const discount = Number(discountAmount || 0);
    if (!Number.isFinite(discount) || discount < 0) {
        throw new HttpsError("invalid-argument", "Discount amount is invalid.");
    }
    const activeRef = normalizeOrderPath(docPath, restaurantId);
    const pathParts = activeRef.path.split("/");
    const restaurantRef = db.collection("restaurants").doc(restaurantId);
    const userRef = db.collection("users").doc(request.auth.uid);
    const actor = await assertRestaurantRole(request, restaurantId, [
        "cashier", "billing", "manager"
    ]);
    const staffRef = actor.staffDocId
        ? restaurantRef.collection("staff").doc(actor.staffDocId)
        : null;
    const completedCollection = restaurantRef.collection("completed_orders");
    let completedOrderId = "";
    await db.runTransaction(async (transaction) => {
        const orderSnapshot = await transaction.get(activeRef);
        const [restaurantSnapshot, userSnapshot, staffSnapshot] = await Promise.all([
            transaction.get(restaurantRef),
            transaction.get(userRef),
            staffRef ? transaction.get(staffRef) : Promise.resolve(null)
        ]);
        if (!orderSnapshot.exists || orderSnapshot.get("restaurantId") !== restaurantId) {
            throw new HttpsError("not-found", "Active order was not found.");
        }
        const role = String(userSnapshot.get("role") || "").toLowerCase();
        const owner = ["owner_single", "owner_multi"].includes(role)
            && userSnapshot.get("status") === "Active"
            && userSnapshot.get("isVerified") === true
            && restaurantSnapshot.get("ownerUid") === request.auth.uid;
        const cashier = userSnapshot.get("restaurantId") === restaurantId
            && userSnapshot.get("status") === "Active"
            && staffSnapshot?.exists
            && userSnapshot.get("staffDocId") === actor.staffDocId
            && staffSnapshot.get("staffId") === userSnapshot.get("staffId")
            && staffSnapshot.get("status") === "Active"
            && String(staffSnapshot.get("role") || "").toLowerCase() === role
            && ["cashier", "billing", "manager"].includes(role);
        if (!owner && !cashier) {
            throw new HttpsError("permission-denied", "Only a cashier or owner/manager may settle this order.");
        }
        const currentStatus = String(orderSnapshot.get("orderStatus") || "").toUpperCase();
        if (!owner && role !== "manager" && !["BILLING", "SERVED"].includes(currentStatus)) {
            throw new HttpsError("failed-precondition", "Cashiers may settle only served or bill-requested orders.");
        }
        if (!owner && role !== "manager" && discount > 0) {
            throw new HttpsError("permission-denied", "Only the restaurant owner or manager may apply a discount.");
        }
        const storedItems = Array.isArray(orderSnapshot.get("items")) ? orderSnapshot.get("items") : [];
        if (storedItems.length === 0) {
            throw new HttpsError("failed-precondition", "Order has no verifiable menu items.");
        }
        const trustedItems = await Promise.all(
            storedItems.map((item) => trustedMenuItem(transaction, restaurantId, item))
        );
        const subtotal = storedItems.reduce((total, item, index) => {
            const quantity = Number(item.quantity);
            const menu = trustedItems[index];
            const expectedItemId = `${menu.catalogItemId}${menu.variantName ? `_${menu.variantName}` : ""}`;
            if (!Number.isInteger(quantity) || quantity < 1 || quantity > 500
                || (String(item.itemId || "") !== menu.catalogItemId
                    && String(item.itemId || "") !== expectedItemId)
                || Number(item.price) !== menu.price
                || Number(item.rowTotal) !== menu.price * quantity) {
                throw new HttpsError(
                    "failed-precondition",
                    "Order prices do not match the trusted menu. Reopen and resubmit the order before settling."
                );
            }
            return total + menu.price * quantity;
        }, 0);
        if (!Number.isFinite(subtotal) || subtotal < 0
            || typeof applyServiceCharge !== "boolean") {
            throw new HttpsError("failed-precondition", "Stored order totals or service charge choice are invalid.");
        }
        const restaurant = restaurantSnapshot.data();
        const serviceEnabled = restaurant.serviceChargeEnabled === true;
        const configuredServiceRate = numericPrice(restaurant.serviceChargePercent);
        const serviceRate = serviceEnabled ? configuredServiceRate : 0;
        const serviceDineInOnly = restaurant.serviceChargeDineInOnly === true;
        const orderType = String(orderSnapshot.get("orderType") || "").toUpperCase();
        const isDineIn = orderType.includes("DINE") || orderType.includes("TABLE");
        const eligibleForCharge = !serviceDineInOnly || isDineIn;
        if (serviceRate === null || serviceRate > 100
            || (applyServiceCharge && (!serviceEnabled || !eligibleForCharge))) {
            throw new HttpsError("failed-precondition", "Restaurant service-charge settings do not allow this selection.");
        }
        const serviceCharge = applyServiceCharge ? subtotal * serviceRate / 100 : 0;
        const serviceChargeTaxable = applyServiceCharge && restaurant.serviceChargeApplyTax === true;
        const tax = calculateOrderTax(subtotal + (serviceChargeTaxable ? serviceCharge : 0), restaurant);
        const gst = tax.gst;
        const gross = tax.grandTotal + (applyServiceCharge && !serviceChargeTaxable ? serviceCharge : 0);
        if (!Number.isFinite(gross) || discount > gross) {
            throw new HttpsError("failed-precondition", "Stored order totals or requested discount are invalid.");
        }
        const now = Date.now();
        const date = new Date(now);
        const billingDate = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
        const billingMonth = billingDate.slice(0, 7);
        const paidTotal = gross - discount;
        completedOrderId = String(orderSnapshot.get("orderId") || activeRef.id);
        const completedRef = completedCollection.doc(completedOrderId);
        const customerMobile = String(orderSnapshot.get("customerMobile") || "");
        const customerRef = customerMobile
            ? restaurantRef.collection("customers").doc(customerMobile)
            : null;
        const tableRef = pathParts.length === 8
            ? restaurantRef.collection("res_floors").doc(pathParts[3])
                .collection("floor_tables").doc(pathParts[5])
            : null;
        const [completedSnapshot, customerSnapshot, tableSnapshot] = await Promise.all([
            transaction.get(completedRef),
            customerRef ? transaction.get(customerRef) : Promise.resolve(null),
            tableRef ? transaction.get(tableRef) : Promise.resolve(null)
        ]);
        if (completedSnapshot.exists) {
            throw new HttpsError("already-exists", "This order has already been settled.");
        }
        transaction.create(completedRef, {
            ...orderSnapshot.data(),
            items: storedItems,
            orderStatus: "PAID",
            paymentMethod,
            subtotal,
            serviceChargeAmount: serviceCharge,
            gst,
            discountAmount: discount,
            grandTotal: paidTotal,
            paidAt: admin.firestore.Timestamp.fromMillis(now),
            billingMonth,
            billingDate
        });
        transaction.delete(activeRef);
        if (tableRef && tableSnapshot?.exists) {
            transaction.set(tableRef, {
                status: "FREE",
                customerName: "",
                currentBillAmount: ""
            }, { merge: true });
        }
        if (customerRef) {
            transaction.set(customerRef, {
                customerId: customerMobile,
                customerName: String(orderSnapshot.get("customerName") || ""),
                customerMobile,
                lastVisit: admin.firestore.Timestamp.fromMillis(now),
                visitCount: Number(customerSnapshot?.get("visitCount") || 0) + 1,
                totalSpent: Number(customerSnapshot?.get("totalSpent") || 0) + paidTotal
            }, { merge: true });
        }
    });
    return { success: true, completedOrderId };
});

exports.completeTakeawayOrder = onAppCheckCall(async (request) => {
    const { restaurantId, docPath } = request.data || {};
    if (!restaurantId || typeof docPath !== "string") {
        throw new HttpsError("invalid-argument", "Restaurant and completed-order path are required.");
    }
    await assertRestaurantRole(request, restaurantId, ["cashier", "billing"]);
    const expectedPrefix = `restaurants/${restaurantId}/completed_orders/`;
    if (!docPath.startsWith(expectedPrefix) || docPath.slice(expectedPrefix.length).includes("/")) {
        throw new HttpsError("permission-denied", "Completed-order path is outside this restaurant.");
    }

    const orderRef = db.doc(docPath);
    await db.runTransaction(async (transaction) => {
        const orderSnapshot = await transaction.get(orderRef);
        if (!orderSnapshot.exists
            || orderSnapshot.get("restaurantId") !== restaurantId
            || orderSnapshot.get("orderStatus") !== "PAID"
            || !["TAKE_AWAY", "DELIVERY"].includes(
                String(orderSnapshot.get("orderType") || "").toUpperCase()
            )) {
            throw new HttpsError(
                "failed-precondition",
                "Only paid takeaway or delivery orders can be marked as handed over."
            );
        }
        transaction.update(orderRef, { orderStatus: "COMPLETED" });
    });
    return { success: true };
});
