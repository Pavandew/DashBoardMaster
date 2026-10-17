# Master Dashboard - Web Authentication & Integration Specification

> **AI / Web Developer Prompt Instructions:**
> Use this document as the single source of truth to implement or update Authentication, User Profiles, and Firebase Cloud Function integrations on the Web Application (React, Next.js, Vue, or Vanilla JS).

---

## 1. System Architecture Overview

* **Backend Engine:** Firebase Cloud Functions (2nd Gen, Node.js 22) + Cloud Firestore + Firebase Authentication.
* **Authentication Model:** Server-Side Bcrypt Credential Verification -> Custom Auth Token -> `signInWithCustomToken()`.
* **Database Models:** Multi-tenant Firestore document structure with strict security rules (`firestore.rules`).

---

## 2. Web Dependencies Setup

Install the required Firebase JS SDK:

```bash
npm install firebase
```

Initialize Firebase in your Web Project:

```javascript
// firebase.js
import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getFunctions } from "firebase/functions";

const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "masterdashboard-836dd.firebaseapp.com",
  projectId: "masterdashboard-836dd",
  storageBucket: "masterdashboard-836dd.firebasestorage.app",
  messagingSenderId: "252505058012",
  appId: "YOUR_APP_ID"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const functions = getFunctions(app);
```

---

## 3. Web Authentication Flows & Code Examples

### Flow A: Owner / Single Restaurant / Multi-Restaurant Login

To log in an Owner or Manager using **Mobile Number + Password**:

```javascript
import { httpsCallable } from "firebase/functions";
import { signInWithCustomToken } from "firebase/auth";
import { doc, getDoc } from "firebase/firestore";
import { auth, db, functions } from "./firebase";

export async function loginOwnerOrManager(mobile, password) {
  try {
    // 1. Format mobile number to +91XXXXXXXXXX
    const cleanDigits = mobile.replace(/\D/g, "");
    const formattedMobile = cleanDigits.length === 10 ? `+91${cleanDigits}` : mobile;

    // 2. Call authenticateUser Cloud Function
    const authenticateUser = httpsCallable(functions, "authenticateUser");
    const result = await authenticateUser({
      mobile: formattedMobile,
      password: password
    });

    const { token, uid } = result.data;

    // 3. Sign in to Firebase Auth using the Custom Token
    const userCredential = await signInWithCustomToken(auth, token);

    // 4. Fetch User Profile from Firestore users/{uid}
    const userDocRef = doc(db, "users", uid);
    const userSnap = await getDoc(userDocRef);

    if (!userSnap.exists()) {
      throw new Error("User profile not found.");
    }

    const userData = userSnap.data();
    return {
      uid: userCredential.user.uid,
      role: userData.role,
      portalType: userData.portalType,
      restaurantId: userData.restaurantId || "",
      isSetupComplete: userData.isSetupComplete || false,
      fullName: userData.fullName || userData.ownerFullName || ""
    };
  } catch (error) {
    // Handle error codes: 'not-found', 'unauthenticated', 'permission-denied', 'resource-exhausted'
    console.error("Login Error:", error.message);
    throw error;
  }
}
```

---

### Flow B: Staff Login (Waiters, Chefs, Cashiers, Managers)

To log in a Staff Member using **Staff ID + Password**:

```javascript
import { httpsCallable } from "firebase/functions";
import { signInWithCustomToken } from "firebase/auth";
import { auth, functions } from "./firebase";

export async function loginStaff(staffId, password) {
  try {
    // 1. Call authenticateStaff Cloud Function
    const authenticateStaff = httpsCallable(functions, "authenticateStaff");
    const result = await authenticateStaff({
      staffId: staffId.trim(),
      password: password
    });

    const { token, uid, restaurantId, ownerUid, profile } = result.data;

    // 2. Sign in to Firebase Auth using Custom Token
    await signInWithCustomToken(auth, token);

    return {
      uid,
      restaurantId,
      ownerUid,
      staffId: profile.staffId,
      staffName: profile.staffName,
      role: profile.role, // 'waiter', 'chef', 'cashier', 'manager'
      permissions: profile.permissions || []
    };
  } catch (error) {
    console.error("Staff Login Error:", error.message);
    throw error;
  }
}
```

---

### Flow C: Owner Sign Up & Phone OTP Verification

1. **Send Phone OTP (Client Side):**

```javascript
import { RecaptchaVerifier, signInWithPhoneNumber } from "firebase/auth";
import { auth } from "./firebase";

export function setupRecaptcha(containerId) {
  return new RecaptchaVerifier(auth, containerId, { size: "invisible" });
}

export async function sendPhoneOtp(mobileNumber, recaptchaVerifier) {
  const cleanDigits = mobileNumber.replace(/\D/g, "");
  const formattedPhone = cleanDigits.length === 10 ? `+91${cleanDigits}` : mobileNumber;
  
  const confirmationResult = await signInWithPhoneNumber(auth, formattedPhone, recaptchaVerifier);
  return confirmationResult; // Save this object to verify code
}
```

2. **Verify OTP & Complete Registration:**

```javascript
import { httpsCallable } from "firebase/functions";
import { doc, setDoc } from "firebase/firestore";
import { db, functions } from "./firebase";

export async function completeRegistration(confirmationResult, otpCode, { fullName, mobile, password, role, portalType }) {
  // 1. Confirm Phone OTP code
  const authResult = await confirmationResult.confirm(otpCode);
  const uid = authResult.user.uid;

  const cleanDigits = mobile.replace(/\D/g, "");
  const formattedMobile = cleanDigits.length === 10 ? `+91${cleanDigits}` : mobile;

  // 2. Save secure bcrypt credentials via Cloud Function
  const registerUser = httpsCallable(functions, "registerUser");
  await registerUser({
    mobile: formattedMobile,
    password: password
  });

  // 3. Save User Profile in users/{uid}
  const userProfile = {
    uid: uid,
    fullName: fullName.trim(),
    mobile: formattedMobile,
    role: role, // 'owner_single' or 'owner_multi'
    portalType: portalType, // 'restaurant_portal' or 'multi_restaurant'
    isVerified: true,
    status: "Active",
    isSetupComplete: false,
    restaurantId: ""
  };

  await setDoc(doc(db, "users", uid), userProfile);
  return uid;
}
```

---

## 4. Error Code Handling Reference

When calling Cloud Functions, caught errors contain clean, user-friendly messages:

| Error Message Contains | UI Action / Field |
| :--- | :--- |
| `"not registered"` / `"sign up"` | Display error on **Phone / Staff ID** input field. Suggest Sign Up. |
| `"Incorrect password"` / `"mismatch"` | Display error on **Password** input field. |
| `"inactive"` / `"suspended"` | Show alert dialog: *"Account suspended. Contact support."* |
| `"Too many failed login attempts"` | Show alert dialog: *"Rate limit exceeded. Try again in 15 minutes."* |

---

## 5. Portal & Role Access Matrix

| Portal Type (`portalType`) | Allowed User Roles (`role`) | Primary Workspace Screen |
| :--- | :--- | :--- |
| `multi_restaurant` | `owner_multi` | Master Multi-Outlet Dashboard |
| `restaurant_portal` | `owner_single`, `manager` | Single Restaurant Dashboard |
| `staff` | `waiter`, `waiter_staff`, `chef`, `kitchen`, `cashier`, `billing`, `manager` | Role-Specific Workspace (POS / KDS / Cashier) |

---

## 6. Summary for AI / Web Developer

* All authentication business logic is centralized in Firebase Cloud Functions (`authenticateUser`, `authenticateStaff`, `registerUser`).
* All phone numbers **MUST** be formatted as `+91XXXXXXXXXX`.
* Custom Tokens returned by backend Cloud Functions **MUST** be passed to `signInWithCustomToken(auth, token)`.
* After `signInWithCustomToken()`, user permissions and profiles can be listened to or fetched from Firestore `users/{uid}` and `restaurants/{restaurantId}`.
