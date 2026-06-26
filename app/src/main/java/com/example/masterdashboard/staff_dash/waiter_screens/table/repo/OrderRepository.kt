package com.example.masterdashboard.staff_dash.waiter_screens.table.repo

import android.util.Log
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuCategoryData
import kotlinx.coroutines.delay

class OrderRepository{

    private val TAG = "OrderRepository"

    // simulates an API call fetching category filters
    suspend fun getMenuCategories(): List<MenuCategoryData> {
        Log.d(TAG, "getMenuCategories: Requesting categories...")
        delay(300)

        return listOf(
            MenuCategoryData("C1", "Appetizers"),
            MenuCategoryData("C2", "Main Courses"),
            MenuCategoryData("C3", "Desserts"),
            MenuCategoryData("C4", "Beverages"),
            MenuCategoryData("C5", "Sides"),
            MenuCategoryData("C6", "Salads"),
            MenuCategoryData("C7", "Specials"),
            MenuCategoryData("C8", "Starters"),
            MenuCategoryData("C9", "Drinks"),
            MenuCategoryData("C10", "Snacks"),
        )
    }


    // Simulates live streaming items matching the screen image
    suspend fun getFoodMenu(): List<FoodItemData> {
        Log.d(TAG, "getFoodMenu: Requesting food menu...")
        delay(100)

        return listOf(
            FoodItemData("F1", "Spring Rolls", 5, "https://images.unsplash.com/photo-1544378730-8b5104b18790?q=80&w=500"),
            FoodItemData("F2", "Grilled Chicken", 12, "https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?q=80&w=500"),
            FoodItemData("F3", "Chocolate Cake", 7, "https://images.unsplash.com/photo-1578985545062-69928b1d9587?q=80&w=500"),
            FoodItemData("F4", "Veggie Burger", 10, "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?q=80&w=500"),
            FoodItemData("F5", "Pepperoni Pizza", 15, "https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=500"),
            FoodItemData("F6", "Greek Salad", 8, "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?q=80&w=500"),
            FoodItemData("F7", "Pesto Pasta", 11, "https://images.unsplash.com/photo-1473093226795-af9932fe5856?q=80&w=500"),
            FoodItemData("F8", "French Fries", 4, "https://images.unsplash.com/photo-1630384060421-cb20d0e0649d?q=80&w=500"),
            FoodItemData("F9", "Iced Coffee", 5, "https://images.unsplash.com/photo-1517701604599-bb29b565090c?q=80&w=500"),
            FoodItemData("F10", "Fruit Salad", 6, "https://images.unsplash.com/photo-1519996529931-28324d5a630e?q=80&w=500")
        )
    }
}