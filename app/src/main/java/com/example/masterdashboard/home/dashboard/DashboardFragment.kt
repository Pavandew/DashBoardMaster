package com.example.masterdashboard.home.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.example.masterdashboard.R
import com.google.android.material.imageview.ShapeableImageView

class DashboardFragment : Fragment() {
    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitle: TextView
    private lateinit var toolbarImgMenu: ImageView
    private lateinit var toolbarImgNot: ImageView
    private lateinit var toolbarImgProfile: ShapeableImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        toolbar(view)

        return view
    }

    // set toolbar
    private fun toolbar(view: View) {

        toolbar = view.findViewById(R.id.dashboard_toolbar)
        toolbarTitle = view.findViewById(R.id.toolbar_tvTitle)
        toolbarImgMenu = view.findViewById(R.id.toolbar_imgMenu)
        toolbarImgNot = view.findViewById(R.id.toolbar_imgNotification)
        toolbarImgProfile = view.findViewById(R.id.toolbar_imgProfile)

        toolbarTitle.setText(R.string.dashboard)
        toolbarImgMenu.visibility = View.GONE
        toolbarImgNot.visibility = View.GONE

    }
}