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
    const val KEY_PERMISSIONS = "key_permissions"

    // Firestore Collections & Fields
    const val COLLECTION_USERS = "users"
    const val FIELD_PHONE = "phone"
    const val FIELD_PASSWORD_HASH = "passwordHash"
    const val FIELD_IS_VERIFIED = "isVerified"
    const val FIELD_UID = "uid"
    const val FIELD_ROLE = "role"
    const val FIELD_PORTAL_TYPE = "portalType"

    // Others
    const val PHONE_PREFIX_INDIA = "+91"
    
    // Preferences Keys
    const val PREF_NAME = "master_dashboard_prefs"
    const val KEY_SELECTED_PORTAL = "selected_portal"
    const val KEY_UID = "uid"
    const val KEY_ROLE = "role"
    const val KEY_PHONE = "phone"
    const val KEY_NAME = "name"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_RESTAURANT_ID = "restaurant_id"

    // Backstack Tags
    const val BACKSTACK_ADD_STAFF = "add_staff_flow"
    const val TAG_MENU_MANAGEMENT = "MenuManagementFragment"

    // Sub-collections
    const val COLLECTION_STAFF = "staff"
    const val COLLECTION_MENU_CATEGORIES = "menu_categories"
    const val COLLECTION_FOOD_ITEMS = "menu_food_items"
    const val COLLECTION_RES_FLOORS = "res_floors"
    const val COLLECTION_TABLES = "floor_tables"

    // UI Strings
    const val TITLE_MENU_MANAGEMENT = "Menu Management"
    const val BTN_ADD_CATEGORY = "+ Add Category"
}
