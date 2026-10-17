const admin = require("firebase-admin");
const { FieldValue } = require("firebase-admin/firestore");
const bcrypt = require("bcryptjs");

const {
    db,
    HttpsError,
    onAppCheckCall,
    normalizeMobile,
    checkRateLimit,
    recordFailedAttempt,
    clearRateLimit
} = require("./helpers");

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

    const uid = request.auth.uid;
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

    const mappingRef = db.collection("mobileMappings").doc(cleanMobile);
    const mappingDoc = await mappingRef.get();
    if (mappingDoc.exists) {
        console.warn("⚠️ [Register] Mobile number already registered:", cleanMobile);
        throw new HttpsError("already-exists", "This mobile number is already registered.");
    }

    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(password, salt);

    await db.collection("authCredentials").doc(uid).set({
        uid: uid,
        mobile: cleanMobile,
        passwordHash: passwordHash,
        createdAt: Date.now()
    });

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

/**
 * 2. Unified Authentication Function (Owner & Staff)
 * Receives mobile + password -> verifies bcrypt -> returns custom token.
 */
exports.authenticateUser = onAppCheckCall(async (request) => {
    try {
        const { mobile, password } = request.data || {};
        console.log("🔐 [Auth] Request received for mobile:", mobile);

        if (typeof mobile !== "string" || !mobile.trim()
            || typeof password !== "string" || !password) {
            console.error("❌ [Auth] Invalid argument structure submitted.");
            throw new HttpsError("invalid-argument", "Mobile number and password are required.");
        }

        const cleanMobile = normalizeMobile(mobile);

        try {
            await checkRateLimit(cleanMobile);
        } catch (error) {
            if (error instanceof HttpsError && error.code === "resource-exhausted") {
                console.error("⚠️ [Auth] Rate limit exceeded for mobile:", cleanMobile);
                throw new HttpsError("resource-exhausted", error.message);
            }
            throw error;
        }

        const mappingDoc = await db.collection("mobileMappings").doc(cleanMobile).get();
        let mappedUid = mappingDoc.exists && typeof mappingDoc.get("uid") === "string"
            ? mappingDoc.get("uid")
            : "";

        if (!mappedUid) {
            console.log("🔍 [Auth] Mobile mapping missing for:", cleanMobile, ". Searching users collection...");
            let userQuery = await db.collection("users").where("mobile", "==", cleanMobile).get();
            if (userQuery.empty) {
                userQuery = await db.collection("users").where("phone", "==", cleanMobile).get();
            }
            if (!userQuery.empty) {
                mappedUid = userQuery.docs[0].id;
                console.log("🟢 [Auth] Found user in users collection! UID:", mappedUid, ". Auto-healing mobileMappings...");
                await db.collection("mobileMappings").doc(cleanMobile).set({
                    uid: mappedUid,
                    createdAt: Date.now()
                }, { merge: true });
            }
        }

        if (!mappedUid) {
            console.warn("❌ [Auth] Unregistered mobile number:", cleanMobile);
            throw new HttpsError("not-found", "Mobile number is not registered. Please sign up first.");
        }

        const [credentialSnapshot, userSnapshot] = await Promise.all([
            db.collection("authCredentials").doc(mappedUid).get(),
            db.collection("users").doc(mappedUid).get()
        ]);

        if (!userSnapshot || !userSnapshot.exists) {
            console.warn("❌ [Auth] User profile missing for UID:", mappedUid);
            throw new HttpsError("not-found", "User account profile not found. Please contact support or sign up again.");
        }

        const user = userSnapshot.data() || {};

        if (user.status !== "Active") {
            console.warn("❌ [Auth] Inactive user account for UID:", mappedUid, "Status:", user.status);
            throw new HttpsError("permission-denied", "Your account is currently inactive or suspended. Please contact support.");
        }

        if (user.isVerified !== true) {
            console.warn("❌ [Auth] Unverified user account for UID:", mappedUid);
            throw new HttpsError("permission-denied", "Account is not verified. Please complete verification.");
        }

        const storedHash = credentialSnapshot?.exists
            ? credentialSnapshot.get("passwordHash")
            : null;
        const legacyPassword = user.password || "";

        const passwordMatches = storedHash
            ? await bcrypt.compare(password, storedHash)
            : legacyPassword ? legacyPassword === password : false;

        if (!passwordMatches) {
            console.warn("❌ [Auth] Password mismatch for mobile:", cleanMobile, "| UID:", mappedUid);
            await recordFailedAttempt(cleanMobile);
            throw new HttpsError("unauthenticated", "Incorrect password. Please check your password and try again.");
        }

        if (!credentialSnapshot.exists || legacyPassword) {
            console.log("🟢 [Auth] Auto-healing authCredentials for UID:", mappedUid);
            const migratedHash = storedHash || await bcrypt.hash(password, await bcrypt.genSalt(10));
            const batch = db.batch();
            batch.set(db.collection("authCredentials").doc(mappedUid), {
                uid: mappedUid,
                mobile: cleanMobile,
                passwordHash: migratedHash,
                createdAt: Date.now()
            }, { merge: true });
            if (userSnapshot.get("password")) {
                batch.update(userSnapshot.ref, { password: FieldValue.delete() });
            }
            await batch.commit();
        }

        await clearRateLimit(cleanMobile);
        const customToken = await admin.auth().createCustomToken(mappedUid);
        console.log("🟢 [Auth] Authentication successful! Custom token generated for UID:", mappedUid);
        return { token: customToken, uid: mappedUid };
    } catch (error) {
        console.error("❌ [authenticateUser] Unexpected server exception:", error);
        if (error instanceof HttpsError) {
            throw error;
        }
        throw new HttpsError("internal", error.message || "Authentication service error.");
    }
});

/**
 * 3. Update Password
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
    if (!request.auth || !request.auth.uid) {
        throw new HttpsError("unauthenticated", "Authentication required to reset password.");
    }

    const uid = request.auth.uid;
    const newPassword = request.data?.newPassword;

    if (typeof newPassword !== "string" || newPassword.length < 6 || newPassword.length > 128) {
        throw new HttpsError("invalid-argument", "Password must be between 6 and 128 characters.");
    }

    const userSnapshot = await db.collection("users").doc(uid).get();
    if (userSnapshot.exists) {
        const user = userSnapshot.data();
        const mobile = normalizeMobile(user.mobile || user.phone);
        if (mobile && /^\+\d{7,15}$/.test(mobile)) {
            console.log("🟢 [ResetPassword] Auto-healing mobileMappings for:", mobile, "UID:", uid);
            await db.collection("mobileMappings").doc(mobile).set({
                uid: uid,
                createdAt: Date.now()
            }, { merge: true });
        }
    }

    const passwordHash = await bcrypt.hash(newPassword, await bcrypt.genSalt(10));
    await db.collection("authCredentials").doc(uid).set({
        uid: uid,
        passwordHash: passwordHash,
        updatedAt: Date.now()
    }, { merge: true });

    console.log("🟢 [ResetPassword] Password & Credentials successfully updated for UID:", uid);
    return { success: true };
});

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

exports.findUserForReset = onAppCheckCall(async (request) => {
    return {
        message: "If an account exists, continue with the verification process."
    };
});
