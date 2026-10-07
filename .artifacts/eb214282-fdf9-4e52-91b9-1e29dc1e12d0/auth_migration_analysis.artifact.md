# Authentication & Security Migration Analysis: Restroone Android App

This document provides a comprehensive architectural and code analysis for migrating the Restroone application from the current insecure Firestore-based credential lookup to a production-grade **Firebase Authentication** architecture.

---

## 1. Current Authentication Flow & Security Audit

### Owner Registration & Login Flow:
1. **Registration:** `SignUpActivity` -> `SignUpViewModel` checks Firestore (`users` collection) -> `OTPVerificationActivity` verifies phone via Firebase Phone Auth -> User profile is saved in Firestore with `passwordHash = password.hashCode().toString()`.
2. **Normal Login:** `LoginActivity` -> `LoginViewModel` queries Firestore `users` collection by mobile -> Compares `user.passwordHash == repository.hashPassword(password)` (where `hashPassword` uses non-cryptographic `String.hashCode()`).
3. **Staff Login:** `StaffLoginActivity` -> `StaffLoginViewModel` queries collectionGroup `staff` -> Compares plain-text `staffProfile.password == cleanPass`.

### Critical Security Flaws:
* **Passwords stored in Firestore:** Both owner and staff documents store password representations (`passwordHash` / plain text) directly in Firestore.
* **Non-Cryptographic Hashing:** Java's `String.hashCode()` is a 32-bit integer that is trivial to reverse or brute-force.
* **Lack of Auth Tokens:** Firestore Security Rules currently rely on `if true;` or custom client flags because `FirebaseAuth.currentUser` is not tied to the password login flow.

---

## 2. Files Involved in Current Authentication

* **Activities & Fragments:**
  * `com/restroone/app/login/views/LoginActivity.kt`
  * `com/restroone/app/login/views/StaffLoginActivity.kt`
  * `com/restroone/app/login/views/SignUpActivity.kt`
  * `com/restroone/app/login/views/OTPVerificationActivity.kt`
* **ViewModels:**
  * `com/restroone/app/login/viewmodel/LoginViewModel.kt`
  * `com/restroone/app/login/viewmodel/StaffLoginViewModel.kt`
  * `com/restroone/app/login/viewmodel/SignUpViewModel.kt`
  * `com/restroone/app/login/viewmodel/OtpViewModel.kt`
* **Repositories & Helpers:**
  * `com/restroone/app/login/repo/AuthRepository.kt`
  * `com/restroone/app/login/repo/StaffLoginRepository.kt`
  * `com/restroone/app/utils/SessionManager.kt`

---

## 3. What Needs to Change

1. **Remove Passwords from Firestore:**
   * Delete `passwordHash` / plain `password` fields from `UserModelData` and `StaffDataModel` (and Firestore documents).
2. **Implement Secure Server-Side Login (Cloud Function + Custom Token):**
   * Since Firebase Auth does not natively support "Mobile Number + Password" out-of-the-box (it expects Email + Password or Phone OTP), a secure **Firebase Cloud Function** is required to authenticate mobile numbers against securely hashed passwords and issue a **Firebase Custom Token**.
3. **Client-Side Auth Integration:**
   * Instead of verifying password locally or checking Firestore fields, the client sends `mobile` + `password` to the Cloud Function, receives a Custom Token, and calls `FirebaseAuth.getInstance().signInWithCustomToken(token)`.
4. **Firestore Security Rules:**
   * Replace insecure `allow read, write: if true;` rules with strict rules based on `request.auth.uid`, checking owner/manager/staff roles and restaurant ID isolation (`restaurants/{restaurantId}`).

---

## 4. What Can Remain Unchanged

* **UI & Design:** All Activities, Fragments, XML layouts, ViewBinding, and design systems remain 100% intact.
* **MVVM Structure:** ViewModels, UI States, and Repositories structure remain preserved.
* **Navigation & Routing:** Portal selection (`ActivityVisitorPortal`), Splash routing, and role-based dashboard redirection (`MasterHomeActivity`, `ManagerHomeActivity`, `WaiterHomeActivity`, `KitchenHomeActivity`, `CashierHomeActivity`) remain unchanged.
* **Firestore Schema:** Database structure (`users/{uid}`, `restaurants/{restaurantId}/...`, `staff`) remains unchanged, minus the password fields.

---

## 5. New Firebase / Cloud Function Components Required

### A. Firebase Authentication Console
* Enable **Phone Authentication** (for registration OTP).
* Enable **Custom Token Authentication** (issued by Cloud Function).

### B. Firebase Cloud Function (Node.js / TypeScript)
1. **`verifyMobilePassword` Function (HTTPS Callable):**
   * Receives `mobile` and `password`.
   * Queries Firestore `users` collection (or staff collection) to find the user.
   * Verifies password using a secure cryptographic hash (e.g., `bcrypt` stored in a secure admin collection or secure auth record).
   * Generates and returns a Firebase Custom Token: `admin.auth().createCustomToken(uid)`.
2. **Staff Login Cloud Function:**
   * Similar secure verification for restaurant staff credentials.

---

## 6. Firestore Security Rule Changes Required

Once Firebase Auth `request.auth.uid` is established, rules will enforce:
* **Authentication Required:** `allow read, write: if request.auth != null;`
* **Restaurant Isolation:** Users can only access data inside their assigned `restaurantId`.
* **Role Enforcement:** Only owners/managers can modify staff or business settings.

---

## 7. Migration Plan for Existing Users

* **The Challenge:** Because the current app uses `String.hashCode()` (which cannot be reversed into plain text to re-hash with bcrypt), existing users will not have valid bcrypt password hashes in the secure server backend.
* **Migration Strategy:**
  1. On first login after the update, if a legacy user attempts login, or via a smooth migration prompt, prompt them to set/reset their password via OTP verification, or automatically migrate their account upon first valid password authentication.
  2. Alternatively, provide a "Forgot Password / Reset via OTP" flow for all existing users.

---

## 8. Dependencies & Configuration Changes

* **Gradle Dependencies:** `com.google.firebase:firebase-auth-ktx` (already present).
* **Firebase SDK:** Node.js Firebase Admin SDK (for Cloud Functions backend).

---

## 9. Production Feasibility & Alternative Ideas

### Is this feasible for a production application?
**Yes, absolutely.** In fact, it is **mandatory** for any production-grade restaurant POS application handling financial transactions, billing, inventory, and staff management. Leaving passwords in Firestore is a critical security vulnerability.

### Alternative / Better Idea:
Instead of maintaining a separate custom password verification table in Firestore via Cloud Functions, you can map mobile numbers to dummy email accounts behind the scenes for Firebase Auth (e.g., `9876543210@restroone.auth.com`), allowing you to use native `FirebaseAuth.signInWithEmailAndPassword(...)` directly on the Android client without needing Cloud Functions. However, **Cloud Functions + Custom Tokens** is cleaner and keeps the login interface strictly "Mobile Number + Password" without exposing email concepts to the user.
