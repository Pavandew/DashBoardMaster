const admin = require("firebase-admin");
const { FieldValue } = require("firebase-admin/firestore");
const bcrypt = require("bcryptjs");

const {
    db,
    HttpsError,
    STAFF_ROLES,
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
    clearRateLimit
} = require("./helpers");

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
    try {
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

        if (matches.size === 0) {
            for (const candidate of variants) {
                try {
                    const userSnap = await db.collection("users").where("staffId", "==", candidate).get();
                    for (const userDoc of userSnap.docs) {
                        const uData = userDoc.data();
                        if (uData.restaurantId && uData.staffDocId) {
                            const staffDoc = await db.collection("restaurants")
                                .doc(uData.restaurantId)
                                .collection("staff")
                                .doc(uData.staffDocId)
                                .get();
                            if (staffDoc.exists) {
                                matches.set(staffDoc.id, staffDoc);
                            }
                        }
                    }
                } catch (e) {
                    console.warn("⚠️ Fallback staff lookup error:", e.message);
                }
            }
        }

        if (matches.size === 0) {
            console.warn(`❌ [AuthStaff] No staff record found for staffId: '${staffId}'`);
            throw new HttpsError("not-found", `Staff ID '${staffId}' is not registered. Please check the Staff ID.`);
        }
        if (matches.size > 1) {
            console.warn(`❌ [AuthStaff] Multiple staff records found for staffId: '${staffId}'`);
            throw new HttpsError("failed-precondition", `Multiple staff records match '${staffId}'. Contact restaurant manager.`);
        }

        const staffSnapshot = [...matches.values()][0];
        const staffDocId = staffSnapshot.id;
        const pathParts = staffSnapshot.ref.path.split("/");
        const restaurantId = pathParts[1];
        const restaurantSnapshot = await db.collection("restaurants").doc(restaurantId).get();
        if (!restaurantSnapshot.exists) {
            console.warn(`❌ [AuthStaff] Restaurant '${restaurantId}' missing for staffId: '${staffId}'`);
            throw new HttpsError("not-found", "Restaurant associated with this staff ID was not found.");
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
        const role = String(staff.role || "").trim().toLowerCase();
        const staffStatus = staff.status || "Active";

        if (staffStatus !== "Active") {
            console.warn(`❌ [AuthStaff] Staff '${staffId}' status is '${staffStatus}'`);
            throw new HttpsError("permission-denied", `Staff account '${staffId}' is inactive or suspended.`);
        }

        if (!STAFF_ROLES.has(role)) {
            console.warn(`❌ [AuthStaff] Staff '${staffId}' role '${staff.role}' is not allowed`);
            throw new HttpsError("permission-denied", `Role '${staff.role}' is not allowed to log in to Staff Portal.`);
        }

        if (user && user.status && user.status !== "Active") {
            console.warn(`❌ [AuthStaff] Staff user profile status is '${user.status}'`);
            throw new HttpsError("permission-denied", `Staff user profile is inactive (${user.status}).`);
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
            console.warn(`❌ [AuthStaff] Password mismatch for staff ID: '${staffId}'`);
            await recordFailedAttempt(rateLimitId);
            throw new HttpsError("permission-denied", "Incorrect password. Please check your password and try again.");
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
    } catch (error) {
        console.error("❌ [authenticateStaff] Unexpected server exception:", error);
        if (error instanceof HttpsError) {
            throw error;
        }
        throw new HttpsError("permission-denied", "Invalid staff ID or password.");
    }
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

    await staffRef.set({
        staffId: user.staffId,
        restaurantId: restaurantId,
        role: user.role,
        status: user.status,
        fcmToken: token
    }, { merge: true });

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
