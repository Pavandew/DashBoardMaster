const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
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
    const snapshots = await db.collectionGroup("staff")
        .where(field, "==", value)
        .get();
    return snapshots.docs.filter(staffRecordPath);
}

async function findStaffUser(restaurantId, staffId) {
    const snapshot = await db.collection("users")
        .where("restaurantId", "==", restaurantId)
        .where("staffId", "==", staffId)
        .get();
    if (snapshot.size > 1) {
        throw new HttpsError("failed-precondition", "Staff identity maps to multiple user accounts.");
    }
    return snapshot.docs[0] || null;
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

// Rate limiting helper for brute-force protection
async function checkRateLimit(identifier) {
    const rateLimitRef = db.collection("authRateLimits").doc(identifier);
    const doc = await rateLimitRef.get();
    const now = Date.now();

    if (doc.exists) {
        const data = doc.data();
        if (data.lockoutUntil && data.lockoutUntil > now) {
            throw new HttpsError(
                "resource-exhausted",
                "Too many failed login attempts. Please try again after 15 minutes."
            );
        }
        if (data.attempts >= 5 && (now - data.lastAttempt) < 15 * 60 * 1000) {
            await rateLimitRef.set({
                attempts: data.attempts + 1,
                lastAttempt: now,
                lockoutUntil: now + 15 * 60 * 1000
            }, { merge: true });
            throw new HttpsError(
                "resource-exhausted",
                "Too many failed login attempts. Account temporarily locked."
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

/**
 * 1. Register User (Owner or Staff)
 * Obtains UID strictly from Firebase Phone-OTP session context (request.auth.uid).
 */
exports.registerUser = onAppCheckCall(async (request) => {
    console.log("📝 [Register] Request received for UID:", request.auth?.uid);
    if (!request.auth || !request.auth.uid) {
        console.error("❌ [Register] Unauthenticated call to registerUser.");
        throw new HttpsError("unauthenticated", "Phone OTP authentication required before registration.");
    }

    const uid = request.auth.uid; // STRICTLY from authenticated session
    const { mobile, password } = request.data || {};

    if (!mobile || !password) {
        console.error("❌ [Register] Missing mobile or password.");
        throw new HttpsError("invalid-argument", "Mobile and password are required.");
    }

    const cleanMobile = normalizeMobile(mobile);
    const verifiedMobile = normalizeMobile(request.auth.token.phone_number);
    if (!verifiedMobile || verifiedMobile !== cleanMobile) {
        console.error("❌ [Register] Mobile mismatch | Verified:", verifiedMobile, "| Requested:", cleanMobile);
        throw new HttpsError(
            "permission-denied",
            "The requested mobile number does not match the verified phone."
        );
    }

    // Check global mobile uniqueness across mobileMappings
    const mappingRef = db.collection("mobileMappings").doc(cleanMobile);
    const mappingDoc = await mappingRef.get();
    if (mappingDoc.exists) {
        console.warn("⚠️ [Register] Mobile number already registered:", cleanMobile);
        throw new HttpsError("already-exists", "This mobile number is already registered.");
    }

    // Hash password securely with bcrypt
    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(password, salt);

    // Save secure credential in authCredentials/{uid} (completely inaccessible from client)
    await db.collection("authCredentials").doc(uid).set({
        uid: uid,
        mobile: cleanMobile,
        passwordHash: passwordHash,
        createdAt: Date.now()
    });

    // Save mobile mapping for global uniqueness resolution
    await mappingRef.set({
        uid: uid,
        createdAt: Date.now()
    });

    console.log("🟢 [Register] Account successfully registered and mapped for UID:", uid);
    return { success: true, uid: uid };
});

exports.isMobileRegistered = onAppCheckCall(async (request) => {
    const mobile = normalizeMobile(request.data?.mobile);
    if (!/^\+91\d{10}$/.test(mobile)) {
        throw new HttpsError("invalid-argument", "A valid Indian mobile number is required.");
    }
    return { registered: false };
});

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

exports.createStaff = onAppCheckCall(async (request) => {
    const { restaurantId, staff: rawStaff, password } = request.data || {};
    if (!restaurantId || typeof password !== "string" || password.length < 6) {
        throw new HttpsError("invalid-argument", "Restaurant and a 6-character password are required.");
    }

    const ownerUid = await assertRestaurantManager(request, restaurantId);
    const staff = staffProfileFromInput(rawStaff);
    const existingId = await findStaffBy("staffId", staff.staffId);
    if (existingId.length > 0) {
        throw new HttpsError("already-exists", "This staff ID is already registered.");
    }

    const staffCollection = db.collection("restaurants").doc(restaurantId).collection("staff");
    const existingMobile = await staffCollection.where("mobile", "==", staff.mobile).limit(1).get();
    if (!existingMobile.empty) {
        throw new HttpsError("already-exists", "This mobile number is already registered at this restaurant.");
    }

    const staffRef = staffCollection.doc();
    const userRef = db.collection("users").doc();
    const credentialRef = db.collection("authCredentials").doc(userRef.id);
    const mirrorRef = db.collection("users").doc(ownerUid).collection("staff").doc(staffRef.id);
    const restaurantMirrorRef = db.collection("users").doc(restaurantId)
        .collection("staff").doc(staffRef.id);
    const passwordHash = await bcrypt.hash(password, await bcrypt.genSalt(10));

    const userData = {
        uid: userRef.id,
        staffDocId: staffRef.id,
        staffId: staff.staffId,
        fullName: staff.staffName,
        mobile: staff.mobile,
        email: staff.email,
        role: staff.role,
        restaurantId: restaurantId,
        ownerUid: ownerUid,
        status: staff.status
    };
    const batch = db.batch();
    batch.create(staffRef, { id: staffRef.id, ...staff });
    batch.create(userRef, userData);
    batch.create(credentialRef, {
        uid: userRef.id,
        restaurantId: restaurantId,
        ownerUid: ownerUid,
        passwordHash: passwordHash,
        createdAt: Date.now()
    });
    batch.create(mirrorRef, {
        staffId: staff.staffId,
        restaurantId: restaurantId,
        role: staff.role,
        status: staff.status,
        fcmToken: ""
    });
    if (restaurantMirrorRef.path !== mirrorRef.path) {
        batch.create(restaurantMirrorRef, {
            staffId: staff.staffId,
            restaurantId: restaurantId,
            role: staff.role,
            status: staff.status,
            fcmToken: ""
        });
    }
    await batch.commit();

    return { staffDocId: staffRef.id };
});

exports.updateStaff = onAppCheckCall(async (request) => {
    const { restaurantId, staffDocId, staff: rawStaff } = request.data || {};
    if (!restaurantId || !staffDocId) {
        throw new HttpsError("invalid-argument", "Restaurant and staff IDs are required.");
    }

    const ownerUid = await assertRestaurantManager(request, restaurantId);
    const staff = staffProfileFromInput(rawStaff);
    const staffRef = db.collection("restaurants").doc(restaurantId)
        .collection("staff").doc(staffDocId);
    const mirrorRef = db.collection("users").doc(ownerUid).collection("staff").doc(staffDocId);
    const restaurantMirrorRef = db.collection("users").doc(restaurantId)
        .collection("staff").doc(staffDocId);
    const staffSnapshot = await staffRef.get();
    if (!staffSnapshot.exists) {
        throw new HttpsError("not-found", "Staff account not found.");
    }
    const userSnapshot = await findStaffUser(restaurantId, staffSnapshot.get("staffId"));
    if (!userSnapshot) throw new HttpsError("not-found", "Staff account not found.");
    const userRef = userSnapshot.ref;
    if (staffSnapshot.get("staffId") !== staff.staffId
        || userSnapshot.get("restaurantId") !== restaurantId
        || userSnapshot.get("ownerUid") !== ownerUid) {
        throw new HttpsError("permission-denied", "Staff identity or restaurant binding cannot be changed.");
    }

    const batch = db.batch();
    batch.set(staffRef, { id: staffDocId, ...staff });
    batch.set(userRef, {
        uid: userSnapshot.id,
        staffDocId: staffDocId,
        staffId: staff.staffId,
        fullName: staff.staffName,
        mobile: staff.mobile,
        email: staff.email,
        role: staff.role,
        restaurantId: restaurantId,
        ownerUid: ownerUid,
        status: staff.status
    });
    batch.set(mirrorRef, {
        staffId: staff.staffId,
        restaurantId: restaurantId,
        role: staff.role,
        status: staff.status
    }, { merge: true });
    if (restaurantMirrorRef.path !== mirrorRef.path) {
        batch.set(restaurantMirrorRef, {
            staffId: staff.staffId,
            restaurantId: restaurantId,
            role: staff.role,
            status: staff.status
        }, { merge: true });
    }
    await batch.commit();
    return { success: true };
});

exports.deleteStaff = onAppCheckCall(async (request) => {
    const { restaurantId, staffDocId } = request.data || {};
    if (!restaurantId || !staffDocId) {
        throw new HttpsError("invalid-argument", "Restaurant and staff IDs are required.");
    }

    const ownerUid = await assertRestaurantManager(request, restaurantId);
    const staffRef = db.collection("restaurants").doc(restaurantId)
        .collection("staff").doc(staffDocId);
    const staffSnapshot = await staffRef.get();
    if (!staffSnapshot.exists) {
        throw new HttpsError("not-found", "Staff account not found.");
    }
    const userSnapshot = await findStaffUser(restaurantId, staffSnapshot.get("staffId"));
    if (!userSnapshot) throw new HttpsError("not-found", "Staff account not found.");
    const userRef = userSnapshot.ref;

    const batch = db.batch();
    batch.delete(staffRef);
    batch.delete(userRef);
    batch.delete(db.collection("authCredentials").doc(userSnapshot.id));
    batch.delete(db.collection("passwordChangeVerifications").doc(userSnapshot.id));
    batch.delete(db.collection("users").doc(ownerUid).collection("staff").doc(staffDocId));
    const restaurantMirrorRef = db.collection("users").doc(restaurantId)
        .collection("staff").doc(staffDocId);
    if (restaurantMirrorRef.path !== db.collection("users").doc(ownerUid)
        .collection("staff").doc(staffDocId).path) {
        batch.delete(restaurantMirrorRef);
    }
    await batch.commit();
    return { success: true };
});

exports.migrateStaffCredentials = onAppCheckCall(async (request) => {
    const { restaurantId } = request.data || {};
    if (!restaurantId) {
        throw new HttpsError("invalid-argument", "Restaurant ID is required.");
    }

    const ownerUid = await assertRestaurantManager(request, restaurantId);
    const restaurantRef = db.collection("restaurants").doc(restaurantId);
    const restaurantSnapshot = await restaurantRef.get();
    if (!restaurantSnapshot.exists || restaurantSnapshot.get("ownerUid") !== ownerUid) {
        throw new HttpsError("permission-denied", "Restaurant ownership data is invalid.");
    }

    const staffSnapshot = await restaurantRef
        .collection("staff").get();
    let batch = db.batch();
    let pendingWrites = 0;
    let migrated = 0;

    for (const staffDoc of staffSnapshot.docs) {
        const staff = staffDoc.data();
        let userSnapshot = staff.staffId
            ? await findStaffUser(restaurantId, staff.staffId)
            : null;
        if (!userSnapshot) {
            const legacyUser = await db.collection("users").doc(staffDoc.id).get();
            if (legacyUser.exists && legacyUser.get("restaurantId") === restaurantId) {
                userSnapshot = legacyUser;
            }
        }
        const userRef = userSnapshot
            ? userSnapshot.ref
            : db.collection("users").doc();
        const credentialRef = db.collection("authCredentials").doc(userRef.id);
        const legacyCredentialRef = db.collection("authCredentials").doc(staffDoc.id);
        const [credentialSnapshot, legacyCredentialSnapshot] = await Promise.all([
            credentialRef.get(),
            credentialRef.path === legacyCredentialRef.path
                ? Promise.resolve(null)
                : legacyCredentialRef.get()
        ]);
        if (userSnapshot
            && ["owner_single", "owner_multi"].includes(
                String(userSnapshot.get("role") || "").toLowerCase()
            )) {
            throw new HttpsError(
                "failed-precondition",
                "A staff record conflicts with an owner identity."
            );
        }

        const plaintextPassword = staff.password
            || (userSnapshot && userSnapshot.get("password"));
        if (plaintextPassword
            && (!credentialSnapshot.exists || !credentialSnapshot.get("passwordHash"))) {
            const passwordHash = await bcrypt.hash(
                plaintextPassword,
                await bcrypt.genSalt(10)
            );
            batch.set(credentialRef, {
                uid: userRef.id,
                restaurantId: restaurantId,
                ownerUid: ownerUid,
                passwordHash: passwordHash,
                createdAt: Date.now()
            }, { merge: true });
            pendingWrites += 1;
        } else if (!credentialSnapshot.exists
            && legacyCredentialSnapshot && legacyCredentialSnapshot.exists) {
            batch.set(credentialRef, legacyCredentialSnapshot.data());
            batch.delete(legacyCredentialRef);
            pendingWrites += 2;
        }

        if (Object.prototype.hasOwnProperty.call(staff, "password")) {
            batch.update(staffDoc.ref, {
                password: FieldValue.delete()
            });
            pendingWrites += 1;
        }
        if (userSnapshot && Object.prototype.hasOwnProperty.call(userSnapshot.data(), "password")) {
            batch.update(userRef, {
                password: FieldValue.delete()
            });
            pendingWrites += 1;
        }

        batch.set(userRef, {
            uid: userRef.id,
            staffDocId: staffDoc.id,
            staffId: staff.staffId || "",
            fullName: staff.staffName || "",
            mobile: normalizeMobile(staff.mobile || staff.phone),
            email: staff.email || "",
            role: staff.role || "",
            restaurantId: restaurantId,
            ownerUid: ownerUid,
            status: staff.status || "Active"
        });
        pendingWrites += 1;

        const mirrorData = {
            staffId: staff.staffId || "",
            restaurantId: restaurantId,
            role: staff.role || "",
            status: staff.status || "Active"
        };
        const mirrorRefs = [
            db.collection("users").doc(ownerUid).collection("staff").doc(staffDoc.id),
            db.collection("users").doc(restaurantId).collection("staff").doc(staffDoc.id)
        ];
        for (const mirrorRef of new Map(mirrorRefs.map((ref) => [ref.path, ref])).values()) {
            batch.set(mirrorRef, mirrorData, { merge: true });
            pendingWrites += 1;
        }

        migrated += 1;

        if (pendingWrites >= 450) {
            await batch.commit();
            batch = db.batch();
            pendingWrites = 0;
        }
    }

    if (pendingWrites > 0) await batch.commit();
    return { migrated };
});

exports.authenticateStaff = onAppCheckCall(async (request) => {
    const staffId = String(request.data?.staffId || "").trim();
    const password = request.data?.password;
    if (!staffId || typeof password !== "string" || !password) {
        throw new HttpsError("invalid-argument", "Staff ID and password are required.");
    }

    const rateLimitId = `staff-login:${staffId.toLowerCase()}`;
    await checkRateLimit(rateLimitId);
    const variants = [...new Set([staffId, staffId.toUpperCase(), staffId.toLowerCase()])];
    const matches = new Map();
    for (const candidate of variants) {
        const records = await findStaffBy("staffId", candidate);
        for (const record of records) matches.set(record.id, record);
    }

    if (matches.size !== 1) {
        await recordFailedAttempt(rateLimitId);
        throw new HttpsError("permission-denied", "Invalid staff ID or password.");
    }

    const staffSnapshot = [...matches.values()][0];
    const staffDocId = staffSnapshot.id;
    const pathParts = staffSnapshot.ref.path.split("/");
    const restaurantId = pathParts[1];
    const restaurantSnapshot = await db.collection("restaurants").doc(restaurantId).get();
    if (!restaurantSnapshot.exists) {
        await recordFailedAttempt(rateLimitId);
        throw new HttpsError("permission-denied", "Invalid staff ID or password.");
    }

    const ownerUid = restaurantSnapshot.get("ownerUid");
    const staff = staffSnapshot.data();
    let userSnapshot = await findStaffUser(restaurantId, staff.staffId);
    if (!userSnapshot) {
        const legacyUser = await db.collection("users").doc(staffDocId).get();
        if (legacyUser.exists && legacyUser.get("restaurantId") === restaurantId) {
            userSnapshot = legacyUser;
        }
    }
    const user = userSnapshot ? userSnapshot.data() : null;
    const role = String(staff.role || "").toLowerCase();
    if (!ownerUid
        || staff.status !== "Active"
        || !STAFF_ROLES.has(role)
        || user && (
            user.restaurantId !== restaurantId
            || user.ownerUid !== ownerUid
            || user.status !== "Active"
            || String(user.role || "").toLowerCase() !== role
        )) {
        await recordFailedAttempt(rateLimitId);
        throw new HttpsError("permission-denied", "Invalid staff ID or password.");
    }

    const userRef = userSnapshot
        ? userSnapshot.ref
        : db.collection("users").doc();
    const credentialRef = db.collection("authCredentials").doc(userRef.id);
    const legacyCredentialRef = db.collection("authCredentials").doc(staffDocId);
    const [credentialSnapshot, legacyCredentialSnapshot] = await Promise.all([
        credentialRef.get(),
        credentialRef.path === legacyCredentialRef.path
            ? Promise.resolve(null)
            : legacyCredentialRef.get()
    ]);
    const legacyPassword = staff.password
        || (user && user.password)
        || "";
    const passwordHash = credentialSnapshot.exists
        ? credentialSnapshot.get("passwordHash")
        : legacyCredentialSnapshot && legacyCredentialSnapshot.exists
            ? legacyCredentialSnapshot.get("passwordHash")
        : null;
    const passwordMatches = passwordHash
        ? await bcrypt.compare(password, passwordHash)
        : legacyPassword === password;
    if (!passwordMatches) {
        await recordFailedAttempt(rateLimitId);
        throw new HttpsError("permission-denied", "Invalid staff ID or password.");
    }

    const batch = db.batch();
    if (!user) {
        batch.set(userRef, {
            uid: userRef.id,
            staffDocId: staffDocId,
            staffId: staff.staffId,
            fullName: staff.staffName,
            mobile: normalizeMobile(staff.mobile || staff.phone),
            email: staff.email || "",
            role: staff.role,
            restaurantId: restaurantId,
            ownerUid: ownerUid,
            status: staff.status
        });
    } else if (user.staffDocId !== staffDocId) {
        batch.update(userRef, { staffDocId: staffDocId });
    }

    if (!credentialSnapshot.exists) {
        let migratedHash = passwordHash;
        if (!migratedHash) {
            migratedHash = await bcrypt.hash(password, await bcrypt.genSalt(10));
        }
        batch.set(credentialRef, {
            uid: userRef.id,
            restaurantId: restaurantId,
            ownerUid: ownerUid,
            passwordHash: migratedHash,
            createdAt: Date.now()
        });
        if (legacyCredentialSnapshot && legacyCredentialSnapshot.exists
            && legacyCredentialRef.path !== credentialRef.path) {
            batch.delete(legacyCredentialRef);
        }
    } else if (!credentialSnapshot.get("passwordHash")) {
        throw new HttpsError("failed-precondition", "Stored staff credential is invalid.");
    }
    if (Object.prototype.hasOwnProperty.call(staff, "password")) {
        batch.update(staffSnapshot.ref, { password: FieldValue.delete() });
    }
    if (userSnapshot && Object.prototype.hasOwnProperty.call(userSnapshot.data(), "password")) {
        batch.update(userRef, { password: FieldValue.delete() });
    }
    await batch.commit();

    await clearRateLimit(rateLimitId);
    const token = await admin.auth().createCustomToken(userRef.id);
    return {
        token: token,
        uid: userRef.id,
        restaurantId: restaurantId,
        ownerUid: ownerUid,
        profile: safeStaffProfile(staff, staffDocId)
    };
});

exports.updateStaffFcmToken = onAppCheckCall(async (request) => {
    if (!request.auth || !request.auth.uid) {
        throw new HttpsError("unauthenticated", "Sign in as staff before updating notifications.");
    }

    const token = String(request.data?.fcmToken || "").trim();
    const staffDocId = request.auth.uid;
    if (!token) {
        throw new HttpsError("invalid-argument", "FCM token is required.");
    }

    const { user, restaurantId, staffRef } = await getStaffAuthRecord(staffDocId);
    const restaurantSnapshot = await db.collection("restaurants").doc(restaurantId).get();
    if (!restaurantSnapshot.exists || restaurantSnapshot.get("ownerUid") !== user.ownerUid) {
        throw new HttpsError("permission-denied", "Staff restaurant binding is invalid.");
    }

    const refs = [
        staffRef,
        db.collection("users").doc(user.ownerUid).collection("staff").doc(staffRef.id),
        db.collection("users").doc(restaurantId).collection("staff").doc(staffRef.id)
    ];
    const uniqueRefs = [...new Map(refs.map((ref) => [ref.path, ref])).values()];
    const batch = db.batch();
    for (const ref of uniqueRefs) {
        batch.set(ref, {
            staffId: user.staffId,
            restaurantId: restaurantId,
            role: user.role,
            status: user.status,
            fcmToken: token
        }, { merge: true });
    }
    await batch.commit();
    return { success: true };
});

/**
 * 2. Unified Authentication Function (Owner & Staff)
 * Receives mobile + password -> verifies bcrypt -> returns custom token.
 */
exports.authenticateUser = onAppCheckCall(async (request) => {
    const { mobile, password } = request.data || {};
    console.log("🔐 [Auth] Request received for mobile:", mobile);

    if (typeof mobile !== "string" || !mobile.trim()
        || typeof password !== "string" || !password) {
        console.error("❌ [Auth] Invalid argument structure submitted.");
        throw new HttpsError("invalid-argument", "Invalid mobile number or password.");
    }

    const cleanMobile = normalizeMobile(mobile);
    const genericError = () => new HttpsError(
        "unauthenticated",
        "Invalid mobile number or password."
    );
    try {
        await checkRateLimit(cleanMobile);
    } catch (error) {
        if (error instanceof HttpsError && error.code === "resource-exhausted") {
            console.error("⚠️ [Auth] Rate limit exceeded for mobile:", cleanMobile);
            throw genericError();
        }
        throw error;
    }

    const mappingDoc = await db.collection("mobileMappings").doc(cleanMobile).get();
    const mappedUid = mappingDoc.exists && typeof mappingDoc.get("uid") === "string"
        ? mappingDoc.get("uid")
        : "";
    const uid = mappedUid || "invalid-login-lookup-placeholder";
    const [credentialSnapshot, userSnapshot] = await Promise.all([
        db.collection("authCredentials").doc(uid).get(),
        db.collection("users").doc(uid).get()
    ]);

    const storedHash = credentialSnapshot?.exists
        ? credentialSnapshot.get("passwordHash")
        : null;
    const passwordMatches = await bcrypt.compare(
        password,
        typeof storedHash === "string" && storedHash ? storedHash : DUMMY_PASSWORD_HASH
    );
    const activeUser = userSnapshot?.exists
        && userSnapshot.get("status") === "Active"
        && userSnapshot.get("isVerified") === true;

    if (!mappedUid || !mappingDoc.exists || !credentialSnapshot.exists || !storedHash
        || !passwordMatches || !activeUser) {
        console.warn("❌ [Auth] Authentication failed for mobile:", cleanMobile, "| MappedExists:", mappingDoc.exists, "| CredExists:", credentialSnapshot?.exists, "| PasswordMatches:", passwordMatches, "| ActiveUser:", activeUser);
        await recordFailedAttempt(cleanMobile);
        throw genericError();
    }

    await clearRateLimit(cleanMobile);
    const customToken = await admin.auth().createCustomToken(uid);
    console.log("🟢 [Auth] Authentication successful! Custom token generated for UID:", uid);
    return { token: customToken, uid };
});

/**
 * 3. Update Password
 * Updates secure bcrypt credential in authCredentials/{uid}.
 */
exports.updatePassword = onAppCheckCall(async (request) => {
    if (!request.auth || !request.auth.uid) {
        throw new HttpsError("unauthenticated", "Authentication required to update password.");
    }

    const uid = request.auth.uid;
    const { newPassword } = request.data || {};
    const authTime = Number(request.auth.token.auth_time || 0);
    const nowSeconds = Math.floor(Date.now() / 1000);
    const profileSnapshot = await db.collection("users").doc(uid).get();
    const callerPhone = normalizeMobile(request.auth.token.phone_number);
    const profilePhone = profileSnapshot.exists
        ? normalizeMobile(profileSnapshot.get("mobile") || profileSnapshot.get("phone"))
        : "";
    const isPhoneVerified = callerPhone && callerPhone === profilePhone;

    if (!Number.isFinite(authTime)
        || nowSeconds - authTime > 300
        || authTime > nowSeconds + 30
        || !profileSnapshot.exists
        || profileSnapshot.get("uid") !== uid
        || profileSnapshot.get("status") !== "Active") {
        throw new HttpsError(
            "failed-precondition",
            "Unable to update password. Verify your account and try again."
        );
    }

    if (typeof newPassword !== "string" || newPassword.length < 6 || newPassword.length > 128) {
        throw new HttpsError("invalid-argument", "Password must be between 6 and 128 characters.");
    }

    const verificationRef = db.collection("passwordChangeVerifications").doc(uid);
    const credentialRef = db.collection("authCredentials").doc(uid);
    await db.runTransaction(async (transaction) => {
        const verification = await transaction.get(verificationRef);
        const verifiedAt = verification.exists ? Number(verification.data().verifiedAt) : 0;
        if (!isPhoneVerified
            && (!verifiedAt || Date.now() - verifiedAt > 5 * 60 * 1000)) {
            throw new HttpsError(
                "failed-precondition",
                "Unable to update password. Verify your account and try again."
            );
        }

        const passwordHash = await bcrypt.hash(newPassword, await bcrypt.genSalt(10));
        transaction.set(credentialRef, {
            passwordHash: passwordHash,
            updatedAt: Date.now()
        }, { merge: true });
        transaction.delete(verificationRef);
    });

    return { success: true };
});

exports.resetPassword = onAppCheckCall(async (request) => {
    const genericFailure = () => new HttpsError(
        "failed-precondition",
        "Unable to reset password. Verify your account and try again."
    );
    const phone = normalizeMobile(request.auth?.token?.phone_number);
    const authTime = Number(request.auth?.token?.auth_time || 0);
    const newPassword = request.data?.newPassword;
    const nowSeconds = Math.floor(Date.now() / 1000);
    if (!request.auth?.uid || !/^\+\d{7,15}$/.test(phone)
        || request.auth.token.firebase?.sign_in_provider !== "phone"
        || !Number.isFinite(authTime)
        || nowSeconds - authTime > 300
        || authTime > nowSeconds + 30) {
        throw genericFailure();
    }
    if (typeof newPassword !== "string" || newPassword.length < 6 || newPassword.length > 128) {
        throw new HttpsError("invalid-argument", "Password must be between 6 and 128 characters.");
    }

    let userSnapshot = null;
    let staffSnapshot = null;
    const mappingSnapshot = await db.collection("mobileMappings").doc(phone).get();
    if (mappingSnapshot.exists && typeof mappingSnapshot.get("uid") === "string") {
        const mappedUser = await db.collection("users").doc(mappingSnapshot.get("uid")).get();
        const role = mappedUser.exists ? String(mappedUser.get("role") || "").toLowerCase() : "";
        if (mappedUser.exists
            && ["owner_single", "owner_multi"].includes(role)
            && mappedUser.get("status") === "Active"
            && mappedUser.get("isVerified") === true
            && normalizeMobile(mappedUser.get("mobile") || mappedUser.get("phone")) === phone) {
            userSnapshot = mappedUser;
        }
    }

    if (!userSnapshot) {
        const ownerCandidates = new Map();
        for (const field of ["mobile", "phone"]) {
            const matches = await db.collection("users").where(field, "==", phone).get();
            matches.docs.forEach((candidate) => ownerCandidates.set(candidate.id, candidate));
        }
        const owners = [...ownerCandidates.values()].filter((candidate) => {
            const role = String(candidate.get("role") || "").toLowerCase();
            return ["owner_single", "owner_multi"].includes(role)
                && candidate.get("status") === "Active"
                && candidate.get("isVerified") === true
                && normalizeMobile(candidate.get("mobile") || candidate.get("phone")) === phone;
        });
        if (owners.length === 1) {
            userSnapshot = owners[0];
        }
    }

    if (!userSnapshot) {
        const matchingStaff = new Map();
        for (const field of ["mobile", "phone"]) {
            for (const record of await findStaffBy(field, phone)) {
                matchingStaff.set(record.ref.path, record);
            }
        }
        const activeMatches = [];
        for (const record of matchingStaff.values()) {
            if (record.get("status") !== "Active"
                || normalizeMobile(record.get("mobile") || record.get("phone")) !== phone) {
                continue;
            }
            const restaurantId = record.ref.path.split("/")[1];
            let candidate;
            let linked;
            try {
                candidate = await findStaffUser(restaurantId, record.get("staffId"));
                if (!candidate) continue;
                linked = await getStaffAuthRecord(candidate.id);
            } catch (error) {
                if (error instanceof HttpsError) continue;
                throw error;
            }
            if (linked.staffRef.path === record.ref.path
                && linked.user.status === "Active"
                && normalizeMobile(linked.user.mobile || linked.user.phone) === phone) {
                activeMatches.push({ userSnapshot: candidate, staffSnapshot: record });
            }
        }
        if (activeMatches.length === 1) {
            ({ userSnapshot, staffSnapshot } = activeMatches[0]);
        }
    }

    if (!userSnapshot) {
        throw genericFailure();
    }

    const user = userSnapshot.data();
    const uid = userSnapshot.id;
    const credentialRef = db.collection("authCredentials").doc(uid);
    const passwordHash = await bcrypt.hash(newPassword, await bcrypt.genSalt(10));
    await db.runTransaction(async (transaction) => {
        transaction.set(credentialRef, {
            uid,
            restaurantId: user.restaurantId || "",
            ownerUid: user.ownerUid || "",
            passwordHash,
            updatedAt: Date.now()
        }, { merge: true });
        transaction.update(userSnapshot.ref, {
            password: FieldValue.delete()
        });
        if (staffSnapshot) {
            transaction.update(staffSnapshot.ref, {
                password: FieldValue.delete()
            });
        }
    });
    return { success: true };
});

exports.verifyStaffCurrentPassword = onAppCheckCall(async (request) => {
    if (!request.auth || !request.auth.uid) {
        throw new HttpsError("unauthenticated", "Authentication required.");
    }

    const { staffDocId, currentPassword } = request.data || {};
    if (!currentPassword) {
        throw new HttpsError("permission-denied", "Sign in as the staff member to verify this password.");
    }

    const uid = request.auth.uid;
    const rateLimitId = `staff-password-change:${uid}`;
    await checkRateLimit(rateLimitId);
    const { staff, user, staffRef } = await getStaffAuthRecord(uid);
    if (staffDocId && staffDocId !== staffRef.id) {
        throw new HttpsError("permission-denied", "Staff roster identity does not match this account.");
    }
    const credentialRef = db.collection("authCredentials").doc(uid);
    const credentialSnapshot = await credentialRef.get();
    const passwordHash = credentialSnapshot.exists
        ? credentialSnapshot.get("passwordHash")
        : null;
    const legacyPassword = staff.password || user.password || "";
    const matches = passwordHash
        ? await bcrypt.compare(currentPassword, passwordHash)
        : legacyPassword === currentPassword;

    if (!matches) {
        await recordFailedAttempt(rateLimitId);
        throw new HttpsError("permission-denied", "Current password is incorrect.");
    }

    if (!passwordHash) {
        const migratedHash = await bcrypt.hash(currentPassword, await bcrypt.genSalt(10));
        const batch = db.batch();
        batch.set(credentialRef, {
            uid: uid,
            restaurantId: user.restaurantId,
            ownerUid: user.ownerUid,
            passwordHash: migratedHash,
            createdAt: Date.now()
        });
        batch.update(staffRef, {
            password: FieldValue.delete()
        });
        batch.update(db.collection("users").doc(uid), {
            password: FieldValue.delete()
        });
        await batch.commit();
    }

    await clearRateLimit(rateLimitId);
    await db.collection("passwordChangeVerifications").doc(uid)
        .set({ verifiedAt: Date.now() });
    return { verified: true };
});

exports.updateStaffPassword = onAppCheckCall(async (request) => {
    if (!request.auth || !request.auth.uid) {
        throw new HttpsError("unauthenticated", "Authentication required.");
    }

    let { staffDocId, newPassword } = request.data || {};
    if (typeof newPassword !== "string" || newPassword.length < 6) {
        throw new HttpsError("invalid-argument", "A 6-character password is required.");
    }

    const callerPhone = normalizeMobile(request.auth.token.phone_number);
    if (!staffDocId && callerPhone) {
        const records = new Map();
        for (const candidate of new Set([callerPhone, String(request.auth.token.phone_number)])) {
            for (const record of await findStaffBy("mobile", candidate)) {
                records.set(record.id, record);
            }
            for (const record of await findStaffBy("phone", candidate)) {
                records.set(record.id, record);
            }
        }
        if (records.size !== 1) {
            throw new HttpsError("permission-denied", "No unique active staff account matches this phone.");
        }
        staffDocId = [...records.keys()][0];
    }
    if (!staffDocId) {
        throw new HttpsError("invalid-argument", "Staff identity is required.");
    }

    let userSnapshot;
    let staffSnapshot;
    if (callerPhone && request.auth.uid !== staffDocId) {
        const records = await findStaffBy("id", staffDocId);
        if (records.length !== 1) {
            throw new HttpsError("permission-denied", "Staff roster record is not unique.");
        }
        staffSnapshot = records[0];
        const pathParts = staffSnapshot.ref.path.split("/");
        userSnapshot = await findStaffUser(pathParts[1], staffSnapshot.get("staffId"));
        if (!userSnapshot) throw new HttpsError("not-found", "Staff account not found.");
    } else {
        userSnapshot = await db.collection("users").doc(request.auth.uid).get();
        const authRecord = await getStaffAuthRecord(request.auth.uid);
        if (staffDocId !== authRecord.staffRef.id) {
            throw new HttpsError("permission-denied", "Staff roster identity does not match this account.");
        }
        staffSnapshot = await authRecord.staffRef.get();
    }
    const staff = staffSnapshot.data();
    const user = userSnapshot.data();
    const restaurantId = user.restaurantId;
    const uid = userSnapshot.id;
    const isSignedInStaff = request.auth.uid === uid;
    const staffPhone = normalizeMobile(staff.mobile || staff.phone);
    const isPhoneVerified = callerPhone && callerPhone === staffPhone;
    const verificationRef = db.collection("passwordChangeVerifications").doc(uid);
    const verified = isSignedInStaff ? await verificationRef.get() : null;
    const verifiedAt = verified && verified.exists
        ? Number(verified.get("verifiedAt"))
        : 0;

    if (!isPhoneVerified && (
        !isSignedInStaff
        || !verifiedAt
        || Date.now() - verifiedAt > 5 * 60 * 1000
    )) {
        throw new HttpsError("failed-precondition", "Verify the current password or registered phone first.");
    }

    const passwordHash = await bcrypt.hash(newPassword, await bcrypt.genSalt(10));
    await db.runTransaction(async (transaction) => {
        if (isSignedInStaff && !isPhoneVerified) {
            const currentVerification = await transaction.get(verificationRef);
            const timestamp = currentVerification.exists
                ? Number(currentVerification.get("verifiedAt"))
                : 0;
            if (!timestamp || Date.now() - timestamp > 5 * 60 * 1000) {
                throw new HttpsError("failed-precondition", "Password verification expired.");
            }
        }

        transaction.set(db.collection("authCredentials").doc(uid), {
            uid: uid,
            restaurantId: restaurantId,
            ownerUid: user.ownerUid,
            passwordHash: passwordHash,
            updatedAt: Date.now()
        }, { merge: true });
        transaction.update(staffSnapshot.ref, {
                password: FieldValue.delete()
            });
        transaction.update(userSnapshot.ref, {
            password: FieldValue.delete()
        });
        transaction.delete(verificationRef);
    });

    return { success: true, authUserMatchesStaff: isSignedInStaff };
});

/**
 * Verify the caller's current password without exposing password hashes to clients.
 */
exports.verifyCurrentPassword = onAppCheckCall(async (request) => {
    if (!request.auth || !request.auth.uid) {
        throw new HttpsError("unauthenticated", "Authentication required.");
    }

    const { currentPassword } = request.data;
    if (!currentPassword) {
        throw new HttpsError("invalid-argument", "Current password is required.");
    }

    const rateLimitId = `password-change:${request.auth.uid}`;
    await checkRateLimit(rateLimitId);

    const credential = await db.collection("authCredentials")
        .doc(request.auth.uid)
        .get();
    if (!credential.exists || !credential.data().passwordHash) {
        await recordFailedAttempt(rateLimitId);
        throw new HttpsError("not-found", "Authentication credentials not found.");
    }

    const matches = await bcrypt.compare(currentPassword, credential.data().passwordHash);
    if (!matches) {
        await recordFailedAttempt(rateLimitId);
        throw new HttpsError("permission-denied", "Current password is incorrect.");
    }

    await clearRateLimit(rateLimitId);
    await db.collection("passwordChangeVerifications")
        .doc(request.auth.uid)
        .set({ verifiedAt: Date.now() });

    return { verified: true };
});

/**
 * 4. Find User For Reset (Unauthenticated lookup)
 */
exports.findUserForReset = onAppCheckCall(async (request) => {
    return {
        message: "If an account exists, continue with the verification process."
    };
});

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

async function getNotificationTokens(restaurantId, spec) {
    if (spec.targetStaffId) {
        const snapshot = await db.collection("users").doc(restaurantId)
            .collection("staff").doc(spec.targetStaffId).get();
        return snapshot.exists && snapshot.get("status") === "Active"
            ? [snapshot.get("fcmToken")].filter((token) => typeof token === "string" && token)
            : [];
    }

    if (spec.targetRole === "manager") {
        const [restaurantSnapshot, staffSnapshot] = await Promise.all([
            db.collection("restaurants").doc(restaurantId).get(),
            db.collection("users").doc(restaurantId).collection("staff")
                .where("restaurantId", "==", restaurantId)
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
    const snapshot = await db.collection("users").doc(restaurantId).collection("staff")
        .where("restaurantId", "==", restaurantId)
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
        const crypto = require("crypto");
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
        const restaurantId = paymentOrder.get("restaurantId");
        const restaurantRef = db.collection("restaurants").doc(restaurantId);
        const paymentRef = db.collection("payments").doc(paymentId);
        const ownerRef = db.collection("owners").doc(request.auth.uid);
        const subscriptionRef = db.collection("subscriptions").doc();
        const now = Date.now();
        const expiry = now + (plan.billingCycle === "YEARLY" ? 365 : 30) * 24 * 60 * 60 * 1000;
        const status = plan.billingCycle === "YEARLY" ? "ACTIVE_YEARLY" : "ACTIVE_MONTHLY";
        await db.runTransaction(async (transaction) => {
            const [freshOrder, restaurantSnapshot, existingPayment] = await Promise.all([
                transaction.get(paymentOrderRef),
                transaction.get(restaurantRef),
                transaction.get(paymentRef)
            ]);
            if (!freshOrder.exists || freshOrder.get("status") !== "CREATED"
                || freshOrder.get("ownerUid") !== request.auth.uid
                || !restaurantSnapshot.exists
                || restaurantSnapshot.get("ownerUid") !== request.auth.uid
                || existingPayment.exists) {
                throw new HttpsError("already-exists", "Payment order has already been applied.");
            }
            transaction.create(subscriptionRef, {
                subscriptionId: subscriptionRef.id,
                ownerUid: request.auth.uid,
                restaurantIds: [restaurantId],
                planId,
                status,
                billingCycle: plan.billingCycle,
                numericPrice: plan.amountRupees,
                currency: "INR",
                startDateTimestamp: now,
                expiryDateTimestamp: expiry,
                autoRenew: false,
                latestPaymentId: paymentId,
                updatedAt: now
            });
            transaction.create(paymentRef, {
                paymentId,
                orderId,
                subscriptionId: subscriptionRef.id,
                ownerUid: request.auth.uid,
                restaurantId,
                amount: plan.amountRupees,
                gateway: "RAZORPAY",
                status: "SUCCESS",
                createdAt: now
            });
            transaction.update(restaurantRef, {
                subscriptionId: subscriptionRef.id,
                subscriptionStatus: status,
                subscriptionExpiry: expiry,
                subscription: {
                    status,
                    planId,
                    startDateTimestamp: now,
                    expiryDateTimestamp: expiry,
                    autoRenew: false,
                    lastPaymentId: paymentId
                }
            });
            transaction.set(ownerRef, {
                ownerUid: request.auth.uid,
                activeSubscriptionIds: FieldValue.arrayUnion(subscriptionRef.id),
                ownedRestaurantIds: FieldValue.arrayUnion(restaurantId),
                maxAllowedOutlets: plan.maxAllowedOutlets
            }, { merge: true });
            transaction.update(paymentOrderRef, {
                status: "APPLIED",
                paymentId,
                subscriptionId: subscriptionRef.id,
                appliedAt: now
            });
        });
        return { success: true, subscriptionId: subscriptionRef.id };
    }
);
