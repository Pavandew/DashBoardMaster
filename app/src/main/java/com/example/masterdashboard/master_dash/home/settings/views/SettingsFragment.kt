package com.example.masterdashboard.master_dash.home.settings.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.masterdashboard.R
import com.example.masterdashboard.master_dash.home.MasterHomeActivity

class SettingsFragment : Fragment() {

    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitle: TextView
    private lateinit var drawerMenuIcon: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        setupToolbar(view)

        return view
    }
    private fun setupToolbar(view: View) {

        toolbar = view.findViewById(R.id.settings_toolbar)
        toolbarTitle = view.findViewById(R.id.toolbar_tvTitle)
        drawerMenuIcon = view.findViewById(R.id.toolbar_imgMenu)

        drawerMenuIcon.visibility = View.VISIBLE

        toolbarTitle.setText(R.string.settings)
        drawerMenuIcon.setOnClickListener {
            (activity as MasterHomeActivity).openDrawer()
        }

    }
}