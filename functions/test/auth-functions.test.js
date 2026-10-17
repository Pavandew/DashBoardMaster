const assert = require("node:assert/strict");
const { after, before, beforeEach, describe, it } = require("node:test");
const admin = require("firebase-admin");
const bcrypt = require("bcryptjs");

const projectId = "demo-dashboardmaster-rules";
const functionsUrl = `http://127.0.0.1:5001/${projectId}/us-central1`;
const authUrl = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1";
const MOBILE = "+919876543210";
const UID = "auth-test-owner";
const OLD_PASSWORD = "old-password-123";
const encodeTestJwtPart = (value) => Buffer.from(JSON.stringify(value)).toString("base64url");
const EMULATOR_APP_CHECK_TOKEN = [
    encodeTestJwtPart({ alg: "none" }),
    encodeTestJwtPart({ sub: "local-test-app" }),
    "emulator-only"
].join(".");

assert.equal(
    process.versions.node.split(".")[0],
    "22",
    `Authentication emulator tests require Node 22, received ${process.versions.node}`
);

if (!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_AUTH_EMULATOR_HOST) {
    throw new Error("Run these tests with the Firestore, Auth, and Functions emulators.");
}
if (process.env.FIREBASE_DEBUG_MODE !== "true"
    || !process.env.FIREBASE_DEBUG_FEATURES?.includes("skipTokenVerification")) {
    throw new Error("Use emulator-only App Check token verification bypass for callable tests.");
}

if (!admin.apps.length) {
    admin.initializeApp({ projectId });
}
const db = admin.firestore();
const auth = admin.auth();

async function clearCollection(name) {
    await db.recursiveDelete(db.collection(name));
}

async function callable(name, data, idToken, includeAppCheck = true) {
    const response = await fetch(`${functionsUrl}/${name}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...(includeAppCheck ? { "X-Firebase-AppCheck": EMULATOR_APP_CHECK_TOKEN } : {}),
            ...(idToken ? { Authorization: `Bearer ${idToken}` } : {})
        },
        body: JSON.stringify({ data })
    });
    const body = await response.json();
    return response.ok
        ? { ok: true, result: body.result }
        : { ok: false, code: body.error?.status, message: body.error?.message };
}

async function createIdToken(uid, phoneNumber) {
    await auth.createUser({ uid, phoneNumber });
    const customToken = await auth.createCustomToken(uid);
    const response = await fetch(`${authUrl}/accounts:signInWithCustomToken?key=fake-api-key`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token: customToken, returnSecureToken: true })
    });
    const body = await response.json();
    assert.equal(response.ok, true, JSON.stringify(body));
    return body.idToken;
}

async function createPhoneIdToken(phoneNumber) {
    const sent = await fetch(`${authUrl}/accounts:sendVerificationCode?key=fake-api-key`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ phoneNumber, recaptchaToken: "emulator" })
    });
    const sentBody = await sent.json();
    assert.equal(sent.ok, true, JSON.stringify(sentBody));

    const codesResponse = await fetch(
        `http://127.0.0.1:9099/emulator/v1/projects/${projectId}/verificationCodes`
    );
    const codesBody = await codesResponse.json();
    const verification = codesBody.verificationCodes?.find((entry) =>
        entry.phoneNumber === phoneNumber && entry.sessionInfo === sentBody.sessionInfo
    );
    assert.ok(verification, `No emulator verification code found for ${phoneNumber}`);

    const signedIn = await fetch(`${authUrl}/accounts:signInWithPhoneNumber?key=fake-api-key`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            sessionInfo: sentBody.sessionInfo,
            code: verification.code,
            returnSecureToken: true
        })
    });
    const signedInBody = await signedIn.json();
    assert.equal(signedIn.ok, true, JSON.stringify(signedInBody));
    return { idToken: signedInBody.idToken, uid: signedInBody.localId };
}

async function seedLoginAccount({ uid, mobile, password, status = "Active" }) {
    const passwordHash = await bcrypt.hash(password, 4);
    await Promise.all([
        db.collection("mobileMappings").doc(mobile).set({ uid }),
        db.collection("authCredentials").doc(uid).set({ uid, passwordHash }),
        db.collection("users").doc(uid).set({
            uid,
            mobile,
            role: "owner_single",
            status,
            isVerified: true
        })
    ]);
}

describe("Authentication callable emulator tests", () => {
    const authUserIds = new Set();

    before(async () => {
        await clearCollection("users");
        await clearCollection("restaurants");
        await clearCollection("authCredentials");
        await clearCollection("mobileMappings");
        await clearCollection("authRateLimits");
        await clearCollection("passwordChangeVerifications");
    });

    beforeEach(async () => {
        for (const uid of authUserIds) {
            await auth.deleteUser(uid).catch(() => {});
        }
        authUserIds.clear();
        await Promise.all([
            clearCollection("users"),
            clearCollection("restaurants"),
            clearCollection("authCredentials"),
            clearCollection("mobileMappings"),
            clearCollection("authRateLimits"),
            clearCollection("passwordChangeVerifications")
        ]);
    });

    after(async () => {
        await Promise.all([
            clearCollection("users"),
            clearCollection("restaurants"),
            clearCollection("authCredentials"),
            clearCollection("mobileMappings"),
            clearCollection("authRateLimits"),
            clearCollection("passwordChangeVerifications")
        ]);
        for (const uid of authUserIds) {
            await auth.deleteUser(uid).catch(() => {});
        }
        await admin.app().delete();
    });

    it("rejects callable requests without App Check and accepts the emulator test token", async () => {
        const withoutAppCheck = await callable("isMobileRegistered", {}, undefined, false);
        assert.equal(withoutAppCheck.ok, false);
        assert.equal(withoutAppCheck.code, "UNAUTHENTICATED");

        const withAppCheck = await callable("isMobileRegistered", { mobile: MOBILE });
        assert.deepEqual(withAppCheck, {
            ok: true,
            result: { registered: false }
        });
    });

    it("returns the same generic login error for a wrong password and unknown mobile", async () => {
        await seedLoginAccount({ uid: UID, mobile: MOBILE, password: OLD_PASSWORD });
        const wrong = await callable("authenticateUser", {
            mobile: MOBILE,
            password: "incorrect-password"
        });
        const unknown = await callable("authenticateUser", {
            mobile: "+919876500000",
            password: "incorrect-password"
        });

        assert.deepEqual(
            { code: wrong.code, message: wrong.message },
            { code: "UNAUTHENTICATED", message: "Invalid mobile number or password." }
        );
        assert.deepEqual(
            { code: unknown.code, message: unknown.message },
            { code: wrong.code, message: wrong.message }
        );
        const missingCredentialUid = "missing-credential-owner";
        await seedLoginAccount({
            uid: missingCredentialUid,
            mobile: "+919876543211",
            password: OLD_PASSWORD
        });
        await db.collection("authCredentials").doc(missingCredentialUid).delete();
        const missingCredential = await callable("authenticateUser", {
            mobile: "+919876543211",
            password: OLD_PASSWORD
        });
        assert.deepEqual(
            { code: missingCredential.code, message: missingCredential.message },
            { code: wrong.code, message: wrong.message }
        );
        assert.equal((await db.collection("authRateLimits").doc(MOBILE).get()).get("attempts"), 1);
        assert.equal((await db.collection("authRateLimits").doc("+919876500000").get()).get("attempts"), 1);
        assert.equal("result" in wrong, false);
        assert.equal("result" in unknown, false);
    });

    it("returns the same generic login error for inactive and unverified accounts", async () => {
        await seedLoginAccount({
            uid: UID,
            mobile: MOBILE,
            password: OLD_PASSWORD,
            status: "Suspended"
        });
        const result = await callable("authenticateUser", {
            mobile: MOBILE,
            password: OLD_PASSWORD
        });

        assert.equal(result.ok, false);
        assert.equal(result.code, "UNAUTHENTICATED");
        assert.equal(result.message, "Invalid mobile number or password.");

        await db.collection("users").doc(UID).update({
            status: "Active",
            isVerified: false
        });
        const unverified = await callable("authenticateUser", {
            mobile: MOBILE,
            password: OLD_PASSWORD
        });
        assert.equal(unverified.ok, false);
        assert.equal(unverified.code, "UNAUTHENTICATED");
        assert.equal(unverified.message, "Invalid mobile number or password.");
    });

    it("keeps login lockout enabled while returning the generic failure response", async () => {
        await seedLoginAccount({ uid: UID, mobile: MOBILE, password: OLD_PASSWORD });
        for (let attempt = 0; attempt < 6; attempt += 1) {
            const result = await callable("authenticateUser", {
                mobile: MOBILE,
                password: "incorrect-password"
            });
            assert.equal(result.code, "UNAUTHENTICATED");
            assert.equal(result.message, "Invalid mobile number or password.");
        }
        const rateLimit = await db.collection("authRateLimits").doc(MOBILE).get();
        assert.equal(rateLimit.exists, true);
        assert.equal(rateLimit.get("attempts"), 6);
        assert.ok(rateLimit.get("lockoutUntil") > Date.now());
    });

    it("authenticates an active account with valid credentials", async () => {
        await seedLoginAccount({ uid: UID, mobile: MOBILE, password: OLD_PASSWORD });
        const result = await callable("authenticateUser", {
            mobile: MOBILE,
            password: OLD_PASSWORD
        });

        assert.equal(result.ok, true);
        assert.equal(result.result.uid, UID);
        assert.equal(typeof result.result.token, "string");
        assert.ok(result.result.token.length > 0);
    });

    it("does not disclose registration state through the pre-signup mobile check", async () => {
        await seedLoginAccount({ uid: UID, mobile: MOBILE, password: OLD_PASSWORD });
        const known = await callable("isMobileRegistered", { mobile: MOBILE });
        const unknown = await callable("isMobileRegistered", { mobile: "+919876500000" });

        assert.deepEqual(known, { ok: true, result: { registered: false } });
        assert.deepEqual(unknown, known);
    });

    it("preserves the existing credential when password verification fails", async () => {
        const passwordHash = await bcrypt.hash(OLD_PASSWORD, 4);
        await db.collection("users").doc(UID).set({
            uid: UID,
            mobile: MOBILE,
            role: "owner_single",
            status: "Active",
            isVerified: true
        });
        await db.collection("authCredentials").doc(UID).set({ uid: UID, passwordHash });
        const idToken = await createIdToken(UID, "+14155550101");
        authUserIds.add(UID);

        const result = await callable("updatePassword", { newPassword: "new-password-123" }, idToken);
        assert.equal(result.ok, false);
        assert.equal(result.message, "Unable to update password. Verify your account and try again.");

        const storedHash = (await db.collection("authCredentials").doc(UID).get()).get("passwordHash");
        assert.equal(storedHash, passwordHash);
        assert.equal(await bcrypt.compare(OLD_PASSWORD, storedHash), true);
        assert.equal(await bcrypt.compare("new-password-123", storedHash), false);
    });

    it("changes the credential only after a valid password verification record", async () => {
        const passwordHash = await bcrypt.hash(OLD_PASSWORD, 4);
        await db.collection("users").doc(UID).set({
            uid: UID,
            mobile: MOBILE,
            role: "owner_single",
            status: "Active",
            isVerified: true
        });
        await db.collection("authCredentials").doc(UID).set({ uid: UID, passwordHash });
        await db.collection("passwordChangeVerifications").doc(UID).set({
            verifiedAt: Date.now()
        });
        const idToken = await createIdToken(UID, "+14155550101");
        authUserIds.add(UID);

        const result = await callable("updatePassword", { newPassword: "new-password-123" }, idToken);
        assert.deepEqual(result, { ok: true, result: { success: true } });

        const credential = await db.collection("authCredentials").doc(UID).get();
        const storedHash = credential.get("passwordHash");
        assert.notEqual(storedHash, passwordHash);
        assert.equal(await bcrypt.compare("new-password-123", storedHash), true);
        assert.equal(await bcrypt.compare(OLD_PASSWORD, storedHash), false);
        assert.equal((await db.collection("passwordChangeVerifications").doc(UID).get()).exists, false);
    });

    it("allows a valid phone-verified account to complete the reset flow", async () => {
        const passwordHash = await bcrypt.hash(OLD_PASSWORD, 4);
        await db.collection("mobileMappings").doc(MOBILE).set({ uid: UID });
        await db.collection("users").doc(UID).set({
            uid: UID,
            mobile: MOBILE,
            role: "owner_single",
            status: "Active",
            isVerified: true
        });
        await db.collection("authCredentials").doc(UID).set({ uid: UID, passwordHash });
        const { idToken, uid } = await createPhoneIdToken(MOBILE);
        authUserIds.add(uid);

        const resetRequest = await callable("findUserForReset", { mobile: MOBILE });
        assert.deepEqual(resetRequest, {
            ok: true,
            result: {
                message: "If an account exists, continue with the verification process."
            }
        });
        const result = await callable("resetPassword", { newPassword: "new-password-123" }, idToken);
        assert.deepEqual(result, { ok: true, result: { success: true } });

        const storedHash = (await db.collection("authCredentials").doc(UID).get()).get("passwordHash");
        assert.equal(await bcrypt.compare("new-password-123", storedHash), true);
        assert.equal(await bcrypt.compare(OLD_PASSWORD, storedHash), false);
    });

    it("does not reset a password for an unknown phone even after OTP verification", async () => {
        const unknownPhone = "+14155550199";
        const { idToken, uid } = await createPhoneIdToken(unknownPhone);
        authUserIds.add(uid);
        const result = await callable(
            "resetPassword",
            { newPassword: "new-password-123" },
            idToken
        );

        assert.deepEqual(
            { code: result.code, message: result.message },
            {
                code: "FAILED_PRECONDITION",
                message: "Unable to reset password. Verify your account and try again."
            }
        );
        assert.equal((await db.collection("authCredentials").get()).empty, true);
    });

    it("resets an active staff account through its trusted roster mapping", async () => {
        const staffPhone = "+14155550188";
        const staffUid = "auth-test-staff";
        const staffDocId = "staff-roster-doc-88";
        await db.collection("restaurants").doc("restaurant-reset").set({
            ownerUid: "restaurant-owner"
        });
        await db.collection("users").doc(staffUid).set({
            uid: staffUid,
            restaurantId: "restaurant-reset",
            ownerUid: "restaurant-owner",
            staffId: "S-88",
            staffDocId,
            mobile: staffPhone,
            role: "waiter",
            status: "Active"
        });
        await db.collection("restaurants").doc("restaurant-reset")
            .collection("staff").doc(staffDocId).set({
                staffId: "S-88",
                mobile: staffPhone,
                role: "waiter",
                status: "Active",
                password: "legacy-plaintext"
            });
        const oldHash = await bcrypt.hash(OLD_PASSWORD, 4);
        await db.collection("authCredentials").doc(staffUid).set({ uid: staffUid, passwordHash: oldHash });
        const { idToken, uid } = await createPhoneIdToken(staffPhone);
        authUserIds.add(uid);

        const result = await callable(
            "resetPassword",
            { newPassword: "new-password-123" },
            idToken
        );
        assert.deepEqual(result, { ok: true, result: { success: true } });

        const storedHash = (await db.collection("authCredentials").doc(staffUid).get()).get("passwordHash");
        assert.equal(await bcrypt.compare("new-password-123", storedHash), true);
        assert.equal(await bcrypt.compare(OLD_PASSWORD, storedHash), false);
        assert.equal((await db.collection("restaurants").doc("restaurant-reset")
            .collection("staff").doc(staffDocId).get()).get("password"), undefined);
    });

    it("returns the same privacy-preserving reset response for known and unknown mobile numbers", async () => {
        await db.collection("users").doc(UID).set({
            uid: UID,
            mobile: MOBILE,
            fullName: "Private Test User",
            role: "owner_single",
            status: "Active"
        });

        const known = await callable("findUserForReset", { mobile: MOBILE });
        const unknown = await callable("findUserForReset", { mobile: "+919876500000" });
        const expected = {
            ok: true,
            result: {
                message: "If an account exists, continue with the verification process."
            }
        };
        assert.deepEqual(known, expected);
        assert.deepEqual(unknown, expected);
        for (const privateField of ["uid", "fullName", "mobile", "role"]) {
            assert.equal(privateField in known.result, false);
            assert.equal(privateField in unknown.result, false);
        }
    });
});
