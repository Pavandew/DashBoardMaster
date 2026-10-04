# RestroOne - Restaurant Management System

A comprehensive Android application built to streamline and simplify daily restaurant operations. RestroOne provides dedicated, role-based interfaces for Managers, Waiters, Kitchen Staff, and Cashiers, ensuring each department has the exact tools they need to operate efficiently.

The goal of this project is to simulate and digitize the complete lifecycle of a restaurant order—from table seating and kitchen prep to final billing and inventory management—all within a single, unified Android application.

> 🚀 **Status:** Currently in final beta testing and preparing for official release on the Google Play Store.

---

## 🌟 Key Features by Role

### 👔 Manager (Owner Dashboard)
- **Centralized Dashboard:** Real-time overview of restaurant performance.
- **Staff Management:** Add, edit, and assign roles to employees.
- **Menu & Category Management:** Dynamically update items, prices, and availability.
- **Floor & Table Management:** Visual layout configuration for different dining areas.
- **Inventory Tracking:** (In Progress) Monitor stock levels and ingredients.
- **Reporting & Analytics:** (In Progress) Daily sales and performance metrics.

### 🍽️ Waiter
- **Live Table Status:** Instantly see which tables are available, occupied, or waiting for food.
- **Digital Order Taking:** Send orders directly to the Kitchen Display System (KDS).
- **Order Management:** Add items to active orders or handle customer modifications.
- **Push Notifications:** Receive real-time alerts when food is ready to serve.

### 🧑‍🍳 Kitchen (KDS)
- **Live Ticket Board:** View incoming orders sorted by time and priority.
- **Order Status Tracking:** Mark individual items or whole tickets as "Preparing" or "Ready."
- **Queue Management:** Separate views for Dine-In, Takeaway, and Delivery orders.

### 💳 Billing (Cashier)
- **Fast Checkout:** Generate final bills with calculated taxes and discounts.
- **Split Payments:** Handle cash, card, and UPI transactions.
- **Quick Takeaway:** Dedicated flow for walk-in orders without table assignments.
- **Billing History:** Review past transactions and reprint receipts.

---

## 🛠️ Technical Stack & Architecture

This project is built using modern Android development practices, strictly adhering to the **MVVM (Model-View-ViewModel)** architectural pattern to ensure separation of concerns and scalable code.

*   **Language:** Kotlin
*   **UI Toolkit:** XML (with Material Design 3 Components)
*   **Architecture:** MVVM, Clean Architecture principles
*   **Asynchronous Programming:** Kotlin Coroutines & Flow
*   **Dependency Injection:** (Consider mentioning Hilt/Dagger here if you plan to add it, or remove this line)
*   **View Binding:** Enabled for safe UI component interactions.
*   **Image Loading:** Glide / Coil

**Firebase Integration:**
*   **Authentication:** Secure Phone Number/OTP and Password login.
*   **Cloud Firestore:** Real-time NoSQL database for syncing orders between Waiter and Kitchen instantly.
*   **Cloud Messaging (FCM):** Push notifications for staff communication.
*   **Cloud Storage:** Hosting menu item images and user avatars.

---

## 🏗️ Architecture Flow

```text
       ┌─────────────┐
       │   UI (XML)  │
       └──────┬──────┘
              │ Observes State (LiveData/Flow)
       ┌──────▼──────┐
       │  ViewModel  │
       └──────┬──────┘
              │ Requests Data
       ┌──────▼──────┐
       │ Repository  │
       └──────┬──────┘
              │ Fetches/Syncs
       ┌──────▼──────┐
       │  Firestore  │
       └─────────────┘
```

---

## 📱 Screenshots

*(Note: Replace these placeholder links with actual images of your app once uploaded to a `/images` folder in your repo)*

| Login & Roles | Manager Dashboard | Waiter UI | Kitchen KDS | Billing / Checkout |
| :---: | :---: | :---: | :---: | :---: |
| `<img src="images/placeholder.png" width="200"/>` | `<img src="images/placeholder.png" width="200"/>` | `<img src="images/placeholder.png" width="200"/>` | `<img src="images/placeholder.png" width="200"/>` | `<img src="images/placeholder.png" width="200"/>` |

---

## 🔮 Future Roadmap

- [ ] **Bluetooth Thermal Printing:** Direct integration for KOTs and final receipts.
- [ ] **QR Code Ordering:** Allow customers to scan a table QR and order from their phones.
- [ ] **Advanced Analytics:** Charts and graphs for peak hour predictions.
- [ ] **Multi-Branch Support:** Manage multiple restaurant locations from one admin account.
- [ ] **Offline-First Mode:** Room Database caching for operation during internet outages.

---

## 💡 Why I Built This Project

I set out to build something significantly more complex than a standard CRUD application. I wanted to understand the challenges of real-time data synchronization across multiple devices (e.g., a Waiter taking an order and it instantly appearing on the Kitchen tablet).

This project served as a deep dive into application architecture, handling complex role-based access control (RBAC), mastering Firebase real-time capabilities, and managing interconnected workflows within a single, cohesive Android application.

---

## 👨‍💻 Author

**Pavan Dewangan**
- [LinkedIn](https://www.linkedin.com/in/pavandewangan/)
- [GitHub](https://github.com/Pavandew/)
