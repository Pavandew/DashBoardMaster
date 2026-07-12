package com.example.masterdashboard.master_dash.settings.manager

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.masterdashboard.R
import com.example.masterdashboard.master_dash.MasterHomeActivity
import com.example.masterdashboard.login.ActivityVisitorPortal
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class SettingsDrawerManager(
    private val activity: AppCompatActivity,
    private val navigationView: NavigationView,
    private val drawerLayout: DrawerLayout
) {

    fun setupDrawerItem() {

        navigationView.setNavigationItemSelectedListener { item ->

            when (item.itemId) {

                // Profile
                R.id.profile -> {

                    Toast.makeText(
                        activity,
                        "Profile",
                        Toast.LENGTH_SHORT
                    ).show()

                    (activity as MasterHomeActivity).openProfile()
                }

                // Change Password
                R.id.changePassword -> {

                    Toast.makeText(
                        activity,
                        "Change Password",
                        Toast.LENGTH_SHORT
                    ).show()

                    (activity as MasterHomeActivity).openChangePassword()
                }

                // Logout
                R.id.logout -> {

                    showLogoutDialog()
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    // -----------------------------------------
    // Logout Dialog
    // -----------------------------------------
    private fun showLogoutDialog() {

        MaterialAlertDialogBuilder(activity)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, _ ->

                Toast.makeText(
                    activity,
                    "Logged out",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
                logout()
            }

            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

            .show()
    }

    // Logout
    private fun logout() {

        // Clear local session
        val sharedPref = activity.getSharedPreferences("UserPrefs", AppCompatActivity.MODE_PRIVATE)
        sharedPref.edit().putBoolean("isLoggedIn", false).apply()

        FirebaseAuth.getInstance().signOut()

        val intent = Intent(
            activity,
            ActivityVisitorPortal::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        activity.startActivity(intent)
        activity.finish()
    }
}