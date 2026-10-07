const fs = require("node:fs");
const path = require("node:path");
const { after, before, beforeEach, describe, it } = require("node:test");
const {
    assertFails,
    assertSucceeds,
    initializeTestEnvironment
} = require("@firebase/rules-unit-testing");
const {
    collection,
    collectionGroup,
    deleteDoc,
    doc,
    getDoc,
    getDocs,
    query,
    setDoc,
    updateDoc,
    where
} = require("firebase/firestore");

describe("Firestore tenant authorization", () => {
    let testEnv;

    before(async () => {
        testEnv = await initializeTestEnvironment({
            projectId: "demo-dashboardmaster-rules",
            firestore: {
                rules: fs.readFileSync(path.join(__dirname, "..", "..", "firestore.rules"), "utf8")
            }
        });
    });

    beforeEach(async () => {
        await testEnv.clearFirestore();
        await testEnv.withSecurityRulesDisabled(async (context) => {
            const db = context.firestore();
            await Promise.all([
                setDoc(doc(db, "restaurants/restaurant-a"), { ownerUid: "owner-a", restaurantId: "restaurant-a" }),
                setDoc(doc(db, "restaurants/restaurant-b"), { ownerUid: "owner-b", restaurantId: "restaurant-b" }),
                setDoc(doc(db, "users/owner-a"), {
                    uid: "owner-a", role: "owner_multi", status: "Active", isVerified: true
                }),
                setDoc(doc(db, "users/waiter-a"), {
                    uid: "waiter-a", role: "waiter", status: "Active", restaurantId: "restaurant-a",
                    staffId: "W-104", staffDocId: "roster-waiter-17"
                }),
                setDoc(doc(db, "users/kitchen-a"), {
                    uid: "kitchen-a", role: "kitchen", status: "Active", restaurantId: "restaurant-a",
                    staffId: "K-205", staffDocId: "roster-kitchen-23"
                }),
                setDoc(doc(db, "users/cashier-a"), {
                    uid: "cashier-a", role: "cashier", status: "Active", restaurantId: "restaurant-a",
                    staffId: "C-306", staffDocId: "roster-cashier-31"
                }),
                setDoc(doc(db, "users/manager-a"), {
                    uid: "manager-a", role: "manager", status: "Active", restaurantId: "restaurant-a",
                    staffId: "M-407", staffDocId: "roster-manager-44"
                }),
                setDoc(doc(db, "restaurants/restaurant-a/staff/roster-waiter-17"), {
                    staffId: "W-104", role: "waiter", status: "Active"
                }),
                setDoc(doc(db, "restaurants/restaurant-a/staff/roster-kitchen-23"), {
                    staffId: "K-205", role: "kitchen", status: "Active"
                }),
                setDoc(doc(db, "restaurants/restaurant-a/staff/roster-cashier-31"), {
                    staffId: "C-306", role: "cashier", status: "Active"
                }),
                setDoc(doc(db, "restaurants/restaurant-a/staff/roster-manager-44"), {
                    staffId: "M-407", role: "manager", status: "Active"
                }),
                setDoc(doc(db, "users/restaurant-a/staff/kitchen-a"), {
                    restaurantId: "restaurant-a", role: "kitchen", status: "Active", fcmToken: "token-a"
                }),
                setDoc(doc(db, "users/restaurant-b/staff/chef-b"), {
                    restaurantId: "restaurant-b", role: "kitchen", status: "Active", fcmToken: "token-b"
                }),
                setDoc(doc(db, "restaurants/restaurant-a/active_orders/order-a"), {
                    restaurantId: "restaurant-a",
                    orderStatus: "READY",
                    paymentMethod: "",
                    subtotal: 100,
                    gst: 5,
                    grandTotal: 105
                }),
                setDoc(doc(db, "restaurants/restaurant-a/active_orders/kitchen-pending-a"), {
                    restaurantId: "restaurant-a",
                    orderStatus: "PENDING",
                    paymentMethod: "",
                    subtotal: 100,
                    gst: 5,
                    grandTotal: 105
                }),
                setDoc(doc(db, "restaurants/restaurant-a/active_orders/kitchen-invalid-transition-a"), {
                    restaurantId: "restaurant-a",
                    orderStatus: "PENDING",
                    paymentMethod: "",
                    subtotal: 100,
                    gst: 5,
                    grandTotal: 105
                }),
                setDoc(doc(db, "restaurants/restaurant-a/active_orders/paid-order-a"), {
                    restaurantId: "restaurant-a",
                    orderStatus: "PAID",
                    paymentMethod: "Cash",
                    subtotal: 100,
                    gst: 5,
                    grandTotal: 105
                }),
                setDoc(doc(db, "restaurants/restaurant-a/active_orders/pending-order-a"), {
                    restaurantId: "restaurant-a",
                    orderStatus: "PENDING",
                    paymentMethod: "",
                    subtotal: 100,
                    gst: 5,
                    grandTotal: 105
                }),
                setDoc(doc(db, "restaurants/restaurant-a/res_floors/floor-a/floor_tables/table-a/active_orders/order-nested-a"), {
                    restaurantId: "restaurant-a",
                    orderStatus: "READY",
                    subtotal: 100,
                    gst: 5,
                    grandTotal: 105
                }),
                setDoc(doc(db, "restaurants/restaurant-b/active_orders/order-b"), {
                    restaurantId: "restaurant-b",
                    orderStatus: "READY",
                    subtotal: 100,
                    gst: 5,
                    grandTotal: 105
                }),
                setDoc(doc(db, "restaurants/restaurant-a/completed_orders/completed-a"), {
                    restaurantId: "restaurant-a", orderStatus: "PAID", grandTotal: 105,
                    timestamp: new Date()
                }),
                setDoc(doc(db, "restaurants/restaurant-a/notifications/broadcast"), {
                    targetRole: "kitchen", targetStaffId: "", type: "ACTIONABLE_REQUEST",
                    isRead: false, status: "PENDING", title: "New order", message: "Order available",
                    timestamp: new Date()
                }),
                setDoc(doc(db, "restaurants/restaurant-a/notifications/waiter-targeted"), {
                    targetRole: "waiter", targetStaffId: "roster-waiter-17",
                    type: "INFORMATIONAL", isRead: false, status: "PENDING",
                    title: "Order ready", message: "Ready for pickup", timestamp: new Date()
                }),
                setDoc(doc(db, "restaurants/restaurant-a/notifications/waiter-broadcast"), {
                    targetRole: "waiter", targetStaffId: "",
                    type: "INFORMATIONAL", isRead: false, status: "PENDING",
                    title: "Waiter notice", message: "For waiters", timestamp: new Date()
                }),
                setDoc(doc(db, "restaurants/restaurant-a/notifications/other-waiter"), {
                    targetRole: "waiter", targetStaffId: "roster-waiter-else",
                    type: "INFORMATIONAL", isRead: false, status: "PENDING",
                    title: "Order ready", message: "Private to another waiter", timestamp: new Date()
                }),
                setDoc(doc(db, "restaurants/restaurant-a/menu_categories/category-a"), {
                    menuCategoryName: "Food"
                }),
                setDoc(doc(db, "restaurants/restaurant-a/menu_categories/category-a/menu_food_items/item-a"), {
                    itemName: "Meal", price: 100
                }),
                setDoc(doc(db, "restaurants/restaurant-a/inventory/item-a"), {
                    itemName: "Rice", itemQuantity: 5
                }),
                setDoc(doc(db, "restaurants/restaurant-a/customers/customer-a"), {
                    customerName: "Customer", lastVisit: new Date()
                })
            ]);
        });
    });

    after(async () => {
        await testEnv.cleanup();
    });

    it("binds multi-owner access to restaurants.ownerUid", async () => {
        const db = testEnv.authenticatedContext("owner-a").firestore();
        await assertSucceeds(getDoc(doc(db, "restaurants/restaurant-a")));
        await assertFails(getDoc(doc(db, "restaurants/restaurant-b")));
    });

    it("denies unauthenticated access and scopes manager access to the assigned restaurant", async () => {
        const anonymousDb = testEnv.unauthenticatedContext().firestore();
        await assertFails(getDoc(doc(anonymousDb, "restaurants/restaurant-a")));
        const managerDb = testEnv.authenticatedContext("manager-a").firestore();
        await assertSucceeds(getDoc(doc(managerDb, "restaurants/restaurant-a")));
        await assertFails(getDoc(doc(managerDb, "restaurants/restaurant-b")));
    });

    it("allows an assigned manager operational edits without allowing tenant reassignment", async () => {
        const db = testEnv.authenticatedContext("manager-a").firestore();
        const order = doc(db, "restaurants/restaurant-a/active_orders/order-a");
        await assertSucceeds(updateDoc(order, { grandTotal: 200 }));
        await assertFails(updateDoc(order, { restaurantId: "restaurant-b" }));
    });

    it("prevents a tenant staff member from reading another restaurant's order", async () => {
        const db = testEnv.authenticatedContext("waiter-a").firestore();
        await assertSucceeds(getDoc(doc(db, "restaurants/restaurant-a/active_orders/order-a")));
        await assertFails(getDoc(doc(db, "restaurants/restaurant-b/active_orders/order-b")));
    });

    it("allows status-only role operations and rejects kitchen/waiter financial edits", async () => {
        const waiterDb = testEnv.authenticatedContext("waiter-a").firestore();
        const waiterOrder = doc(waiterDb, "restaurants/restaurant-a/active_orders/order-a");
        await assertSucceeds(updateDoc(waiterOrder, { orderStatus: "SERVED" }));
        await assertFails(updateDoc(waiterOrder, { subtotal: 1 }));
        await assertFails(updateDoc(waiterOrder, { restaurantId: "restaurant-b" }));

        const kitchenDb = testEnv.authenticatedContext("kitchen-a").firestore();
        const kitchenOrder = doc(kitchenDb, "restaurants/restaurant-a/active_orders/kitchen-pending-a");
        await assertSucceeds(updateDoc(kitchenOrder, { orderStatus: "PREPARING" }));
        await assertSucceeds(updateDoc(kitchenOrder, { orderStatus: "READY" }));
        await assertFails(updateDoc(kitchenOrder, { orderStatus: "PREPARING" }));
        await assertFails(updateDoc(kitchenOrder, { grandTotal: 1 }));
        await assertFails(updateDoc(
            doc(kitchenDb, "restaurants/restaurant-a/active_orders/kitchen-invalid-transition-a"),
            { orderStatus: "READY" }
        ));

        const cashierDb = testEnv.authenticatedContext("cashier-a").firestore();
        const cashierOrder = doc(cashierDb, "restaurants/restaurant-a/active_orders/order-a");
        await assertFails(updateDoc(cashierOrder, {
            orderStatus: "PAID",
            paymentMethod: "Cash"
        }));
        await assertFails(updateDoc(cashierOrder, { orderStatus: "BILLING" }));
        await assertFails(updateDoc(cashierOrder, { gst: 0 }));
        await assertFails(updateDoc(cashierOrder, { paymentMethod: "UPI" }));
        await assertFails(updateDoc(
            doc(cashierDb, "restaurants/restaurant-a/active_orders/paid-order-a"),
            { orderStatus: "COMPLETED" }
        ));
    });

    it("denies direct order creation because validated creation is performed by the callable", async () => {
        const db = testEnv.authenticatedContext("waiter-a").firestore();
        await assertFails(setDoc(doc(db, "restaurants/restaurant-a/active_orders/waiter-created"), {
            restaurantId: "restaurant-a", orderStatus: "PENDING", paymentMethod: "",
            subtotal: 100, gst: 5, grandTotal: 105
        }));
        await assertFails(setDoc(doc(db, "restaurants/restaurant-a/active_orders/waiter-paid"), {
            restaurantId: "restaurant-a", orderStatus: "PAID", paymentMethod: "Cash"
        }));
        await assertFails(setDoc(doc(db, "restaurants/restaurant-b/active_orders/waiter-cross-tenant"), {
            restaurantId: "restaurant-b", orderStatus: "PENDING", paymentMethod: ""
        }));
    });

    it("uses users.staffId to authorize a roster document whose ID differs from Auth UID", async () => {
        const db = testEnv.authenticatedContext("waiter-a").firestore();
        await assertSucceeds(getDoc(doc(db, "restaurants/restaurant-a/staff/roster-waiter-17")));
        await assertFails(getDoc(doc(db, "restaurants/restaurant-a/staff/roster-kitchen-23")));
        await assertFails(getDoc(doc(db, "restaurants/restaurant-b/staff/roster-waiter-17")));
    });

    it("allows operational roles that query completed orders to read, but never edit them", async () => {
        const waiterDb = testEnv.authenticatedContext("waiter-a").firestore();
        const waiterCompleted = doc(waiterDb, "restaurants/restaurant-a/completed_orders/completed-a");
        await assertSucceeds(getDoc(waiterCompleted));
        await assertFails(updateDoc(waiterCompleted, { grandTotal: 1 }));
        const cashierDb = testEnv.authenticatedContext("cashier-a").firestore();
        const completed = doc(cashierDb, "restaurants/restaurant-a/completed_orders/completed-a");
        await assertSucceeds(getDoc(completed));
        await assertFails(updateDoc(completed, { orderStatus: "COMPLETED" }));
        const ownerDb = testEnv.authenticatedContext("owner-a").firestore();
        await assertSucceeds(getDoc(doc(ownerDb, "restaurants/restaurant-a/completed_orders/completed-a")));
    });

    it("denies client access to credential and payment authority collections", async () => {
        const db = testEnv.authenticatedContext("owner-a").firestore();
        await assertFails(getDoc(doc(db, "authCredentials/secret")));
        await assertFails(getDoc(doc(db, "mobileMappings/123")));
        await assertFails(getDoc(doc(db, "authRateLimits/123")));
        await assertFails(getDoc(doc(db, "subscriptionPaymentOrders/order")));
        await assertFails(getDoc(doc(db, "payments/payment")));
        await assertFails(getDoc(doc(db, "subscriptions/subscription")));
        await assertFails(deleteDoc(doc(db, "restaurants/restaurant-a")));
    });

    it("denies clients from creating forged notifications or changing notification targets", async () => {
        const db = testEnv.authenticatedContext("waiter-a").firestore();
        const notification = doc(db, "restaurants/restaurant-a/notifications/forged");
        await assertFails(setDoc(notification, {
            title: "System alert",
            message: "Forged notification",
            type: "INFORMATIONAL",
            targetRole: "manager",
            targetStaffId: "M-407",
            timestamp: new Date(),
            isRead: false,
            status: "PENDING"
        }));
        await assertFails(updateDoc(
            doc(db, "restaurants/restaurant-a/notifications/existing"),
            { targetRole: "manager" }
        ));
    });

    it("limits targeted notification reads and status updates to the mapped staff member", async () => {
        const waiterDb = testEnv.authenticatedContext("waiter-a").firestore();
        await assertSucceeds(getDoc(doc(waiterDb, "restaurants/restaurant-a/notifications/waiter-targeted")));
        await assertFails(getDoc(doc(waiterDb, "restaurants/restaurant-a/notifications/other-waiter")));
        await assertFails(getDoc(doc(waiterDb, "restaurants/restaurant-a/notifications/broadcast")));
        await assertFails(updateDoc(
            doc(waiterDb, "restaurants/restaurant-a/notifications/waiter-targeted"),
            { status: "ACCEPTED", isRead: true }
        ));
        await assertFails(updateDoc(
            doc(waiterDb, "restaurants/restaurant-a/notifications/waiter-targeted"),
            { status: "DONE", isRead: true }
        ));

        const kitchenDb = testEnv.authenticatedContext("kitchen-a").firestore();
        await assertFails(updateDoc(
            doc(kitchenDb, "restaurants/restaurant-a/notifications/broadcast"),
            { status: "ACCEPTED", isRead: true }
        ));
        await assertSucceeds(updateDoc(
            doc(kitchenDb, "restaurants/restaurant-a/notifications/broadcast"),
            { isRead: true }
        ));
        await assertFails(updateDoc(
            doc(kitchenDb, "restaurants/restaurant-a/notifications/broadcast"),
            { targetStaffId: "K-205", isRead: true }
        ));
    });

    it("requires the trusted user-to-roster mapping for staff authorization", async () => {
        const db = testEnv.authenticatedContext("waiter-a").firestore();
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await updateDoc(doc(context.firestore(), "users/waiter-a"), {
                staffDocId: "wrong-roster-id"
            });
        });
        await assertFails(getDoc(doc(db, "restaurants/restaurant-a")));
        await assertFails(getDoc(doc(db, "restaurants/restaurant-a/staff/roster-waiter-17")));
        await assertFails(getDoc(doc(db, "restaurants/restaurant-a/active_orders/order-a")));
    });

    it("denies client writes to user security fields and restaurant staff records", async () => {
        const db = testEnv.authenticatedContext("waiter-a").firestore();
        await assertFails(updateDoc(doc(db, "users/waiter-a"), { role: "owner_multi" }));
        await assertFails(setDoc(doc(db, "restaurants/restaurant-a/staff/new-staff"), {
            role: "manager",
            status: "Active"
        }));
    });

    it("requires a tenant predicate to list mirrored staff tokens", async () => {
        const db = testEnv.authenticatedContext("waiter-a").firestore();
        const scoped = query(
            collection(db, "users/restaurant-a/staff"),
            where("restaurantId", "==", "restaurant-a"),
            where("role", "in", ["kitchen", "chef"])
        );
        const results = await assertSucceeds(getDocs(scoped));
        if (results.size !== 1) throw new Error(`Expected 1 tenant staff mirror, got ${results.size}`);
        await assertFails(getDoc(doc(db, "users/restaurant-b/staff/chef-b")));
        await assertFails(getDocs(query(
            collection(db, "users/restaurant-a/staff"),
            where("role", "in", ["kitchen", "chef"])
        )));
    });

    it("rejects collection-group order queries and permits tenant-scoped path queries", async () => {
        const db = testEnv.authenticatedContext("waiter-a").firestore();
        await assertFails(getDocs(query(
            collectionGroup(db, "active_orders"),
            where("restaurantId", "==", "restaurant-a")
        )));
        await assertFails(getDocs(collectionGroup(db, "active_orders")));

        const nestedPathQuery = query(
            collection(db, "restaurants/restaurant-a/res_floors/floor-a/floor_tables/table-a/active_orders"),
            where("restaurantId", "==", "restaurant-a")
        );
        const nestedResults = await assertSucceeds(getDocs(nestedPathQuery));
        if (nestedResults.size !== 1) throw new Error(`Expected 1 nested tenant order, got ${nestedResults.size}`);

        const rootPathQuery = query(
            collection(db, "restaurants/restaurant-a/active_orders"),
            where("restaurantId", "==", "restaurant-a")
        );
        await assertSucceeds(getDocs(rootPathQuery));
        await assertFails(getDocs(query(
            collection(db, "restaurants/restaurant-b/active_orders"),
            where("restaurantId", "==", "restaurant-b")
        )));
    });

    it("requires restaurant predicates for user and mirrored-staff list queries", async () => {
        const db = testEnv.authenticatedContext("manager-a").firestore();
        const users = await assertSucceeds(getDocs(query(
            collection(db, "users"),
            where("restaurantId", "==", "restaurant-a")
        )));
        if (users.size < 1) throw new Error("Expected assigned restaurant users.");
        await assertFails(getDocs(collection(db, "users")));

        const staff = await assertSucceeds(getDocs(query(
            collection(db, "users/restaurant-a/staff"),
            where("restaurantId", "==", "restaurant-a")
        )));
        if (staff.size !== 1) throw new Error(`Expected 1 tenant staff record, got ${staff.size}`);
    });

    it("allows Android tenant-scoped menu, inventory, notification, and completed-order queries", async () => {
        const waiterDb = testEnv.authenticatedContext("waiter-a").firestore();
        const categories = await assertSucceeds(getDocs(collection(
            waiterDb, "restaurants/restaurant-a/menu_categories"
        )));
        if (categories.size !== 1) throw new Error("Expected tenant menu categories.");
        const menuItems = await assertSucceeds(getDocs(collection(
            waiterDb, "restaurants/restaurant-a/menu_categories/category-a/menu_food_items"
        )));
        if (menuItems.size !== 1) throw new Error("Expected tenant menu items.");
        const inventory = await assertSucceeds(getDocs(collection(
            waiterDb, "restaurants/restaurant-a/inventory"
        )));
        if (inventory.size !== 1) throw new Error("Expected tenant inventory.");
        const waiterBroadcasts = await assertSucceeds(getDocs(query(
            collection(waiterDb, "restaurants/restaurant-a/notifications"),
            where("targetRole", "in", ["all", "waiter", "waiter_staff"]),
            where("targetStaffId", "==", ""),
            where("timestamp", ">=", new Date(0))
        )));
        if (waiterBroadcasts.size !== 1) throw new Error("Expected only waiter broadcast notifications.");
        const waiterTargets = await assertSucceeds(getDocs(query(
            collection(waiterDb, "restaurants/restaurant-a/notifications"),
            where("targetRole", "in", ["waiter", "waiter_staff"]),
            where("targetStaffId", "==", "roster-waiter-17"),
            where("timestamp", ">=", new Date(0))
        )));
        if (waiterTargets.size !== 1) throw new Error("Expected only this waiter's targeted notifications.");
        await assertFails(getDocs(query(
            collection(waiterDb, "restaurants/restaurant-a/notifications"),
            where("targetRole", "in", ["all", "waiter", "waiter_staff"]),
            where("timestamp", ">=", new Date(0))
        )));
        await assertSucceeds(getDocs(query(
            collection(waiterDb, "restaurants/restaurant-a/completed_orders"),
            where("timestamp", ">=", new Date(0))
        )));
        await assertFails(getDocs(collection(
            waiterDb, "restaurants/restaurant-b/menu_categories"
        )));

        const managerDb = testEnv.authenticatedContext("manager-a").firestore();
        const customers = await assertSucceeds(getDocs(collection(
            managerDb, "restaurants/restaurant-a/customers"
        )));
        if (customers.size !== 1) throw new Error("Expected manager-visible customer records.");
        await assertFails(getDocs(collection(
            waiterDb, "restaurants/restaurant-a/customers"
        )));
    });
});
