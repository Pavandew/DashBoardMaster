package com.example.masterdashboard.utils

object AppConstants {
    // Portal Types
    const val PORTAL_MULTI_RESTAURANT = "multi_restaurant"
    const val PORTAL_RESTAURANT = "restaurant_portal"
    const val PORTAL_STAFF = "staff"

    // User Roles
    const val ROLE_OWNER_MULTI = "owner_multi"
    const val ROLE_OWNER_SINGLE = "owner_single"
    const val ROLE_MANAGER = "manager"
    const val ROLE_STAFF = "waiter_staff"
    const val KEY_STAFF_ID = "key_staff_id"
    const val KEY_STAFF_DOC_ID = "key_staff_doc_id"
    const val KEY_PERMISSIONS = "key_permissions"

    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_STAFF = "staff"
    const val COLLECTION_MENU_CATEGORIES = "menu_categories"
    const val COLLECTION_FOOD_ITEMS = "menu_food_items"
    const val COLLECTION_ADDONS = "addons"
    const val COLLECTION_RES_FLOORS = "res_floors"
    const val COLLECTION_RESTAURANTS = "restaurants_details"
    const val COLLECTION_TABLES = "floor_tables"
    const val COLLECTION_ACTIVE_ORDERS = "active_orders"
    const val COLLECTION_COMPLETED_ORDERS = "completed_orders"
    const val COLLECTION_INVENTORY = "inventory"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val COLLECTION_CUSTOMERS = "customers"

    // Firestore Fields - Common
    const val FIELD_UID = "uid"
    const val FIELD_FULL_NAME = "fullName"
    const val FIELD_ROLE = "role"
    const val FIELD_MOBILE = "mobile"
    const val FIELD_STATUS = "status"
    const val FIELD_PORTAL_TYPE = "portalType"
    const val FIELD_FCM_TOKEN = "fcmToken"
    const val FIELD_TIMESTAMP = "timestamp"

    // Firestore Fields - Restaurant & Manager
    const val FIELD_RESTAURANT_ID = "restaurantId"
    const val FIELD_RESTAURANT_NAME = "restaurantName"
    const val FIELD_IS_SETUP_COMPLETE = "isSetupComplete"

    // Firestore Fields - Staff
    const val FIELD_STAFF_ID = "staffId"
    const val FIELD_STAFF_NAME = "staffName"
    const val FIELD_EMAIL = "email"
    const val FIELD_PASSWORD = "password"
    const val FIELD_PASSWORD_HASH = "passwordHash"
    const val FIELD_GENDER = "gender"
    const val FIELD_DEPARTMENT = "department"
    const val FIELD_JOINING_DATE = "joiningDate"
    const val FIELD_SHIFT = "shift"
    const val FIELD_SALARY = "salary"
    const val FIELD_PERMISSIONS = "permissions"
    const val FIELD_DOCUMENT_TYPE = "documentType"
    const val FIELD_DOCUMENT_NUMBER = "documentNumber"
    const val FIELD_WAITER_ID = "waiterId"

    // Firestore Fields - Menu & Category
    const val FIELD_CATEGORY_NAME = "menuCategoryName"
    const val FIELD_ITEM_NAME = "itemName"
    const val FIELD_ITEM_PRICE = "price"
    const val FIELD_ITEM_IMAGE = "imageUrl"
    const val FIELD_IS_VEG = "isVeg"
    const val FIELD_HAS_VARIANTS = "hasVariants"
    const val FIELD_VARIANTS = "variants"
    const val FIELD_VARIANT_NAME = "variantName"

    // Firestore Fields - Table & Floor
    const val FIELD_FLOOR_ID = "floorId"
    const val FIELD_FLOOR_NAME = "floorName"
    const val FIELD_TABLE_ID = "tableId"
    const val FIELD_TABLE_NAME = "tableName"
    const val FIELD_TOTAL_SEATS = "capacity"
    const val FIELD_CUSTOMER_NAME_TABLE = "customerName"
    const val FIELD_CURRENT_BILL = "currentBillAmount"

    // Firestore Fields - Orders & Billing
    const val FIELD_ORDER_ID = "orderId"
    const val FIELD_ORDER_TYPE = "orderType"
    const val FIELD_ORDER_STATUS = "orderStatus"
    const val FIELD_ORDER_ITEMS = "items"
    const val FIELD_ITEM_ID = "itemId"
    const val FIELD_QUANTITY = "quantity"
    const val FIELD_ORDERED_QTY = "orderedQuantity"
    const val FIELD_READY_QTY = "readyQuantity"
    const val FIELD_ITEM_NOTE = "itemNote"
    const val FIELD_ROW_TOTAL = "rowTotal"
    const val FIELD_CATEGORY = "category"
    const val FIELD_SUBTOTAL = "subtotal"
    const val FIELD_GST = "gst"
    const val FIELD_GRAND_TOTAL = "grandTotal"
    const val FIELD_PAID_AT = "paidAt"
    const val FIELD_PAYMENT_METHOD = "paymentMethod"
    const val FIELD_DISCOUNT_AMOUNT = "discountAmount"
    const val FIELD_BILLING_MONTH = "billingMonth"
    const val FIELD_BILLING_DATE = "billingDate"
    const val FIELD_SPECIAL_NOTES = "specialNotes"
    const val FIELD_REJECTION_REASON = "rejectionReason"
    const val FIELD_CUSTOMER_NAME = "customerName"
    const val FIELD_CUSTOMER_MOBILE = "customerMobile"
    const val FIELD_TAX = "tax"
    const val FIELD_ORDER_DOC_PATH = "orderDocPath"
    const val FIELD_IS_READ = "isRead"
    const val FIELD_TARGET_ROLE = "targetRole"
    const val FIELD_TARGET_STAFF_ID = "targetStaffId"
    const val FIELD_NOTIFICATION_TYPE = "type"
    const val FIELD_NOTIFICATION_TITLE = "title"
    const val FIELD_NOTIFICATION_MESSAGE = "message"

    // Firestore Fields - Customer
    const val FIELD_CUSTOMER_ID = "customerId"
    const val FIELD_LAST_VISIT = "lastVisit"
    const val FIELD_VISIT_COUNT = "visitCount"
    const val FIELD_TOTAL_SPENT = "totalSpent"

    // Firestore Fields - Inventory
    const val FIELD_INVENTORY_ID = "inventoryId"
    const val FIELD_ITEM_QUANTITY = "itemQuantity"
    const val FIELD_ITEM_UNIT = "itemUnit"
    const val FIELD_MIN_THRESHOLD = "minThreshold"
    const val FIELD_LAST_UPDATED = "lastUpdated"
    const val FIELD_INVENTORY_CATEGORY = "inventoryCategory"
    const val FIELD_ESTIMATED_DAYS_LEFT = "estimatedDaysLeft"

    // Status Values
    const val STATUS_ACTIVE = "Active"
    const val STATUS_FREE = "FREE"
    const val STATUS_OCCUPIED = "OCCUPIED"
    const val STATUS_PENDING = "PENDING"
    const val STATUS_PREPARING = "PREPARING"
    const val STATUS_READY = "READY"
    const val STATUS_SERVED = "SERVED"
    const val STATUS_BILLING = "BILLING"
    const val STATUS_PAID = "PAID"
    const val STATUS_COMPLETED = "COMPLETED"
    const val STATUS_REJECTED = "REJECTED"

    // Order Types
    const val ORDER_TYPE_DINE_IN = "DINE_IN"
    const val ORDER_TYPE_TAKE_AWAY = "TAKE_AWAY"
    const val ORDER_TYPE_DELIVERY = "DELIVERY"

    // Preferences Keys
    const val PREF_NAME = "master_dashboard_prefs"
    const val KEY_SELECTED_PORTAL = "selected_portal"
    const val KEY_UID = "uid"
    const val KEY_ROLE = "role"
    const val KEY_MOBILE = "mobile"
    const val KEY_NAME = "name"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_RESTAURANT_ID = "restaurant_id"
    const val KEY_RESTAURANT_NAME = "restaurant_name"
    const val KEY_RESTAURANT_DETAILS = "restaurant_details"
    const val KEY_IS_RESTAURANT_SETUP = "is_restaurant_setup"
    const val KEY_REGISTRATION_DRAFT = "registration_draft"

    // Intent Extras
    const val EXTRA_EDIT_MODE = "extra_edit_mode"
    const val EXTRA_START_STEP = "extra_start_step"

    // UI Strings & Tags
    const val TITLE_MENU_MANAGEMENT = "Menu Management"
    const val BTN_ADD_CATEGORY = "+ Add Category"
    const val BACKSTACK_ADD_STAFF = "add_staff_flow"
    const val TAG_MENU_MANAGEMENT = "MenuManagementFragment"
}
