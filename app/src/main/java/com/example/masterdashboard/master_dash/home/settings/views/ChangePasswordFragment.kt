package com.example.masterdashboard.master_dash.home.settings.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentChangePasswordBinding
import com.example.masterdashboard.master_dash.home.MasterHomeActivity

class ChangePasswordFragment : Fragment() {
    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitle: TextView
    private lateinit var drawerMenuIcon: ImageView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
    }

    private fun setupToolbar() {

        binding.settingsToolbar.toolbarTvTitle.text = getString(R.string.change_password)

        binding.settingsToolbar.toolbarImgMenu.visibility = View.VISIBLE
        binding.settingsToolbar.toolbarImgProfile.visibility = View.GONE
        binding.settingsToolbar.toolbarImgNotification.visibility = View.GONE

        binding.settingsToolbar.toolbarImgMenu.setOnClickListener {
            (activity as MasterHomeActivity).openDrawer()
        }

    }

}