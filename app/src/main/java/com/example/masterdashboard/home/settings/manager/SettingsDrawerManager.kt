package com.example.masterdashboard.home.settings.manager

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.masterdashboard.R
import com.example.masterdashboard.home.HomeActivity
import com.example.masterdashboard.login.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView

class SettingsDrawerManager(
    private val activity: AppCompatActivity,
    private val navigationView: NavigationView,
    private val drawerLayout: DrawerLayout
) {

    fun setupDrawerItem() {

        navigationView.setNavigationItemSelectedListener { item ->

            when (item.itemId) {

                // -----------------------------------------
                // Profile
                // -----------------------------------------
                R.id.profile -> {

                    Toast.makeText(
                        activity,
                        "Profile",
                        Toast.LENGTH_SHORT
                    ).show()

                    (activity as HomeActivity).openProfile()
                }

                // -----------------------------------------
                // Change Password
                // -----------------------------------------
                R.id.changePassword -> {

                    Toast.makeText(
                        activity,
                        "Change Password",
                        Toast.LENGTH_SHORT
                    ).show()

                    (activity as HomeActivity).openChangePassword()
                }

                // -----------------------------------------
                // Logout
                // -----------------------------------------
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

    // -----------------------------------------
    // Logout
    // -----------------------------------------
    private fun logout() {

        val intent = Intent(
            activity,
            LoginActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        activity.startActivity(intent)
        activity.finish()
    }
}