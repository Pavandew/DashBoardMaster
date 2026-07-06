# Restaurant Management System

A complete Android application built to simplify restaurant operations. The project provides separate interfaces for Managers, Waiters, Kitchen Staff, and Billing Staff, allowing each role to perform only the tasks assigned to them.

The goal of this project is to simulate how a real restaurant works by managing tables, orders, billing, staff, and kitchen operations from a single application.

> This project is currently under development, and new features are being added regularly.

---

## Features

### Manager

- Dashboard
- Staff Management
- Menu Management
- Floor & Table Management
- Inventory Management
- Reports
- Settings

### Waiter

- View Tables
- Take Customer Orders
- Manage Active Orders
- Customer Requests
- Notifications
- Profile

### Kitchen

- View Incoming Orders
- Start Preparing Orders
- Mark Orders as Ready
- Separate Dine-In and Takeaway Orders

### Billing

- Create New Orders
- Dine-In Billing
- Takeaway Billing
- Generate Bills
- Payment Management
- Billing History

---

## Technologies Used

- Kotlin
- XML
- Firebase Authentication
- Cloud Firestore
- MVVM Architecture
- ViewBinding
- RecyclerView
- Kotlin Coroutines
- Material Design Components

---

## Current Progress

- Authentication
- Role-Based Login
- Manager Module
- Waiter Module
- Kitchen Module
- Billing Module
- Staff Management
- Floor & Table Management
- Menu Management

Currently working on:

- Inventory
- Reports
- Restaurant Settings

---

## Project Structure

```
app
│
├── authentication
├── manager
├── waiter
├── kitchen
├── billing
├── models
├── repository
├── viewmodel
├── adapters
├── utils
└── firebase
```

---

## Architecture

This project follows the MVVM architecture.

```
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Firebase
```

The architecture helps keep the code organized and makes future feature additions easier.

---

## Screenshots

### Login

### Manager Dashboard

### Staff Management

### Kitchen

### Billing

### Waiter


---

## Future Improvements

Some features planned for upcoming versions:

- QR Code Ordering
- Customer Feedback
- Sales Analytics
- Expense Tracking
- Multi-Restaurant Support
- Offline Mode

---

## Why I Built This Project

I wanted to build something larger than a basic CRUD application and understand how different departments inside a restaurant work together.

This project helped me learn about application architecture, role-based access, Firebase integration, reusable UI components, and managing multiple workflows within a single Android application.

---

## Author

**Pavan Dewangan**

- [LinkedIn](https://www.linkedin.com/in/pavandewangan/)
- [GitHub](https://github.com/Pavandew/)
