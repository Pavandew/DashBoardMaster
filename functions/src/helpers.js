const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { FieldValue } = require("firebase-admin/firestore");
const bcrypt = require("bcryptjs");
const crypto = require("node:crypto");

if (!admin.apps.length) {
    admin.initializeApp();
}

const db = admin.firestore();
const DUMMY_PASSWORD_HASH = bcrypt.hashSync(crypto.randomBytes(32).toString("hex"), 10);

function getRazorpayKeyId() {
    return process.env.RAZORPAY_KEY_ID || "";
}

function getRazorpayKeySecret() {
    return process.env.RAZORPAY_KEY_SECRET || "";
}

function onAppCheckCall(optionsOrHandler, maybeHandler) {
    if (typeof optionsOrHandler === "function") {
        return onCall({ enforceAppCheck: false }, optionsOrHandler);
    }
    return onCall({ ...optionsOrHandler, enforceAppCheck: false }, maybeHandler);
}

const STAFF_ROLES = new Set([
    "waiter",
    "head waiter",
    "waiter_staff",
    "chef",
    "master chef",
    "kitchen",
    "chef_staff",
    "cashier",
    "billing",
    "manager"
]);

const STAFF_STATUSES = new Set(["Active", "Suspended", "Disabled"]);

function normalizeMobile(value) {
    const digits = String(value || "").replace(/\D/g, "");
    if (digits.length === 10) return `+91${digits}`;
    if (digits.length === 12 && digits.startsWith("91")) return `+${digits}`;
    return String(value || "").trim();
}

function staffProfileFromInput(input) {
    if (!input || typeof input !== "object") {
        throw new HttpsError("invalid-argument", "Staff profile is required.");
    }

    const role = String(input.role || "").trim();
    const status = String(input.status || "Active").trim();
    const profile = {
        staffId: String(input.staffId || "").trim(),
        staffName: String(input.staffName || "").trim(),
        mobile: normalizeMobile(input.mobile),
        email: String(input.email || "").trim(),
        gender: String(input.gender || "").trim(),
        role: role,
        department: String(input.department || "").trim(),
        joiningDate: String(input.joiningDate || "").trim(),
        shift: String(input.shift || "").trim(),
        salary: String(input.salary || "").trim(),
        status: status,
        permissions: Array.isArray(input.permissions) ? input.permissions : [],
        documentType: String(input.documentType || "").trim(),
        documentNumber: String(input.documentNumber || "").trim()
    };

    if (!profile.staffId || !profile.staffName || !profile.mobile) {
        throw new HttpsError("invalid-argument", "Staff ID, name, and mobile are required.");
    }
    if (!STAFF_ROLES.has(role.toLowerCase()) || !STAFF_STATUSES.has(status)) {
        throw new HttpsError("invalid-argument", "Staff role or status is invalid.");
    }
    if (!profile.permissions.every((permission) => typeof permission === "string")) {
        throw new HttpsError("invalid-argument", "Staff permissions must be strings.");
    }

    return profile;
}

async function assertRestaurantManager(request, restaurantId) {
    if (!request.auth || !request.auth.uid) {
        throw new HttpsError("unauthenticated", "Sign in before managing staff.");
    }

    const userSnapshot = await db.collection("users").doc(request.auth.uid).get();
    if (!userSnapshot.exists) {
        throw new HttpsError("permission-denied", "Trusted user profile is required.");
    }

    const user = userSnapshot.data();
    const role = String(user.role || "").toLowerCase();
    const restaurantSnapshot = await db.collection("restaurants").doc(restaurantId).get();
    if (!restaurantSnapshot.exists) {
        throw new HttpsError("not-found", "Restaurant not found.");
    }

    const restaurant = restaurantSnapshot.data();
    const managerRecord = role === "manager" && user.restaurantId === restaurantId
        && typeof user.staffDocId === "string"
        ? await db.collection("restaurants").doc(restaurantId)
            .collection("staff").doc(user.staffDocId).get()
        : null;
    const isOwner = ["owner_single", "owner_multi"].includes(role)
        && user.status === "Active"
        && user.isVerified === true
        && restaurant.ownerUid === request.auth.uid;
    const isManager = role === "manager"
        && user.status === "Active"
        && user.restaurantId === restaurantId
        && managerRecord?.exists
        && managerRecord.get("staffId") === user.staffId
        && String(managerRecord.get("role") || "").toLowerCase() === role
        && managerRecord.get("status") === "Active";

    if (!isOwner && !isManager) {
        throw new HttpsError("permission-denied", "You cannot manage staff for this restaurant.");
    }

    return restaurant.ownerUid;
}

function staffRecordPath(snapshot) {
    const segments = snapshot.ref.path.split("/");
    return segments.length === 4
        && segments[0] === "restaurants"
        && segments[2] === "staff";
}

async function findStaffBy(field, value) {
    try {
        const snapshots = await db.collectionGroup("staff")
            .where(field, "==", value)
            .get();
        return snapshots.docs.filter(staffRecordPath);
    } catch (e) {
        console.warn(`⚠️ collectionGroup("staff") query failed for ${field}=${value}:`, e.message);
        return [];
    }
}

async function findStaffUser(restaurantId, staffId) {
    try {
        const snapshot = await db.collection("users")
            .where("staffId", "==", staffId)
            .get();
        const matches = snapshot.docs.filter((d) => d.get("restaurantId") === restaurantId);
        if (matches.length > 1) {
            throw new HttpsError("failed-precondition", "Staff identity maps to multiple user accounts.");
        }
        return matches[0] || null;
    } catch (e) {
        console.warn("⚠️ [findStaffUser] Single-field fallback query error:", e.message);
        return null;
    }
}

function safeStaffProfile(data, id) {
    return {
        id: id,
        staffId: data.staffId || "",
        staffName: data.staffName || "",
        mobile: data.mobile || data.phone || "",
        email: data.email || "",
        gender: data.gender || "",
        role: data.role || "",
        department: data.department || "",
        joiningDate: data.joiningDate || "",
        shift: data.shift || "",
        salary: data.salary || "",
        status: data.status || "Active",
        permissions: Array.isArray(data.permissions) ? data.permissions : [],
        documentType: data.documentType || "",
        documentNumber: data.documentNumber || ""
    };
}

async function getStaffAuthRecord(staffDocId) {
    const userSnapshot = await db.collection("users").doc(staffDocId).get();
    if (!userSnapshot.exists) {
        throw new HttpsError("not-found", "Staff account not found.");
    }
    const user = userSnapshot.data();
    const restaurantId = user.restaurantId;
    if (!restaurantId || user.uid !== staffDocId || !user.staffId) {
        throw new HttpsError("permission-denied", "Staff account is not linked to a restaurant.");
    }

    const restaurantRef = db.collection("restaurants").doc(restaurantId);
    const restaurantSnapshot = await restaurantRef.get();
    let rosterId = user.staffDocId;
    if (typeof rosterId !== "string" || !rosterId) {
        const matches = (await findStaffBy("staffId", user.staffId)).filter((record) =>
            record.ref.path.startsWith(`restaurants/${restaurantId}/staff/`)
        );
        if (matches.length !== 1) {
            throw new HttpsError("not-found", "Staff account not found.");
        }
        rosterId = matches[0].id;
        await userSnapshot.ref.update({ staffDocId: rosterId });
    }
    const staffRef = restaurantRef.collection("staff").doc(rosterId);
    const staffSnapshot = await staffRef.get();
    if (!restaurantSnapshot.exists || !staffSnapshot.exists) {
        throw new HttpsError("not-found", "Staff account not found.");
    }
    const staff = staffSnapshot.data();
    const role = String(user.role || "").toLowerCase();
    if (staff.status !== "Active"
        || user.status !== "Active"
        || !STAFF_ROLES.has(role)
        || role !== String(staff.role || "").toLowerCase()
        || staff.staffId !== user.staffId
        || restaurantSnapshot.get("ownerUid") !== user.ownerUid) {
        throw new HttpsError("permission-denied", "This staff account is inactive.");
    }
    return { user, staff, staffRef, restaurantId };
}

async function checkRateLimit(identifier) {
    const rateLimitRef = db.collection("authRateLimits").doc(identifier);
    const doc = await rateLimitRef.get();
    const now = Date.now();

    if (doc.exists) {
        const data = doc.data();
        if (data.lockoutUntil && data.lockoutUntil <= now) {
            await rateLimitRef.delete();
            return;
        }
        if (data.lastAttempt && (now - data.lastAttempt) >= 15 * 60 * 1000) {
            await rateLimitRef.delete();
            return;
        }

        if (data.lockoutUntil && data.lockoutUntil > now) {
            const minutesLeft = Math.max(1, Math.ceil((data.lockoutUntil - now) / (60 * 1000)));
            throw new HttpsError(
                "resource-exhausted",
                `Too many failed login attempts. Please try again after ${minutesLeft} minute${minutesLeft > 1 ? 's' : ''}.`
            );
        }
        if (data.attempts >= 5) {
            await rateLimitRef.set({
                attempts: data.attempts + 1,
                lastAttempt: now,
                lockoutUntil: now + 15 * 60 * 1000
            }, { merge: true });
            throw new HttpsError(
                "resource-exhausted",
                "Too many failed login attempts. Account temporarily locked for 15 minutes."
            );
        }
    }
}

async function recordFailedAttempt(identifier) {
    const rateLimitRef = db.collection("authRateLimits").doc(identifier);
    const doc = await rateLimitRef.get();
    const now = Date.now();
    const attempts = doc.exists ? (doc.data().attempts || 0) + 1 : 1;
    await rateLimitRef.set({
        attempts: attempts,
        lastAttempt: now
    }, { merge: true });
}

async function clearRateLimit(identifier) {
    const rateLimitRef = db.collection("authRateLimits").doc(identifier);
    await rateLimitRef.delete();
}

function normalizeOrderPath(path, restaurantId) {
    if (typeof path !== "string") {
        throw new HttpsError("invalid-argument", "An active-order path is required.");
    }
    const parts = path.split("/");
    const rootOrder = parts.length === 4
        && parts[0] === "restaurants"
        && parts[1] === restaurantId
        && parts[2] === "active_orders";
    const tableOrder = parts.length === 8
        && parts[0] === "restaurants"
        && parts[1] === restaurantId
        && parts[2] === "res_floors"
        && parts[4] === "floor_tables"
        && parts[6] === "active_orders";
    if (!rootOrder && !tableOrder) {
        throw new HttpsError("permission-denied", "Order path is outside this restaurant.");
    }
    return db.doc(path);
}

async function assertRestaurantRole(request, restaurantId, allowedRoles) {
    if (!request.auth?.uid || typeof restaurantId !== "string" || !restaurantId) {
        throw new HttpsError("unauthenticated", "Sign in and select a restaurant.");
    }
    const [userSnapshot, restaurantSnapshot] = await Promise.all([
        db.collection("users").doc(request.auth.uid).get(),
        db.collection("restaurants").doc(restaurantId).get()
    ]);
    if (!userSnapshot.exists || !restaurantSnapshot.exists) {
        throw new HttpsError("permission-denied", "Trusted user and restaurant records are required.");
    }
    const user = userSnapshot.data();
    const role = String(user.role || "").toLowerCase();
    const isOwner = ["owner_single", "owner_multi"].includes(role)
        && user.status === "Active"
        && user.isVerified === true
        && restaurantSnapshot.get("ownerUid") === request.auth.uid;
    const staffDocId = typeof user.staffDocId === "string" ? user.staffDocId : "";
    const staffRecord = staffDocId
        ? await db.collection("restaurants").doc(restaurantId)
            .collection("staff").doc(staffDocId).get()
        : null;
    const isStaff = user.restaurantId === restaurantId
        && user.status === "Active"
        && staffRecord?.exists
        && staffRecord.get("staffId") === user.staffId
        && staffRecord.get("status") === "Active"
        && String(staffRecord.get("role") || "").toLowerCase() === role
        && STAFF_ROLES.has(role);
    if ((!isOwner && !isStaff) || (!isOwner && !allowedRoles.includes(role))) {
        throw new HttpsError("permission-denied", "Your role cannot perform this restaurant operation.");
    }
    return {
        role: isOwner ? "owner" : role,
        user,
        staffDocId: isOwner ? "" : staffDocId,
        restaurant: restaurantSnapshot.data()
    };
}

function numericPrice(value) {
    const parsed = typeof value === "number"
        ? value
        : Number(String(value || "").replace(/[^\d.]/g, ""));
    return Number.isFinite(parsed) && parsed >= 0 ? parsed : null;
}

function calculateOrderTax(subtotal, restaurant) {
    const settings = restaurant.taxSettings || restaurant;
    const configuredRate = numericPrice(settings.defaultTaxRate);
    const rate = settings.chargeTaxOnBills === true
        ? configuredRate
        : 0;
    if (rate === null || rate > 100) {
        throw new HttpsError("failed-precondition", "Restaurant tax configuration is invalid.");
    }
    const inclusive = settings.priceIncludesTax === true;
    const gst = rate === 0
        ? 0
        : inclusive
            ? subtotal - subtotal / (1 + rate / 100)
            : subtotal * rate / 100;
    return { gst, grandTotal: inclusive ? subtotal : subtotal + gst };
}

async function trustedMenuItem(transaction, restaurantId, candidate) {
    const categoryId = String(candidate.categoryId || "");
    const catalogItemId = String(candidate.catalogItemId || "");
    if (!categoryId || !catalogItemId) {
        throw new HttpsError("failed-precondition", "Refresh the menu; an order item has no catalog binding.");
    }
    const itemRef = db.collection("restaurants").doc(restaurantId)
        .collection("menu_categories").doc(categoryId)
        .collection("menu_food_items").doc(catalogItemId);
    const snapshot = await transaction.get(itemRef);
    if (!snapshot.exists || String(snapshot.get("status") || "Active").toLowerCase() !== "active") {
        throw new HttpsError("failed-precondition", "A selected menu item is unavailable.");
    }
    const data = snapshot.data();
    const variantName = String(candidate.variantName || "");
    const variants = Array.isArray(data.variants) ? data.variants : [];
    const price = data.hasVariants === true
        ? numericPrice(variants.find((variant) =>
            String(variant.variantName || "") === variantName
        )?.price)
        : numericPrice(data.price);
    if (price === null) {
        throw new HttpsError("failed-precondition", "A selected menu variant is invalid.");
    }
    const addonIds = Array.isArray(candidate.addonIds)
        ? [...new Set(candidate.addonIds.map((id) => String(id)))]
        : [];
    if (addonIds.length > 5 || addonIds.some((id) => !id || id.includes("/"))) {
        throw new HttpsError("invalid-argument", "Selected add-ons are invalid.");
    }
    let addonPrice = 0;
    for (const addonId of addonIds) {
        const addon = await transaction.get(itemRef.collection("addons").doc(addonId));
        const addonValue = addon.exists ? numericPrice(addon.get("price")) : null;
        if (addonValue === null) {
            throw new HttpsError("failed-precondition", "A selected menu add-on is unavailable.");
        }
        addonPrice += addonValue;
    }
    return {
        catalogItemId,
        categoryId,
        itemId: String(candidate.itemId || ""),
        itemName: String(data.itemName || ""),
        variantName,
        addonIds,
        price: Math.trunc(price + addonPrice)
    };
}

module.exports = {
    admin,
    db,
    HttpsError,
    DUMMY_PASSWORD_HASH,
    STAFF_ROLES,
    STAFF_STATUSES,
    getRazorpayKeyId,
    getRazorpayKeySecret,
    onAppCheckCall,
    normalizeMobile,
    staffProfileFromInput,
    assertRestaurantManager,
    findStaffBy,
    findStaffUser,
    safeStaffProfile,
    getStaffAuthRecord,
    checkRateLimit,
    recordFailedAttempt,
    clearRateLimit,
    normalizeOrderPath,
    assertRestaurantRole,
    numericPrice,
    calculateOrderTax,
    trustedMenuItem
};
