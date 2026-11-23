# 🍽️ RestroOne - Smart Multi-Tenant Restaurant Management System

**RestroOne** is an enterprise-grade Android application and Cloud Functions backend built to digitize and streamline daily restaurant operations. It features secure, role-based workspaces for **Restaurant Owners, Managers, Waiters, Kitchen Staff (KDS), and Cashiers**, providing a real-time order execution engine from table seating to kitchen preparation, checkout, thermal printing, and reporting.

---

## 🌟 Core Highlights & Capabilities

* **🔒 Enterprise Multi-Tenancy & Data Isolation:** Built on strict tenant boundaries (`restaurants/{restaurantId}/...`) with automated session sanitization and custom Firebase Security Rules to ensure zero cross-restaurant data leakage.
* **⚡ Serverless Cloud Backend:** Powered by **Firebase Cloud Functions** (Node.js) handling secure custom token authentication, staff credentials management, atomic order transactions, and subscription processing.
* **🖨️ ESC/POS Thermal Printing:** Native Bluetooth thermal printer integration (`58mm` / `80mm`) for printing provisional bills, final receipts, and Kitchen Order Tickets (KOTs) via `PrintBillController` & `BluetoothPrinterManager`.
* **💳 Subscriptions & Payments:** Integrated Razorpay payment gateway for single/multi-branch restaurant subscription plans, live expiry tracking, and feature access enforcement.
* **📱 Real-Time Sync Engine:** Instant order pipeline between Waiters, Kitchen Display System (KDS), and Settlement Center using Firestore snapshot streams and Kotlin Coroutines/StateFlow.

---

## 👔 Workspace Features by Role

### 👑 Owner & Manager Workspace
- **Real-Time Operations Dashboard:** Total revenue, active tables, pending orders, and daily KPIs at a glance.
- **Staff Roster Management:** Add, edit, assign roles (`Waiter`, `Chef`, `Cashier`, `Manager`), and configure granular permissions.
- **Menu & Category Engine:** Dynamically manage categories, food items, variants (sizes, toppings), pricing, and availability tags (Veg/Non-Veg).
- **Interactive Floor & Table Layout:** Create custom dining floors and define table capacities with live seating availability.
- **Reports & Financial Analytics:** Comprehensive breakdown of daily sales, cashier settlements, order history, and customer metrics.

### 🍽️ Waiter Workspace
- **Visual Floor & Table Map:** Instant status view (Free, Occupied, Billing) with customer details.
- **Digital Order Taking & Cart System:** Instant item additions, variant selections, custom instructions, and KOT generation.
- **Live Order Status Tracking:** Monitor active orders through `Preparing` ➔ `Ready` ➔ `Served` ➔ `Billing`.
- **Alert System:** Push notifications when kitchen marks tickets as ready to serve.

### 🧑‍🍳 Kitchen Display System (KDS)
- **Live Ticket Board:** Prioritized preparation queue categorized by order type (Dine-In, Takeaway, Delivery).
- **One-Tap Preparation Updates:** Transition item tickets from `Pending` ➔ `Preparing` ➔ `Ready` or specify rejection reasons.
- **KDS Item Breakdown:** Detailed itemized list displaying custom variants and customer dietary tags.

### 💳 Cashier & Settlement Center
- **Settlement Center:** Process table bills with configurable discounts, Service Charge toggles, and GST/Tax calculation.
- **Quick Bill Counter:** Fast checkout mode for walk-in takeaway and counter delivery orders without table assignments.
- **Multi-Mode Payment Acceptance:** Settle bills via Cash, UPI (with QR generation), Card, or Digital Wallets.
- **Receipt Printing & Handover:** Print provisional bills, final tax invoices, and confirm takeaway pickups.

---

## 🛠️ Technical Architecture & Tech Stack

The app follows **Clean Architecture** principles and the **MVVM (Model-View-ViewModel)** design pattern.

```text
               ┌──────────────────────────────┐
               │    UI Layer (XML & M3)       │
               └──────────────┬───────────────┘
                              │ Observes StateFlow / LiveData
               ┌──────────────▼───────────────┐
               │         ViewModel            │
               └──────────────┬───────────────┘
                              │ Coroutines / Result Wrapper
               ┌──────────────▼───────────────┐
               │         Repository           │
               └──────────────┬───────────────┘
                              │ Live Snapshot Streams
        ┌─────────────────────┴─────────────────────┐
        │                                           │
 ┌──────▼────────────┐                   ┌──────────▼───────────┐
 │ Cloud Firestore   │                   │ Cloud Functions (Node)│
 └───────────────────┘                   └──────────────────────┘
```

* **Client Stack:**
  * **Language:** 100% Idiomatic Kotlin
  * **Architecture:** MVVM, Clean Architecture, Repository Pattern
  * **UI Framework:** XML Layouts, Material Design 3, View Binding, Custom Drawers
  * **Asynchronous Execution:** Kotlin Coroutines, `StateFlow`, `SharedFlow`, `callbackFlow`
  * **Image Processing:** Image compression pipeline, Glide
  * **Hardware Interop:** Android Bluetooth Classic socket layer for ESC/POS Thermal Printers

* **Cloud Backend Stack:**
  * **Serverless Backend:** Node.js Firebase Cloud Functions v2
  * **Authentication:** Firebase Auth Phone OTP + Custom Token Server Authentication
  * **Database:** Cloud Firestore with strict multi-tenant Security Rules (`firestore.rules`)
  * **Push Notifications:** Firebase Cloud Messaging (FCM)
  * **Storage:** Firebase Cloud Storage for compressed menu & profile images

---

## 📁 Repository Structure

```text
Master Dashboard/
├── app/src/main/java/com/restroone/app/
│   ├── login/                     # Authentication & Splash Workspaces
│   ├── manager_single_res_dash/   # Owner/Manager Dashboard, Staff & Menu Setup
│   ├── staff_dash/
│   │   ├── waiter_screens/        # Waiter Floor Layout & Order Taking
│   │   ├── kitchen_screens/       # KDS Ticket Preparation Queue
│   │   └── billing_screens/       # Cashier Settlement Center & Quick Counter
│   ├── notifications/             # FCM Push Notification Handlers & Alert UI
│   ├── print_bill/                # ESC/POS Bluetooth Thermal Printing Controller
│   ├── subscription/              # Razorpay Payment & Plan Activation Engine
│   └── utils/                     # SessionManager, TaxCalculator, NavigationUtils
├── functions/                     # Firebase Cloud Functions Engine (Node.js)
│   ├── index.js                   # Entry point exporting modular functions
│   └── src/                       # Cloud function modules (auth, staff, orders, etc.)
└── firestore.rules                # Tenant-isolated Firestore security rules
```

---

## 👨‍💻 Author & Maintainer

**Pavan Dewangan**
- [LinkedIn](https://www.linkedin.com/in/pavandewangan/)
- [GitHub](https://github.com/Pavandew/)
