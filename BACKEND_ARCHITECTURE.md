# Master Dashboard - Backend Architecture & Business Logic Guide

## Overview

This project uses **Firebase Cloud Functions (Node.js 22, 2nd Gen Callable Functions)** to implement a secure, multi-tenant backend for restaurant management, POS ordering, staff authorization, and Razorpay subscriptions.

The backend is organized into a **clean, modular structure** inside the `functions/` directory:

```text
functions/
├── index.js                     # Main entry point (exports all cloud functions)
├── package.json                 # Node.js dependencies (firebase-admin, firebase-functions, bcryptjs)
└── src/
    ├── helpers.js               # Common utilities, rate limiters, DB helpers & tax logic
    ├── auth.js                  # User registration, login, custom tokens, and password reset
    ├── staff.js                 # Staff management, credential migration, and staff login
    ├── orders.js                # Active POS orders, kitchen status, waiter serving, & cashier settlement
    ├── notifications.js         # Real-time FCM notifications across Waiters, Kitchen, Cashiers & Managers
    └── subscriptions.js         # Restaurant setup and Razorpay payment order & signature verification
```

---

## 1. Modular Structure & Function Map

### `src/helpers.js` (Shared Utilities & Security)
Contains shared constants, rate limiting, path validators, and price/tax calculations used across all modules:
- `onAppCheckCall`: Callable function wrapper configuring App Check policies.
- `STAFF_ROLES`: Set of allowed staff roles (`waiter`, `chef`, `cashier`, `manager`, etc.).
- `normalizeMobile`: Standardizes Indian mobile numbers into `+91XXXXXXXXXX` format.
- `assertRestaurantManager`: Validates that the caller is the verified owner or active manager of the specified `restaurantId`.
- `assertRestaurantRole`: Validates that the caller has an allowed role for an operational request.
- `checkRateLimit`: Enforces anti-brute-force login rate limiting (max 5 failed attempts per 15 minutes).
- `recordFailedAttempt` / `clearRateLimit`: Manages `authRateLimits` Firestore collection.
- `calculateOrderTax` / `trustedMenuItem`: Calculates inclusive/exclusive tax (GST) and verifies item prices against the database.

---

### `src/auth.js` (User & Owner Authentication)

| Cloud Function | Access | Description |
| :--- | :--- | :--- |
| `registerUser` | Unauthenticated (Phone OTP context) | Binds a newly verified Phone OTP session (`request.auth.uid`) to a mobile number, hashes the password using `bcrypt`, creates `authCredentials/{uid}`, and sets `mobileMappings/{mobile}`. |
| `isMobileRegistered` | Public | Checks if a mobile number is registered. Designed with privacy protections against mobile number enumeration. |
| `authenticateUser` | Public | Authenticates owners and managers using mobile + password. Verifies `bcrypt` password hash, checks account status, auto-heals missing mappings/legacy credentials, clears rate limits, and returns a Firebase Custom Token (`admin.auth().createCustomToken`). |
| `updatePassword` | Authenticated | Allows an active logged-in owner/manager to update their password. |
| `resetPassword` | Authenticated (Phone OTP) | Allows a user with a verified Phone OTP session to set a new password. Auto-heals `mobileMappings`. |
| `verifyCurrentPassword` | Authenticated | Validates current password before allowing sensitive account updates. |
| `findUserForReset` | Public | Unauthenticated lookup for starting the password reset verification flow. |

---

### `src/staff.js` (Staff Roster & Staff Login)

| Cloud Function | Access | Description |
| :--- | :--- | :--- |
| `createStaff` | Owner / Manager | Adds a new staff member to `restaurants/{restaurantId}/staff/{staffDocId}`, creates their `users/{uid}` account, hashes their password into `authCredentials/{uid}`, and creates staff roster mirrors. |
| `updateStaff` | Owner / Manager | Updates staff details (name, phone, role, status, permissions) across the roster and `users` collection. |
| `deleteStaff` | Owner / Manager | Deletes staff profile, roster mirrors, `users` record, and credentials cleanly. |
| `migrateStaffCredentials` | Owner / Manager | Auto-migrates legacy staff records into official `users` and `authCredentials` records. |
| `authenticateStaff` | Public | Authenticates staff using `staffId` + `password`. Resolves `staffDocId` and `restaurantId`, verifies bcrypt password (with legacy fallback), auto-migrates credentials, and returns a Custom Token. |
| `updateStaffFcmToken` | Staff | Updates FCM push notification token across staff roster mirrors. |
| `verifyStaffCurrentPassword` | Staff | Verifies current staff password before password changes. |
| `updateStaffPassword` | Staff / Phone OTP | Updates staff password in `authCredentials`. |

---

### `src/orders.js` (POS & Order Lifecycle)

| Cloud Function | Access | Description |
| :--- | :--- | :--- |
| `submitRestaurantOrder` | Waiter / Manager | Validates prices against the database, calculates tax, creates/updates active orders at `restaurants/{restaurantId}/res_floors/{floorId}/floor_tables/{tableId}/active_orders/{orderId}`, and marks the table status as `OCCUPIED`. |
| `updateKitchenOrder` | Kitchen / Manager | Updates kitchen status (`PREPARING`, `READY`, `REJECTED`), updates ready item quantities, or rejects items with automatic bill total recalculation. |
| `updateWaiterOrder` | Waiter / Manager | Updates order status to `SERVED` or `BILLING`. |
| `settleRestaurantOrder` | Cashier / Manager | Settles active order, verifies prices, applies service charges and discounts, moves order to `completed_orders`, frees the table, and updates customer visit history. |
| `completeTakeawayOrder` | Cashier / Billing | Marks takeaway or delivery orders as handed over (`COMPLETED`). |

---

### `src/notifications.js` (Real-Time FCM Push Alerts)

| Cloud Function | Access | Description |
| :--- | :--- | :--- |
| `createStaffNotification` | Operational Staff | Triggers FCM push notifications for events (`KITCHEN_NEW_ORDER`, `BILL_REQUEST`, `ORDER_READY`, `INVENTORY_UPDATE`, `PAYMENT_SUCCESS`) targeted to relevant roles (Waiters, Kitchen, Cashiers, Managers). |
| `updateStaffNotificationStatus` | Operational Staff | Marks actionable notifications as `ACCEPTED` or `DONE`. |

---

### `src/subscriptions.js` (SaaS Subscription & Razorpay)

| Cloud Function | Access | Description |
| :--- | :--- | :--- |
| `completeRestaurantSetup` | Owner | Completes restaurant setup and initializes 30-day trial subscription. |
| `createSubscriptionPaymentOrder` | Owner | Creates a server-side Razorpay order for Monthly/Yearly SaaS plan subscriptions. |
| `verifyAndActivateSubscription` | Owner | Verifies Razorpay payment HMAC SHA-256 signature, validates capture status via Razorpay API, and activates the subscription in `restaurants` and `users` collections. |

---

## 2. Key Database Collections

```text
users/{uid}                              # Top-level user account profiles (Owners, Managers, Staff)
authCredentials/{uid}                   # Secure bcrypt password hashes (Inaccessible from client rules)
mobileMappings/{+91XXXXXXXXXX}          # Global mobile number uniqueness index -> uid
authRateLimits/{identifier}             # Rate-limiting tracking documents for brute-force protection
restaurants/{restaurantId}               # Restaurant outlet details & tax/service-charge settings
  ├── staff/{staffDocId}                # Staff roster record
  ├── res_floors/{floorId}/floor_tables/{tableId}/active_orders/{orderId}  # Live POS table orders
  ├── completed_orders/{orderId}        # Paid & settled historical orders
  ├── customers/{customerMobile}        # Customer visit and spend metrics
  └── notifications/{notificationId}    # In-app actionable & informational notifications
```

---

## 3. Important Security & Deployment Rules

1. **Custom Token Generation (`admin.auth().createCustomToken`):**
   Requires the **`Service Account Token Creator`** IAM role on your default Google Cloud Compute service account (`252505058012-compute@developer.gserviceaccount.com`).
2. **Deploying Cloud Functions:**
   Run the following command from the root directory whenever `functions/` code is updated:
   ```bash
   firebase deploy --only functions
   ```
