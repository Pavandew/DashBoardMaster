package com.example.masterdashboard.home.settings.manager

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.masterdashboard.R
import com.example.masterdashboard.home.HomeActivity
import com.example.masterdashboard.home.settings.views.ChangePasswordFragment
import com.example.masterdashboard.home.settings.views.ProfileFragment
import com.example.masterdashboard.login.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView

class SettingsDrawerManager (
    private val activity: AppCompatActivity,
    private val navigationView: NavigationView,
    private val drawerLayout: DrawerLayout
){

    fun setupDrawerItem() {

        navigationView.setNavigationItemSelectedListener { item ->

            when (item.itemId) {

                // click on profile
                R.id.profile -> {
                    Toast.makeText(activity, "Profile", Toast.LENGTH_SHORT).show()

                    (activity as HomeActivity).onClickDrawerItem(
                        ProfileFragment(),
                        "profile"
                    )
                }

                // click on Change password
                R.id.changePassword -> {
                    Toast.makeText(activity, "Change Password", Toast.LENGTH_SHORT).show()

                    (activity as HomeActivity).onClickDrawerItem(
                        ChangePasswordFragment(),
                        "change_password"
                    )
                }

                R.id.logout -> {
                    Toast.makeText(activity, "Logout", Toast.LENGTH_SHORT).show()

                    MaterialAlertDialogBuilder(activity)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes") { dialog, which ->
                            // Handle logout logic here
                            Toast.makeText(activity, "Logged out", Toast.LENGTH_SHORT).show()
                            logOut()

                        }
                        .setNegativeButton("No") { dialog, which ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun logOut() {
        val intent = Intent(activity, LoginActivity::class.java).apply {
            // Clear backstack so user can't go back to HomeActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(intent)
        activity.finish()
    }
}