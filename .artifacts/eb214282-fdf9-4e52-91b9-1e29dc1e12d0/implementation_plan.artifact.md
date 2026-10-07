# Final Approved Implementation Plan: Secure Cloud Function + Custom Token Authentication Migration

This document is the final approved implementation plan incorporating all 9 security corrections specified by the user.

---

## 1. Approved Architectural Corrections & Security Rules

1. **Server-Side UID Binding (`request.auth.uid`):**
   * During registration, `registerUser` extracts the UID **strictly** from the authenticated Firebase Phone-OTP session context (`context.auth.uid`), completely ignoring any UID sent in client request parameters.
2. **Secure Mobile $\rightarrow$ UID Mapping & Global Uniqueness:**
   * Implement a dedicated server-side collection `mobileMappings/{mobile}`.
   * Mobile numbers are guaranteed globally unique across all owners, managers, and staff. Registration checks this mapping before allowing account creation.
3. **Unified Staff & Owner Authentication:**
   * Staff authentication uses the exact same secure UID / Custom Token architecture. Staff records in `restaurants/{restaurantId}/staff/{staffDocId}` link to a Firebase Auth UID, allowing staff to authenticate via Mobile + Password and receive a Custom Token.
4. **Legacy Migration via OTP:**
   * Legacy users with old Java `String.hashCode()` passwords are not converted. They must verify via OTP once to set a secure password.
5. **Brute-Force Protection & Rate Limiting:**
   * The Cloud Function implements rate limiting / brute-force protection (tracking failed attempt counts per IP/mobile in Firestore or memory) and integrates with Firebase App Check.
6. **Zero Logging of Credentials:**
   * Passwords, plain text tokens, and password hashes are **never** logged to Logcat, Analytics, or Crashlytics.
7. **Deferral of Final Firestore Security Rules:**
   * Final security rules will be applied *only* after authentication and custom token sign-in are fully implemented and verified.
8. **Preservation of Existing Architecture:**
   * All Activities, Fragments, XML layouts, ViewBinding, ViewModels, and navigation paths remain untouched.
